import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Gateway {
    private static final int PCKT_SIZE = 17640 /* 8820 x 1.5 */, G_PORT = 5000, SV_PORT = 1337, BC_PORT = 6000;
    private static final String BC_ADDRESS = "255.255.255.255", SV_ADDRESS = "10.0.1.12"; //10.0.1.12
    private static byte[] musicList = new byte[PCKT_SIZE];
    private static ServerSocket gSocket;
    private static DatagramSocket bcSocket = null;
    private static Socket tcpServerSocket;
    private static ConcurrentHashMap<String, Socket> activeClients = new ConcurrentHashMap<String, Socket>();
    private static byte[][] storedPackets = new byte[0xFFFF][];
    private static int maxSN = 0, pausedSN = 0, minSN = 0;
    private static ScheduledExecutorService executor;
    private static long initialTStamp = 0;
    private static boolean musicPaused = true, scheduling = false;
    private static final AsyncString aString = new AsyncString();
    public static void main(String[] agrs) throws UnknownHostException, IOException, InterruptedException,
            ClassNotFoundException {
        connectToServer();
        receiveClients();
    }

    public static void connectToServer() throws UnknownHostException, IOException, ClassNotFoundException {
        executor = Executors.newScheduledThreadPool(1);
        tcpServerSocket = new Socket(SV_ADDRESS, SV_PORT);
        System.out.println("> Connected to TCP Server @" + tcpServerSocket.getInetAddress().getHostAddress()
                + ":" + tcpServerSocket.getPort());
        int listLength = tcpServerSocket.getInputStream().read(musicList);
        musicList = Arrays.copyOf(musicList, listLength);
        System.out.println("> TCP received from Server (" + musicList.length + ")");

        new Thread(receiveFromServer(tcpServerSocket)).start();
    }

    public static void receiveClients() throws IOException {
        gSocket = new ServerSocket(G_PORT);
        System.out.println("\n> TCP Gateway server listening on port " + G_PORT);
        System.out.println("> Waiting for client connections...\n");

        while (gSocket.isBound() && !gSocket.isClosed()) {    
            Socket socket = gSocket.accept();
            activeClients.put(socket.getInetAddress().getHostAddress(), socket);
            System.out.println("> New client connection: "+ socket.getInetAddress().getHostAddress()
                    + ":" + socket.getPort());
            
            socket.getOutputStream().write(musicList, 0, musicList.length);
            System.out.println("> TCP sending to client @" + socket.getInetAddress().getHostAddress()
                    + ":" + socket.getPort() + " (" + musicList.length + ")\n");
            
            Thread receiver = new Thread(receiveFromClients(socket));
            receiver.start();
            new Thread(clientsSender(socket, receiver)).start();
        }
    }
    
    public static Runnable clientsSender(Socket socket, Thread receiver) {
        return new Runnable() {
			@Override
			public void run() {
                try {                    
                    while (receiver.isAlive()) {
                        byte[] buf = aString.getBytes();
                        socket.getOutputStream().write(buf);
                    }
                }

                catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
			}
        };
    }
    
    private static Runnable receiveFromServer(Socket socket) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    int length = 0;
                    byte[] receiveData = new byte[PCKT_SIZE];
                    bcSocket = new DatagramSocket();
                    bcSocket.setBroadcast(true);

                    while ((length = socket.getInputStream().read(receiveData)) > 0) {
                        receiveData = Arrays.copyOf(receiveData, length);
                        System.out.println("> TCP received from Server: " + String.format("%02X%02X %01X %01X %02X%02X %02X%02X ... ",
                                receiveData[0], receiveData[1], ((receiveData[2] & 0xFF) >> 4), (receiveData[2] & 0xF),
                                receiveData[3], receiveData[4], receiveData[5], receiveData[6]) + "(" + receiveData.length + " bytes)");
                        int pcktCount = ((receiveData[0] << 8) + (receiveData[1] & 0xFF)) & 0xFFFF;

                        if (pcktCount < maxSN + 16) {
                            if (pcktCount == 0) {
                                System.out.println("> Setting initial timestamp");
                                initialTStamp = System.currentTimeMillis();
                            }
                            
                            if (!scheduling) {
                                executor.scheduleWithFixedDelay(reTransmitPackets(bcSocket), 2L, 2L, TimeUnit.SECONDS);
                                scheduling = true;
                            }

                            storedPackets[pcktCount] = receiveData;
                            maxSN = pcktCount;
                            DatagramPacket bcPacket = new DatagramPacket(receiveData, receiveData.length,
                                    InetAddress.getByName(BC_ADDRESS), BC_PORT);

                            System.out.println("> UDP broadcasting to clients: " + String.format("%02X%02X ... ",
                                    receiveData[0], receiveData[1]) + "(" + receiveData.length + " bytes)");

                            bcSocket.send(bcPacket);
                            //sendTo2ndPort(receiveData);

                        }

                        else {
                            System.out.println("> Discarding packet");
                        }

                        receiveData = new byte[PCKT_SIZE];                        
                    }

                    bcSocket.close();
                }
                
                catch (IOException e) {
                    e.printStackTrace();
                }
                
            }
        };
    }

    private static void sendTo2ndPort(byte[] receiveData) throws IOException {
        DatagramPacket bcPacket2 = new DatagramPacket(receiveData, receiveData.length,
                InetAddress.getByName(BC_ADDRESS), BC_PORT + 1);

        System.out.println("> UDP broadcasting to clients (2): " + String.format("%02X%02X ... ",
                receiveData[0], receiveData[1]) + "(" + receiveData.length + " bytes)");

        bcSocket.send(bcPacket2);
    }

    private static Runnable reTransmitPackets(DatagramSocket socket) {
        return new Runnable() {
            @Override
            public void run() {
                if (musicPaused)
                    minSN = pausedSN;
                
                else {
                    minSN = (int) ((System.currentTimeMillis() - initialTStamp) / 1000 * 20);
                    minSN += pausedSN;
                }
                    
                System.out.println(">> Resending between " + String.format("%04X and %04X", minSN, maxSN));

                if (maxSN >= minSN){
                    int midSN = (maxSN + minSN) / 2;
                    List<byte[]> firstList = Arrays.asList(Arrays.copyOfRange(storedPackets, minSN, midSN));
                    List<byte[]> secondList = Arrays.asList(Arrays.copyOfRange(storedPackets, midSN, maxSN));

                    Collections.shuffle(firstList);
                    Collections.shuffle(secondList);
                    
                    try {
                        System.out.println(">> Resending 50% between " + String.format("%04X and %04X", minSN, midSN));

                        for (byte[] pckt : firstList.stream().limit((int) ((midSN - minSN) * 0.2)).collect(Collectors.toList())) {
                            if (pckt != null && pckt.length > 0) {
                                System.out.printf(">> UDP resending to clients: %02X%02X ...\n", pckt[0], pckt[1]);
                                socket.send(new DatagramPacket(pckt, pckt.length, InetAddress.getByName(BC_ADDRESS), BC_PORT));
                                TimeUnit.MILLISECONDS.sleep(5);
                            }

                            else {
                                System.out.println(">> Null packet...");
                            }
                        }

                        System.out.println(">> Resending 20% between " + String.format("%04X and %04X", midSN, maxSN));

                        for (byte[] pckt : secondList.stream().limit((int) ((midSN - minSN) * 0.2)).collect(Collectors.toList())) {
                            if (pckt != null && pckt.length > 0) {
                                System.out.println(">> UDP resending to clients: " + String.format("%02X%02X ... ", pckt[0], pckt[1]));
                                socket.send(new DatagramPacket(pckt, pckt.length, InetAddress.getByName(BC_ADDRESS), BC_PORT));
                                TimeUnit.MILLISECONDS.sleep(5);
                            }

                            else {
                                System.out.println(">> Null packet...");
                            }
                        }

                        System.out.println(">> Packets resent");

                    }
                    
                    catch ( IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
        
                }
                
            }
            
        };
    }

    private static Runnable receiveFromClients(Socket socket) {
        return new Runnable(){
            @Override
            public void run() {
                try {
                    byte[] bytes = new byte[3];
                    int length = 0;

                    while ((length = socket.getInputStream().read(bytes)) > 0) {
                        int request = bytes[0];
                        System.out.println("> TCP received from Client @" + socket.getInetAddress().getHostAddress()
                                + ":" + socket.getPort() + ": " + String.format("%02X", bytes[0])
                                + (length > 1 ? String.format(" %02X%02X", bytes[1], bytes[2]) : ""));
                        if (request == 0) {
                            if (musicPaused) {
                                System.out.println("> Resuming music");
                                System.out.println("> Setting initial timestamp");
                                initialTStamp = System.currentTimeMillis();
                                musicPaused = false;
                                pausedSN = ((bytes[1] << 8) + (bytes[2] & 0xFF)) & 0xFFFF;
                            }
                            
                            else {
                                musicPaused = true;
                                pausedSN = ((bytes[1] << 8) + (bytes[2] & 0xFF)) & 0xFFFF;
                                System.out.println("> Music paused at " + String.format("%04X", pausedSN));
                            }

                            System.out.println("> TCP async sending to Clients: " + String.format("%02X", bytes[0])
                                    + String.format(" %02X%02X", bytes[1], bytes[2]) + " (" + length + ")\n");
                            aString.setBytes(Arrays.copyOf(bytes, length));

                        }
                        
                        else if (request == 1) {                             //1 -> replay
                            pausedSN = 0;
                            System.out.println("> Replaying music");
                            System.out.println("> Setting initial timestamp");
                            initialTStamp = System.currentTimeMillis();
                            System.out.println("> TCP async sending to Clients: " + String.format("%02X", request) + "\n");
                            aString.setBytes(bytes);
                        }

                        else {
                            maxSN = pausedSN = minSN = 0;
                            initialTStamp = 0;
                            storedPackets = null;
                            storedPackets = new byte[0xFFFF][]; 
                            musicPaused = true;
                            System.out.println("> Playing new music #" + request);
                            System.out.println("> TCP sending to Server: " + String.format("%02X", request) + "\n");
                            tcpServerSocket.getOutputStream().write(Arrays.copyOf(bytes, 1));
                            System.out.println("> TCP async sending to Clients: " + String.format("%02X", request));
                            aString.setBytes(Arrays.copyOf(bytes, 1));                           
                        }                       
                    }
                }
                
                catch (IOException e) {
                    e.printStackTrace();
                }
                
            }
        };

    }
}