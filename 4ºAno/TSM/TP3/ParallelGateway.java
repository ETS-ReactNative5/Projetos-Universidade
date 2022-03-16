import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class ParallelGateway {
    private static final int PCKT_SIZE = 13230 /* 8820 x 1.5 */, SV_PORT = 1337, BC_PORT = 6000;
    private static final String BC_ADDRESS = "255.255.255.255", SV_ADDRESS = "10.0.1.12"; //10.0.1.12
    private static String musicListS = "";
    private static DatagramSocket bcSocket = null;
    private static Socket tcpServerSocket;
    public static void main(String[] agrs) throws UnknownHostException, IOException, InterruptedException,
            ClassNotFoundException {
        connectToServer();
    }

    public static void connectToServer() throws UnknownHostException, IOException, ClassNotFoundException {
        tcpServerSocket = new Socket(SV_ADDRESS, SV_PORT);
        System.out.println("> Connected to TCP Server @" + tcpServerSocket.getInetAddress().getHostAddress()
                + ":" + tcpServerSocket.getPort());
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(tcpServerSocket.getInputStream()));
        musicListS = inFromClient.readLine();
        System.out.println("> TCP received from Server: " + musicListS);

        new Thread(receiveFromServer(tcpServerSocket)).start();
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

                        DatagramPacket bcPacket = new DatagramPacket(receiveData, receiveData.length,
                                InetAddress.getByName(BC_ADDRESS), BC_PORT);

                        System.out.println("> UDP broadcasting to clients: " + String.format("%02X%02X ... ",
                                receiveData[0], receiveData[1]) + "(" + receiveData.length + " bytes)");

                        bcSocket.send(bcPacket);
                        //sendTo2ndPort(receiveData);
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
}