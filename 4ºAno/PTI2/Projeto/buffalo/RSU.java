package buffalo;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import imtt.LimitedQueue;
import imtt.Utils;

public class RSU {
    private static final String ADDR = "buffalo.auto.pt", MULTICAST = "FF02::1", INTERFACE = "eth1";
    private static final int PORT = 1337, BRAND_ID = 1, PCKG_SIZE = 20512, PCKG_SMALL = 1024;
    private static final LimitedQueue<String> sentMessages = new LimitedQueue<>(256);
    private static Socket socket;
    private static MulticastSocket mSocket;
    private static InetAddress mcAddress;

    public static InetAddress getInterface() throws SocketException {
        NetworkInterface nInterface = NetworkInterface.getByName(INTERFACE);
        Enumeration<InetAddress> addresses = nInterface.getInetAddresses();
        InetAddress iAddress = null;

        while (addresses.hasMoreElements()) {
            iAddress = addresses.nextElement();
        }

        return iAddress;
    }

    public static void main(String[] args) throws IOException {        
        System.out.println(">>> Initializing " + Utils.RED + "Buffalo" + Utils.NF + "'s RSU Application...\n");
        Utils.clearScreen();
        System.out.println("\n\t=== [ " + Utils.RED + "Buffalo" + Utils.NF + " RSU ] ===");
        boolean connected = false;
        String brandAddress = args.length != 0 ? args[0] : ADDR;
        System.out.print("\n> Connecting to " + Utils.RED + "Buffalo" + Utils.NF + "'s TCP Server @" + brandAddress + ":" + PORT + "..." + Utils.RED);

        while (!connected) {
            try {
                socket = new Socket(brandAddress, PORT);
                connected = true;
                break;
            }

            catch (ConnectException e) {
                System.out.print(".");
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            }

            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println(Utils.NF + "\n> Successfully connected to " + Utils.RED + "Buffalo" + Utils.NF + "'s Server @" + brandAddress + ":" + PORT);
        mcAddress = Inet6Address.getByName(MULTICAST);
        mSocket = new MulticastSocket(PORT);
        mSocket.setInterface(getInterface());
        new Thread(obuReceiver()).start();
        byte[] buf = new byte[PCKG_SMALL];
        int size = 0;
        
        while ((size = socket.getInputStream().read(buf)) > -1) {
            buf = Arrays.copyOf(buf, size);
            sentMessages.add(new String(buf));
            System.out.println("> " + Utils.RED + "Buffalo" + Utils.NF + " message (" + size + " B)");
            mSocket.send(new DatagramPacket(buf, size, mcAddress, PORT));
            buf = new byte[PCKG_SMALL];
        }

        socket.close();
        mSocket.close();
    }

    public static Runnable obuReceiver() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while (mSocket.isBound() && socket.isBound()) {
                        byte[] buf = new byte[PCKG_SIZE];
                        DatagramPacket dPacket = new DatagramPacket(buf, buf.length);
                        mSocket.receive(dPacket);
                        buf = Arrays.copyOf(buf, dPacket.getLength());
                        String data = new String(buf);
                        
                        if (!sentMessages.contains(data)) {
                            sentMessages.add(data);
                            String[] params = data.split(" ");

                            if (params[0].equals("S") && Integer.parseInt(params[2]) == BRAND_ID && params[3].equals("brand")) {
                                System.out.println("> OBU message to " + Utils.RED + "Buffalo" + Utils.NF + " (" + buf.length + " B)");
                                socket.getOutputStream().write(buf);
                            }
                            
                            else {
                                System.out.println("> General vehicular traffic (" + buf.length + " B)");
                                mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                            }
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