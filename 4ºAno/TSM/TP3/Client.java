import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Client {
    private static final int PCKT_SIZE = 8820, SV_PORT = 5000, BC_PORT = 6001;

    // ANSI escape codes
    public static final String NF = "\u001b[0m", RVRSD = "\u001b[7m", B_RED = "\u001b[41m", CLRLN = "\u001b[0K",
            BEGLN = "\u001b[0E";

    private static AudioFormat aFormat = new AudioFormat(44100.0F, 16, 2, true, false);
    private static DataLine.Info dlInfo = new DataLine.Info(SourceDataLine.class, aFormat);
    private static SourceDataLine sdLine;
    private static byte[][] buffer = new byte[0xFFFF][PCKT_SIZE];
    private static ArrayList<Integer> storedSNs = new ArrayList<>();
    private static boolean playing = false;
    private static int maxSN = 0, playingId = 0, currentSN = 0, musicLength = 0;
    private static Socket socket;
    private static ArrayList<Music> musicList = new ArrayList<Music>();
    private static Thread playSoundThread = null;

    public static String timeFormat(int ms) {
        return new SimpleDateFormat("mm:ss").format(new Date((long) ms));
    }

    public static synchronized void printAndBar(String str) {
        System.out.println(BEGLN + CLRLN + str);
        System.out.print("[" + timeFormat(currentSN * 50) + "]");
        int c = 0, b = 0, l = 0;        
        
        if (musicLength > 0) {
            for (c = 0; c < (float) currentSN / musicLength * 50; c ++) {
                System.out.print(B_RED + " " + NF);
            }

            for (b = c; b < (float) maxSN / musicLength * 50; b ++) {
                System.out.print(RVRSD + " " + NF);
            }
        }

        for (l = b; l < 50; l ++) {
            System.out.print(" ");
        }

        System.out.print("[" + timeFormat(musicLength * 50) + "]");
    }

    public static Runnable playSound()
            throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException {
        return new Runnable() {
            @Override
            public void run() {

                try {
                    sdLine.open(aFormat);
                    sdLine.start();
                    printAndBar("> Starting reproduction");

                    for (; currentSN < musicLength && playing; currentSN ++) {
                        byte[] stream = buffer[currentSN];
                        InputStream iStream = new ByteArrayInputStream(stream);
                        AudioInputStream aiStream;
                        aiStream = new AudioInputStream(iStream, aFormat, stream.length / aFormat.getFrameSize());
                        int aisRead = 0;
                        byte[] buf = new byte[sdLine.getBufferSize()];

                        try {
                            printAndBar("> Playing " + String.format("%04X", currentSN));

                            while ((aisRead = aiStream.read(buf, 0, buf.length)) >= 0) {
                                sdLine.write(buf, 0, aisRead);
                            }

                            aiStream.close();
                        }

                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    playing = false;
                    printAndBar("> Reproduction end at " + String.format("%04X", currentSN));
                    sdLine.drain();
                    sdLine.stop();
                }

                catch (LineUnavailableException e) {
                    e.printStackTrace();
                }

            }
        };

    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        socket = new Socket(args[0], SV_PORT);
        printAndBar("> Connected to TCP Gateway server @" + socket.getInetAddress().getHostAddress() + ":"
                + socket.getPort());

        byte[] musicBytes = new byte[PCKT_SIZE];
        int listLength = socket.getInputStream().read(musicBytes);
        musicBytes = Arrays.copyOf(musicBytes, listLength);
        String listString = "";

        for (int i = 0; i < musicBytes.length;) {
            int duration = ((musicBytes[i] << 8) + (musicBytes[i + 1] & 0xFF)) & 0xFFFF;
            String name = new String(Arrays.copyOfRange(musicBytes, i + 2, musicBytes.length)).split("/")[0];
            musicList.add(new Music(name, duration));
            listString += String.format("%04X %s / ", duration, name);
            i += name.length() + 3;
        }

        printAndBar("> TCP received from Gateway: " + listString + " (" + musicBytes.length + ")");

        printAndBar("\n--- [ List of musics ] ---");

        for (int i = 0; i < musicList.size(); i++) {
            Music music = musicList.get(i);
            printAndBar("#" + i + " " + music.getName() + " (" + timeFormat(music.getDuration() * 1000) + ")");
        }

        printAndBar("");
        
        new Thread(musicReceiver()).start();
        new Thread(msgReceiver()).start();
    }

    private static Runnable msgReceiver() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] bytes = new byte[3];
                    int length = 0;

                    while ((length = socket.getInputStream().read(bytes)) > 0) {
                        int request = bytes[0];
                        printAndBar("> TCP received from Gateway: " + String.format("%02X", bytes[0])
                                + (length > 1 ? String.format(" %02X%02X", bytes[1], bytes[2]) : "")
                                + " (" + length + ")");

                        if (request == 0) {
                            if (playing) {
                                playing = false;
                                currentSN = ((bytes[1] << 8) + (bytes[2] & 0xFF)) & 0xFFFF;
                                printAndBar("> Music paused at " + String.format("%04X", currentSN));
                            }

                            else {
                                playing = true;
                                currentSN = ((bytes[1] << 8) + (bytes[2] & 0xFF)) & 0xFFFF;
                                printAndBar("> Resuming music at " + String.format("%04X", currentSN));
                                playSoundThread = new Thread(playSound());
                                playSoundThread.start();
                            }
                        }

                        else if (request == 1) {
                            printAndBar("> Replaying music");

                            if (playing) { // caso esteja a tocar, parar e recomeçar -> replay
                                currentSN = 0;
                                playing = false;
                                playSoundThread.join();
                                System.err.println("MUSIC STOPPED!");
                            }

                            playing = true;
                            playSoundThread = new Thread(playSound());
                            playSoundThread.start();

                        }

                        else {
                            printAndBar("> Clearing");
                            maxSN = 0;
                            currentSN = 0;
                            playing = false;
                            storedSNs.clear();
                            Arrays.stream(buffer).forEach(x -> Arrays.fill(x, (byte) 0));
                            Music newMusic = musicList.get(request - 2);
                            musicLength = newMusic.getDuration() * 20;
                            printAndBar("> Playing new music #" + (request - 2) + " " + newMusic.getName() + " ("
                                    + timeFormat(newMusic.getDuration() * 1000) + ")");
                        }

                    }
                }

                catch (IOException | UnsupportedAudioFileException | LineUnavailableException
                        | InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };

    }

    public static Runnable musicReceiver() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket dSocket = new DatagramSocket(BC_PORT);
                    sdLine = (SourceDataLine) AudioSystem.getLine(dlInfo);

                    while (dSocket.isBound()) {
                        byte[] buf = new byte[PCKT_SIZE * 2];
                        DatagramPacket dPacket = new DatagramPacket(buf, buf.length);

                        try {
                            dSocket.receive(dPacket);
                            buf = Arrays.copyOf(buf, dPacket.getLength());
                            byte[] header = Arrays.copyOf(buf, 7);
                            int pcktCount = ((header[0] << 8) + (header[1] & 0xFF)) & 0xFFFF;
                            printAndBar("> UDP received from Gateway: "
                                            + String.format("%02X%02X %01X %01X %02X%02X %02X%02X ... ", header[0],
                                                    header[1], ((header[2] & 0xFF) >> 4), (header[2] & 0xF), header[3],
                                                    header[4], header[5], header[6])
                                            + "(" + dPacket.getLength() + " bytes)");

                            if (!storedSNs.contains(pcktCount) && pcktCount < maxSN + 16) {
                                storedSNs.add(pcktCount);

                                byte[] compressed = Arrays.copyOfRange(buf, 7, buf.length);

                                int maxBavg = ((header[2] & 0xFF) >> 4) + 3;
                                int maxBoff = (header[2] & 0xF) + 2;
                                int firstLeft = ((header[3] << 8) + (header[4] & 0xFF)) & 0xFFFF;
                                int firstRight = ((header[5] << 8) + (header[6] & 0xFF)) & 0xFFFF;

                                double[] davgs = new double[PCKT_SIZE / 4 - 1];
                                double[] doffs = new double[PCKT_SIZE / 4 - 1];
                                int dCounter = 0;
                                int avgCounter = 0, offCounter = -1;
                                int absintDavg = 0, absintDoff = 0;

                                for (int i = 0; i < compressed.length; i++) {
                                    char[] binary = String.format("%8s", Integer.toString(compressed[i] & 0xFF, 2))
                                            .replace(' ', '0').toCharArray();

                                    for (int j = 0; j < binary.length; j++) {
                                        if (avgCounter > -1) {

                                            if (binary[j] == '1')
                                                absintDavg |= 1 << (maxBavg - avgCounter - 1);
                                            avgCounter++;

                                            if (avgCounter >= maxBavg) {
                                                davgs[dCounter] = (absintDavg & ((1 << (maxBavg - 1)) - 1)) >> 1;

                                                if ((absintDavg >> (maxBavg - 1)) == 1)
                                                    davgs[dCounter] *= -1;
                                                if ((absintDavg & 1) == 1)
                                                    davgs[dCounter] += 0.5 * (davgs[dCounter] > 0 ? 1 : -1);
                                                avgCounter = -1;
                                                offCounter = 0;
                                            }
                                        }

                                        else {

                                            if (binary[j] == '1')
                                                absintDoff |= 1 << (maxBoff - offCounter - 1);
                                            offCounter++;

                                            if (offCounter >= maxBoff) {
                                                doffs[dCounter] = absintDoff & ((1 << (maxBoff - 1)) - 1);
                                                if ((absintDoff >> (maxBoff - 1)) == 1)
                                                    doffs[dCounter] *= -1;
                                                if ((absintDavg & 1) == 1)
                                                    doffs[dCounter] += 0.5 * (doffs[dCounter] > 0 ? 1 : -1);
                                                offCounter = -1;
                                                avgCounter = 0;
                                                absintDavg = 0;
                                                absintDoff = 0;
                                                dCounter++;
                                            }
                                        }
                                    }
                                }

                                dCounter++;

                                davgs = Arrays.copyOf(davgs, dCounter);
                                doffs = Arrays.copyOf(doffs, dCounter);

                                byte[] audio = new byte[(davgs.length + 1) * 4];
                                double davg = (double) (firstLeft + firstRight) / 2;
                                double doff = firstLeft - davg;
                                audio[0] = (byte) ((firstLeft & 0xFF) - 0xFF);
                                audio[1] = (byte) ((firstLeft >> 8) - 0x7F);
                                audio[2] = (byte) ((firstRight & 0xFF) - 0xFF);
                                audio[3] = (byte) ((firstRight >> 8) - 0x7F);

                                for (int i = 0; i < davgs.length; i++) {
                                    double avg = davg + davgs[i];
                                    double off = doff + doffs[i];
                                    int left = (int) (avg + off);
                                    int right = (int) (avg - off);
                                    audio[i * 4 + 4] = (byte) ((left & 0xFF) - 0xFF);
                                    audio[i * 4 + 5] = (byte) ((left >> 8) - 0x7F);
                                    audio[i * 4 + 6] = (byte) ((right & 0xFF) - 0xFF);
                                    audio[i * 4 + 7] = (byte) ((right >> 8) - 0x7F);

                                    davg = avg;
                                    doff = off;
                                }

                                buffer[pcktCount] = audio;
                                if (pcktCount > maxSN) {
                                    maxSN = pcktCount;
                                }
                            }

                            else {                                
                                printAndBar("> Discarding packet");
                            }
                        }
                        
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    
                    dSocket.close();                    
                }
                
                catch (IOException | LineUnavailableException e) {
                    e.printStackTrace();
                }
                
            }
        };
    }
    
}