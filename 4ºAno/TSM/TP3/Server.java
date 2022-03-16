import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Server {
    private static final int PCKT_SIZE = 8820, SV_PORT = 1337;
    private static final String MUSIC_PATH = "Musics/";
    private static ServerSocket sSocket;
    private static ArrayList<String> musicList = new ArrayList<>();
    private static int playingId = 0;
    
    public static String timeFormat(int ms) {
        return new SimpleDateFormat("mm:ss").format(new Date((long) ms));
    }

    public static void main(String[] args) throws Exception {
        byte[] list = scanMusics();
        
        sSocket = new ServerSocket(SV_PORT);
        System.out.println("\n> TCP Server listening on port " + SV_PORT);
        System.out.println("> Waiting for gateway connections...\n");
        
        while (sSocket.isBound() && !sSocket.isClosed()) {
            Socket socket = sSocket.accept();
            System.out.println("> New Gateway connection: " + socket.getInetAddress().getHostAddress()
                    + ":" + socket.getPort());
            Socket parallelSocket = sSocket.accept();
            System.out.println("> Parallel Gateway connection: " + parallelSocket.getInetAddress().getHostAddress()
                    + ":" + parallelSocket.getPort());
            Thread receiver = new Thread(receiver(socket, parallelSocket));
            receiver.start();
            String listString = "";
            
            for (int i = 0; i < list.length;) {
                int duration = ((list[i] << 8) + (list[i + 1] & 0xFF)) & 0xFFFF;
                String name = new String(Arrays.copyOfRange(list, i + 2, list.length)).split("/")[0];
                listString += String.format("%04X %s / ", duration, name);
                i += name.length() + 3;
            }

            System.out.println("> TCP sending to Gateway @" + socket.getInetAddress().getHostAddress()
                    + ":" + socket.getPort() + ": " + listString + " (" + list.length + ")\n");
            socket.getOutputStream().write(list, 0, list.length);
        }
    }

    public static byte[] scanMusics() {
        File f = null;
        String[] paths;
        byte[] packet = new byte[PCKT_SIZE];
        int i = 0;
        System.out.println("--- [ List of musics ] ---");
        // create new file
        f = new File(MUSIC_PATH);
                                
        // array of files and directory
        paths = f.list();
        
        // for each name in the path array
        for (String path : paths) {
            try {
                AudioInputStream aiStream = AudioSystem.getAudioInputStream(new File(MUSIC_PATH + path));
                AudioFormat aFormat = aiStream.getFormat();

                if (aFormat.getFrameRate() == 44100.0 && aFormat.getSampleSizeInBits() == 16
                        && aFormat.getChannels() == 2 && aFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                        && !aFormat.isBigEndian()) {
                    int duration = (int) (aiStream.getFrameLength() / aFormat.getFrameRate());
                    System.out.println(" #" + musicList.size() + " " + path + " ("
                            + timeFormat(duration * 1000) + ")");
                    musicList.add(path);
                    packet[i] = (byte) (duration >> 8);
                    packet[i + 1] = (byte) duration;
                    System.arraycopy((path + "/").getBytes(), 0, packet, i + 2, path.length() + 1);
                    i += path.length() + 3;
                }

            }
            
            catch (UnsupportedAudioFileException | IOException e) {}
        }

        return Arrays.copyOf(packet, i);
    }

    private static Runnable receiver(Socket socket, Socket parallelSocket) {
        return new Runnable() {

            @Override
            public void run() {
                try {
                    byte[] bytes = new byte[1];

                    while (socket.getInputStream().read(bytes) > 0) {
                        int request = bytes[0];
                        System.out.println("> TCP received from Gateway @"
                                + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ": "
                                + String.format("%02X", request));

                        if (request > 0) {
                            playingId = request;
                            new Thread(streamMusic(request, socket, parallelSocket)).start();
                        }
                    }
                }
                
                catch (IOException | UnsupportedAudioFileException | InterruptedException e) {
                    e.printStackTrace();
                }
                
            }
        };
    }

    public static byte[] compress(byte[] audio, int pcktCount) throws IOException {
        double davg = 0;
        double doff = 0;
        int maxBavg = 2, maxBoff = 1, firstLeft = 0, firstRight = 0;
        double[] davgs = new double[audio.length / 4 - 1];
        double[] doffs = new double[audio.length / 4 - 1];

        
        for (int i = 0; i < audio.length; i += 4) {
            byte[] uaudio = {(byte) (audio[i] + 0xFF),
                    (byte) (audio[i + 1] + 0x7F), 
                    (byte) (audio[i + 2] + 0xFF),
                    (byte) (audio[i + 3] + 0x7F)};

            int channels[] = {
                ((uaudio[1] << 8) + (uaudio[0] & 0xFF)) & 0xFFFF,
                ((uaudio[3] << 8) + (uaudio[2] & 0xFF)) & 0xFFFF
            };
            
            double avg = (double) (channels[0] + channels[1]) / 2;
            double off = channels[0] - avg;
            davg = avg - davg;
            doff = off - doff;

            int bavg = 2 + new BigInteger((int) Math.abs(davg) + "").bitLength();
            int boff = 1 + new BigInteger((int) Math.abs(doff) + "").bitLength();

            if (bavg < 3) bavg = 3;
            if (boff < 2) boff = 2;
        
            if (i > 0) {
                if (bavg > maxBavg) maxBavg = bavg;
                if (boff > maxBoff) maxBoff = boff;
                davgs[i / 4 - 1] = davg;
                doffs[i / 4 - 1] = doff;          
            }

            else {
                firstLeft = channels[0];
                firstRight = channels[1];
            }

            davg = avg;
            doff = off;
        }
                    
        byte[] compressed = new byte[audio.length * 2];
        int bCounter = 0, cLength = 0;

        for (int i = 0; i < davgs.length - 1; i ++) {
            int absintDavg = (int) Math.abs(davgs[i]);
            absintDavg <<= 1;
            
            absintDavg |= (davgs[i] * 2 % 2 == 0 ? 0 : 1);
            absintDavg |= (davgs[i] >= 0 ? 0 : 1) << (maxBavg - 1);

            char[] binaryAvg = String.format("%" + maxBavg + "s", Integer.toString(absintDavg, 2)).replace(' ', '0').toCharArray();

            for (int j = 0; j < binaryAvg.length; j ++) {
                if (binaryAvg[j] == '1') compressed[cLength] |= 1 << (7 - bCounter);
                if (bCounter < 7) bCounter ++;

                else {
                    bCounter = 0;
                    cLength ++;
                }
            }

            int absintDoff = (int) Math.abs(doffs[i]);

            absintDoff |= (doffs[i] >= 0 ? 0 : 1) << (maxBoff - 1);

            char[] binaryOff = String.format("%" + maxBoff + "s", Integer.toString(absintDoff, 2)).replace(' ', '0').toCharArray();

            for (int j = 0; j < binaryOff.length; j ++) {
                if (binaryOff[j] == '1') compressed[cLength] |= 1 << (7 - bCounter);
                if (bCounter < 7) bCounter ++;

                else {
                    bCounter = 0;
                    cLength ++;
                }
            }
        }

        if (bCounter != 0) cLength ++;

        compressed = Arrays.copyOf(compressed, cLength);

        byte[] header = {(byte) (pcktCount >> 8), (byte) (pcktCount), (byte) (((maxBavg - 3) << 4) + (maxBoff - 2)),
                (byte) (firstLeft >> 8), (byte) (firstLeft), (byte) (firstRight >> 8), (byte) (firstRight)};

        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
        baoStream.write(header);
        baoStream.write(compressed);
        byte[] buf = baoStream.toByteArray();
        return buf;
    }

    public static Runnable streamMusic(int request, Socket socket, Socket parallelSocket) throws IOException, UnsupportedAudioFileException, InterruptedException {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    ConcurrentLinkedQueue<OutputStream> clQueue = new ConcurrentLinkedQueue<>();
                    clQueue.add(socket.getOutputStream());
                    clQueue.add(parallelSocket.getOutputStream());
                    int pcktCount = 0;
                    DatagramSocket dSocket = new DatagramSocket();
                    String ip = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                    String songName = musicList.get(request - 2);
                    File song = new File(MUSIC_PATH + songName);
                    AudioInputStream aiStream = AudioSystem.getAudioInputStream(song);
                    AudioFormat aFormat = aiStream.getFormat();
                    int duration = (int) (aiStream.getFrameLength() / aFormat.getFrameRate());

                    byte[] audio = new byte[PCKT_SIZE];
                    System.out.println("> Streaming " + songName + " (" + song.length() + " bytes | "
                            + timeFormat(duration * 1000) + ") to " + ip+ " & "
                            + parallelSocket.getInetAddress().getHostAddress() + ":" + parallelSocket.getPort() +"\n");

                    while ((aiStream.read(audio, 0, audio.length)) >= 0 && playingId == request) {
                        TimeUnit.MILLISECONDS.sleep(20);
                        byte[] buf = compress(audio, pcktCount);
                        
                        System.out.println("> TCP sending to Gateway @" + ip + " & "
                                + parallelSocket.getInetAddress().getHostAddress() + ":"
                                + parallelSocket.getPort() + ": "
                                + String.format("%02X%02X %01X %01X %02X%02X %02X%02X ... ",
                                buf[0], buf[1], ((buf[2] & 0xFF) >> 4), (buf[2] & 0xF), buf[3], buf[4],
                                buf[5], buf[6]) + "(" + buf.length + " bytes)");

                        for (OutputStream oStream : clQueue) {
                            oStream.write(buf, 0, buf.length);
                        }

                        pcktCount ++;
                    }

                    while(dSocket.isBound());
                    dSocket.close();
                }
                
                catch (Exception e) {
                    e.printStackTrace();
                }
            }   
        };
    }
}