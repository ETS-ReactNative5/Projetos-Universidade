package buffalo.obu;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import imtt.Alert;
import imtt.LimitedQueue;
import imtt.Utils;

public class OBU {
    private static final String MULTICAST = "FF02::1", FILE_PLATE = "licence_plate",
            P12_PASSWORD = "totallysecurepassword", FILE_CA = "certificates/buffalo_ca.cert";
    private static PrivateKey prKey;
    private static byte[] callSymKey;
    private static PublicKey caKey, sharedLocationKey, callKey;
    private static CertificateFactory cFactory;
    private static final int PORT = 1337, GPS_PORT = 2337, BRAND_ID = 1, PCKT_SIZE = 20000;
    private static final LimitedQueue<String> sentMessages = new LimitedQueue<>(256);
    private static final HashMap<Integer, Alert> alerts = new HashMap<>();
    private static String myPlate;
    private static int myNodeId;
    private static MulticastSocket mSocket;
    private static InetAddress mcAddress;
    private static AudioFormat aFormat;
    private static boolean outdatedLocation = false, sharingLocation = false, maintenanceAlert = false;
    private static double latitude = 0, longitude = 0;
    private static String newestAlert = "", callVoiceIntensity = "", callAudioIntensity = "", sharedLocationPlate = "",
            callPlate = "";
    private static int listing = 0, calling = 0, sharedDistance = -1, sharedBearing = -1, alertCounter = 0;
    private static long lastMaintenance = 0, nextMaintenance = 0;

    private static void updateScreen() {
        Utils.clearScreen();
        System.out.println("\n=== [ " + Utils.RED + "Buffalo" + Utils.NF + " OBU " + Utils.stylePlate(myPlate)
                + "] ===\n\n" + "\t--- [ " + Utils.BOLD + "Main menu" + Utils.NF + " ] ---\n\n" + Utils.CYAN + "[1]"
                + Utils.NF + " List of active alerts\n"
                + (calling == 0
                        ? Utils.CYAN + "[2 " + Utils.ITALIC + "<vehicle plate>" + Utils.NF + Utils.CYAN + "]" + Utils.NF
                                + " Call user\n"
                        : Utils.CYAN + "[2]" + Utils.NF + " End call\n")
                + (!sharingLocation
                        ? Utils.CYAN + "[3 " + Utils.ITALIC + "<vehicle plate>" + Utils.NF + Utils.CYAN + "]" + Utils.NF
                                + " Share location with user\n"
                        : Utils.CYAN + "[3]" + Utils.NF + " End location sharing\n")
                + Utils.CYAN + "[4]" + Utils.NF + " Do maintenance\n" + Utils.CYAN + "[5]" + Utils.NF
                + " List of stored certificates");
        System.out.print(Utils.CYAN + ">>>" + Utils.NF + " " + Utils.SAVE);
        System.out.println();
        printLocation();
        System.out.println();
        printMaintenanceDates();
        System.out.println();
        printNewestAlert();
        System.out.println();
        printCall();
        System.out.println();
        printSharedLocation();
        System.out.print(Utils.LOAD);
    }

    private static void printLocation() {
        if (latitude == 0) {
            System.out.print("[Location] Calculating..." + Utils.CLRLN);
        }

        else if (outdatedLocation) {
            System.out.print(Utils.B_YELLOW + Utils.BLACK + "[Location] " + latitude + "º, " + longitude + "º"
                    + Utils.CLRLN + Utils.NF);
        }

        else {
            System.out.print(Utils.B_GREEN + Utils.BLACK + "[Location] " + latitude + "º, " + longitude + "º"
                    + Utils.CLRLN + Utils.NF);
        }
    }

    private static void printMaintenanceDates() {
        if (!maintenanceAlert) {
            System.out.print("[Scheduled maintenance] Not needed for a while" + Utils.CLRLN);
        }

        else {
            if (nextMaintenance < Utils.now()) {
                System.out.print(Utils.B_RED + Utils.BLACK + "[Scheduled maintenance surpassed] "
                        + Utils.date(nextMaintenance).split(" ")[0] + " (last maintenance: "
                        + (lastMaintenance > 0 ? Utils.date(lastMaintenance) : "never") + ")" + Utils.CLRLN + Utils.NF);
            }

            else {
                System.out.print(Utils.B_YELLOW + Utils.BLACK + "[Scheduled maintenance] "
                        + Utils.date(nextMaintenance).split(" ")[0] + " (last maintenance: "
                        + (lastMaintenance > 0 ? Utils.date(lastMaintenance) : "never") + ")" + Utils.CLRLN + Utils.NF);
            }
        }
    }

    private static void printNewestAlert() {
        if (alertCounter == 0) {
            System.out.print("[IMTT alerts] No active alerts" + Utils.CLRLN);
        }

        else {
            System.out.print(Utils.B_YELLOW + Utils.BLACK + "[IMTT alerts] " + newestAlert
                    + (alertCounter > 1 ? " [+" + (alertCounter - 1) + "]" : "") + Utils.CLRLN + Utils.NF);
        }
    }

    private static void printCall() {
        if (calling == 0) {
            System.out.println("[Voice call] Microphone not in use" + Utils.CLRLN);
            System.out.print("[Voice call] Not receiving audio" + Utils.CLRLN);
        }

        else if (calling == -1) {
            System.out.println(
                    "[Voice call] Waiting for " + Utils.stylePlate(callPlate) + "'s response..." + Utils.CLRLN);
            System.out.print("[Voice call] Not receiving audio" + Utils.CLRLN);
        }

        else if (calling == -2) {
            System.out.println(
                    Utils.CYAN + "[6]" + Utils.NF + " Accept " + Utils.stylePlate(callPlate) + "'s voice call");
            System.out.print("[Voice call] Not receiving audio" + Utils.CLRLN);
        }

        else {
            System.out.println(
                    Utils.B_CYAN + Utils.BLACK + "[Voice call]   " + callVoiceIntensity + Utils.CLRLN + Utils.NF);
            System.out.print(Utils.B_RED + Utils.BLACK + "[" + Utils.stylePlate(callPlate) + Utils.B_RED + Utils.BLACK
                    + "] " + callAudioIntensity + Utils.CLRLN + Utils.NF);
        }
    }

    private static void printSharedLocation() {
        if (!sharingLocation) {
            if (sharedLocationPlate.length() == 0) {
                System.out.print("[Location sharing] Disabled" + Utils.CLRLN);
            }

            else {
                System.out.print(Utils.CYAN + "[7]" + Utils.NF + " Accept " + Utils.stylePlate(sharedLocationPlate)
                        + "'s location sharing request");
            }
        }

        else if (sharedDistance == -1) {
            System.out.print("[Location sharing] Waiting for " + Utils.stylePlate(sharedLocationPlate)
                    + "'s response..." + Utils.CLRLN);
        }

        else if (sharedDistance == -2) {
            System.out.print(
                    Utils.B_PURPLE + Utils.BLACK + "[Sharing location with " + Utils.stylePlate(sharedLocationPlate)
                            + Utils.B_PURPLE + Utils.BLACK + "] Calculating..." + Utils.CLRLN + Utils.NF);
        }

        else {
            System.out.print(Utils.B_PURPLE + Utils.BLACK + "[Sharing location with "
                    + Utils.stylePlate(sharedLocationPlate) + Utils.B_PURPLE + Utils.BLACK + "] " + sharedDistance
                    + " m, " + sharedBearing + "º" + Utils.CLRLN + Utils.NF);
        }
    }

    private static void listAlerts() {
        listing = 1;
        Utils.clearScreen();
        System.out.println("\n=== [ " + Utils.RED + "Buffalo" + Utils.NF + " OBU " + Utils.stylePlate(myPlate)
                + "] ===\n\n" + "--- [ " + Utils.BOLD + "List of active " + Utils.YELLOW + "IMTT" + Utils.NF
                + Utils.BOLD + " alerts" + Utils.NF + " ] ---\n");

        int newestAlertId = 0, c = 0;
        alerts.keySet().removeIf(e -> alerts.get(e).getTtl() < Utils.now());

        for (int i : alerts.keySet()) {
            Alert alert = alerts.get(i);

            if (alert.getLatitude() == 0 && alert.getLongitude() == 0 || latitude != 0 && longitude != 0
                    && alert.getDistance(latitude, longitude) <= alert.getRadius() * 2) {
                System.out.println("> #" + i + " [" + Utils.date(alert.getTstamp()) + "] " + alert.getMessage()
                        + " at (" + alert.getLatitude() + "º, " + alert.getLongitude() + "º)"
                        + (alert.getLatitude() == 0 && alert.getLongitude() == 0
                                || alert.getDistance(latitude, longitude) <= alert.getRadius() ? ""
                                        : " (" + (alert.getDistance(latitude, longitude) - alert.getRadius()) + " m)")
                        + " until " + Utils.date(alert.getTtl()));
                c++;

                if (i > newestAlertId) {
                    newestAlertId = i;
                }
            }
        }

        alertCounter = c;

        if (newestAlertId > 0 && alertCounter > 0) {
            Alert alert = alerts.get(newestAlertId);

            if (alert.getLatitude() == 0 && alert.getLongitude() == 0
                    || alert.getDistance(latitude, longitude) <= alert.getRadius()) {
                newestAlert = "#" + newestAlertId + " " + alert.getMessage() + " until " + Utils.date(alert.getTtl());
            }

            else {
                newestAlert = "#" + newestAlertId + " " + alert.getMessage() + " at "
                        + (alert.getDistance(latitude, longitude) - alert.getRadius()) + " m until "
                        + Utils.date(alert.getTtl());
            }
        }

        else {
            alertCounter = 0;
            newestAlert = "";
            System.out.println(Utils.RED + "x No alerts found..." + Utils.NF);
        }

        System.out.print("\n" + Utils.CYAN + "[0]" + Utils.NF + " Go back\n" + Utils.CYAN + "[1]" + Utils.NF
                + " Refresh list\n" + Utils.CYAN + ">>>" + Utils.NF + " " + Utils.SAVE);
    }

    private static void callUser() {
        // calling = 0;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (callKey == null) {
                    try {
                        Certificate cert = cFactory.generateCertificate(new FileInputStream(
                                "certificates/" + callPlate.replace("-", "").toLowerCase() + ".cert"));
                        callKey = cert.getPublicKey();
                    }

                    catch (FileNotFoundException | CertificateException e) {
                        // e.printStackTrace();

                        try {
                            TimeUnit.SECONDS.sleep(1);
                            Path myCert = Paths.get("certificates/" + myPlate.replace("-", "").toLowerCase() + ".cert");
                            byte[] myCertBytes = Files.readAllBytes(myCert);
                            byte[] header = ("S " + Utils.rand(16) + " " + BRAND_ID + " allVehicles C " + callPlate
                                    + " " + myPlate + " ").getBytes();
                            ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                            baoStream.write(header);
                            baoStream.write(myCertBytes);
                            byte[] buf = baoStream.toByteArray();
                            sentMessages.add(new String(buf));
                            mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                        } catch (IOException | InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }

                byte[] header = ("S " + Utils.rand(16) + " " + BRAND_ID + " " + callPlate + " V ").getBytes();

                try {
                    byte[] encryptedPlate = Utils.encrypt(myPlate + " ", callKey);
                    DataLine.Info dlInfo = new DataLine.Info(TargetDataLine.class, aFormat);
                    TargetDataLine tdLine = (TargetDataLine) AudioSystem.getLine(dlInfo);
                    tdLine.open(aFormat);
                    tdLine.start();
                    byte[] buf = new byte[PCKT_SIZE];
                    int size = 0;

                    while (calling == 1) {
                        try {
                            if ((size = tdLine.read(buf, 0, buf.length)) > 0) {
                                buf = Arrays.copyOf(buf, size);
                                byte[] encryptedCall = Utils.symEncrypt(buf, callSymKey);
                                ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                                baoStream.write(header);
                                baoStream.write(encryptedPlate);
                                baoStream.write(encryptedCall);
                                byte[] buf2 = baoStream.toByteArray();

                                if (calling != 1) {
                                    break;
                                }

                                mSocket.send(new DatagramPacket(buf2, buf2.length, mcAddress, PORT));
                                float avgBuf = Math.abs(Utils.avg(buf, 0, buf.length) * 10);
                                callVoiceIntensity = "";

                                for (int i = 0; i <= avgBuf; i++) {
                                    callVoiceIntensity += "#";
                                }
                            }
                        }

                        catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                                | InvalidAlgorithmParameterException | IllegalBlockSizeException
                                | BadPaddingException e) {
                            callVoiceIntensity = "x Voice error!...";
                            // e.printStackTrace();
                        }

                        if (listing == 0) {
                            System.out.print(Utils.SAVE);

                            for (int i = 0; i < 4; i++) {
                                System.out.println();
                            }

                            printCall();
                            System.out.print(Utils.LOAD);
                        }
                    }

                    tdLine.close();

                }

                catch (LineUnavailableException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException
                        | NoSuchAlgorithmException | NoSuchPaddingException e) {
                    callVoiceIntensity = "x Voice error...";

                    if (listing == 0) {
                        System.out.print(Utils.SAVE);

                        for (int i = 0; i < 4; i++) {
                            System.out.println();
                        }

                        printCall();
                        System.out.print(Utils.LOAD);
                    }
                }
            }
        }).start();
    }

    private static void processSharedLocation(String plate, double lat, double lng) {
        if (sharingLocation && plate.equals(sharedLocationPlate)) {
            if (lat == 0 && lng == 0) {
                sharedDistance = sharedBearing = -2;
            }

            else {
                sharedDistance = Utils.getDistance(latitude, longitude, lat, lng);
                sharedBearing = Utils.getBearing(latitude, longitude, lat, lng);
            }
        }

        else {
            sharedLocationPlate = plate;
        }

        if (listing == 0) {
            System.out.print(Utils.SAVE);

            for (int i = 0; i < 6; i++) {
                System.out.println();
            }

            printSharedLocation();
            System.out.print(Utils.LOAD);
        }

    }

    private static void processAlert(int id, long tstamp, long ttl, double lat, double lng, int rad, String message) {
        if (ttl > Utils.now()) {
            alerts.put(id, new Alert(tstamp, ttl, lat, lng, rad, message));
        }

        else {
            alerts.remove(id);
        }

        alertVerifier();
    }

    private static void playCall(byte[] data) {
        calling = 1;

        try {
            byte[] decrypted = Utils.symDecrypt(data, callSymKey);
            InputStream iStream = new ByteArrayInputStream(decrypted);
            AudioInputStream aiStream = new AudioInputStream(iStream, aFormat, data.length / aFormat.getFrameSize());
            DataLine.Info dlInfo = new DataLine.Info(SourceDataLine.class, aFormat);
            SourceDataLine sdLine = (SourceDataLine) AudioSystem.getLine(dlInfo);
            sdLine.open(aFormat);
            sdLine.start();

            new Thread(new Runnable() {
                byte[] buf = new byte[PCKT_SIZE];

                @Override
                public void run() {
                    try {
                        int rd;

                        while ((rd = aiStream.read(buf, 0, buf.length)) != -1) {
                            if (rd > 0) {
                                sdLine.write(buf, 0, rd);
                                float avgBuf = Math.abs(Utils.avg(buf, 0, buf.length) * 10);
                                callAudioIntensity = "";

                                for (int i = 0; i <= avgBuf; i++) {
                                    callAudioIntensity += "#";
                                }
                            }
                        }

                        aiStream.close();
                        sdLine.close();
                    }

                    catch (IOException e) {
                        callAudioIntensity = "x Audio error...";
                        // e.printStackTrace();
                    }
                }
            }).start();
        }

        catch (LineUnavailableException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            // e.printStackTrace();
            callAudioIntensity = "x Audio error...";
        }

        if (listing == 0) {
            System.out.print(Utils.SAVE);

            for (int i = 0; i < 4; i++) {
                System.out.println();
            }

            printCall();
            System.out.print(Utils.LOAD);
        }

    }

    private static void doMaintenance() {
        maintenanceAlert = false;
        lastMaintenance = Utils.now();
        nextMaintenance = 0;
    }

    private static void listCertificates() {
        listing = 2;
        Utils.clearScreen();
        System.out.println("\n=== [ " + Utils.RED + "Buffalo" + Utils.NF + " OBU " + Utils.stylePlate(myPlate)
                + "] ===\n\n" + "--- [ " + Utils.BOLD + "List of stored certificates" + Utils.NF + " ] ---\n");

        File dir = new File("certificates");
        File[] certFiles = dir.listFiles();

        for (File f : certFiles) {
            try {
                Certificate cert = cFactory.generateCertificate(new FileInputStream(f));
                String subject = ((X509Certificate) cert).getSubjectDN().getName();
                String cName = subject.substring(subject.indexOf("CN") + 3);

                if (cName.contains("Buffalo")) {
                    System.out.println("> " + Utils.RED + cName + Utils.NF + " (certificate authority)");
                }

                else if (cName.equals(myPlate)) {
                    System.out.println("> " + Utils.stylePlate(cName) + " (yours)");
                }

                else {
                    System.out.println("> " + Utils.stylePlate(cName));
                }
            }

            catch (CertificateException | FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        System.out.print("\n" + Utils.CYAN + "[0]" + Utils.NF + " Go back\n" + Utils.CYAN + "[5]" + Utils.NF
                + " Refresh list\n" + Utils.CYAN + ">>>" + Utils.NF + " " + Utils.SAVE);
    }

    private static void alertVerifier() {
        int newestAlertId = 0, c = 0;
        alerts.keySet().removeIf(e -> alerts.get(e).getTtl() < Utils.now());

        for (int i : alerts.keySet()) {
            Alert alert = alerts.get(i);

            if (alert.getLatitude() == 0 && alert.getLongitude() == 0 || latitude != 0 && longitude != 0
                    && alert.getDistance(latitude, longitude) <= alert.getRadius() * 2) {
                c++;

                if (i > newestAlertId) {
                    newestAlertId = i;
                }
            }
        }

        alertCounter = c;

        if (newestAlertId > 0 && alertCounter > 0) {
            Alert alert = alerts.get(newestAlertId);

            if (alert.getLatitude() == 0 && alert.getLongitude() == 0
                    || alert.getDistance(latitude, longitude) <= alert.getRadius()) {
                newestAlert = "#" + newestAlertId + " " + alert.getMessage() + " until " + Utils.date(alert.getTtl());
            }

            else {
                newestAlert = "#" + newestAlertId + " " + alert.getMessage() + " at "
                        + (alert.getDistance(latitude, longitude) - alert.getRadius()) + " m until "
                        + Utils.date(alert.getTtl());
            }
        }

        else {
            alertCounter = 0;
            newestAlert = "";
        }

        if (listing == 0) {
            System.out.print(Utils.SAVE);

            for (int i = 0; i < 3; i++) {
                System.out.println();
            }

            printNewestAlert();
            System.out.print(Utils.LOAD);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        BufferedReader bReader = new BufferedReader(new FileReader(FILE_PLATE));
        myPlate = bReader.readLine();
        bReader.close();
        System.out.println(">>> Initializing " + myPlate + "'s Vehicular Application by " + Utils.RED + "Buffalo"
                + Utils.NF + "... ");
        KeyStore kStore = KeyStore.getInstance("PKCS12");
        kStore.load(new FileInputStream(myPlate.replace("-", "").toLowerCase() + ".p12"), P12_PASSWORD.toCharArray());
        prKey = (PrivateKey) kStore.getKey("1", P12_PASSWORD.toCharArray());
        cFactory = CertificateFactory.getInstance("X.509");
        Certificate ca = cFactory.generateCertificate(new FileInputStream(FILE_CA));
        caKey = ca.getPublicKey();
        String hostname = Utils.execReadToString("hostname");
        myNodeId = Integer.parseInt(hostname.substring(1, hostname.length() - 1));
        mcAddress = Inet6Address.getByName(MULTICAST);
        mSocket = new MulticastSocket(PORT);
        aFormat = new AudioFormat(16000.0F, 16, 1, true, false);
        updateScreen();
        new Thread(receiver()).start();
        new Thread(location()).start();
        new Thread(looper()).start();
        Scanner scanner = new Scanner(System.in);
        String nLine;

        while ((nLine = scanner.nextLine()) != null) {
            String[] params = nLine.split(" ", 2);

            if (listing == 1) {
                switch (params[0]) {
                    case "1":
                        listAlerts();
                        break;

                    default:
                        listing = 0;
                        break;
                }
            }

            else if (listing == 2) {
                switch (params[0]) {
                    case "5":
                        listCertificates();
                        break;

                    default:
                        listing = 0;
                        break;
                }
            }

            else {
                switch (params[0]) {
                    case "1":
                        listAlerts();
                        break;

                    case "2":
                        if (calling == 0) {
                            callPlate = params.length > 1 ? params[1] : myPlate;
                            calling = -1;
                        }

                        else {
                            calling = 0;
                            byte[] toCallBuf = ("S " + Utils.rand(16) + " " + BRAND_ID + " " + callPlate + " VC "
                                    + myPlate + " ").getBytes();
                            callPlate = callAudioIntensity = callVoiceIntensity = "";
                            callKey = null;
                            callSymKey = null;

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = 0; i < 5; i++) {
                                        try {
                                            mSocket.send(
                                                    new DatagramPacket(toCallBuf, toCallBuf.length, mcAddress, PORT));
                                            TimeUnit.SECONDS.sleep(1);
                                        }

                                        catch (IOException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }).start();
                        }

                        break;

                    case "3":
                        if (!sharingLocation) {
                            sharedLocationPlate = params.length > 1 ? params[1] : myPlate;
                            sharingLocation = true;
                        }

                        else {
                            sharingLocation = false;
                            sharedBearing = sharedDistance = -1;
                            byte[] toShareLocBuf = ("S " + Utils.rand(16) + " " + BRAND_ID + " " + sharedLocationPlate
                                    + " LC " + myPlate + " ").getBytes();
                            sharedLocationPlate = "";
                            sharedLocationKey = null;

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = 0; i < 5; i++) {
                                        try {
                                            mSocket.send(new DatagramPacket(toShareLocBuf, toShareLocBuf.length,
                                                    mcAddress, PORT));
                                            TimeUnit.SECONDS.sleep(1);
                                        }

                                        catch (IOException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }).start();
                        }

                        break;

                    case "4":
                        doMaintenance();
                        break;

                    case "5":
                        listCertificates();
                        break;

                    case "6":
                        if (calling == -2) {
                            calling = 1;
                            callUser();
                        }

                        break;

                    case "7":
                        if (!sharingLocation && sharedLocationPlate.length() > 0) {
                            sharingLocation = true;
                        }

                        break;

                    default:
                        break;
                }
            }

            if (listing == 0) {
                updateScreen();
            }
        }

        mSocket.close();
        scanner.close();
    }

    private static Runnable looper() {
        return new Runnable() {
            @Override
            public void run() {
                while (mSocket.isBound()) {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                        alertVerifier();

                        // S <rand(16)> 2 <destinationPlate> L {<sourcePlate> <latitude> <longitude>}
                        if (sharingLocation) {
                            if (sharedLocationKey == null) {
                                try {
                                    Certificate cert = cFactory.generateCertificate(new FileInputStream("certificates/"
                                            + sharedLocationPlate.replace("-", "").toLowerCase() + ".cert"));
                                    sharedLocationKey = cert.getPublicKey();
                                }

                                catch (FileNotFoundException | CertificateException e) {
                                    // e.printStackTrace();
                                    Path myCert = Paths
                                            .get("certificates/" + myPlate.replace("-", "").toLowerCase() + ".cert");
                                    byte[] myCertBytes = Files.readAllBytes(myCert);
                                    byte[] header = ("S " + Utils.rand(16) + " " + BRAND_ID + " allVehicles C "
                                            + sharedLocationPlate + " " + myPlate + " ").getBytes();
                                    ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                                    baoStream.write(header);
                                    baoStream.write(myCertBytes);
                                    byte[] buf = baoStream.toByteArray();
                                    sentMessages.add(new String(buf));
                                    mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                                }
                            }

                            else if (sharedDistance == -1) {
                                byte[] encrypted = Utils.encrypt(myPlate, sharedLocationKey);
                                String header = "S " + Utils.rand(16) + " " + BRAND_ID + " " + sharedLocationPlate
                                        + " L ";
                                ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                                baoStream.write(header.getBytes());
                                baoStream.write(encrypted);
                                byte[] buf = baoStream.toByteArray();
                                sentMessages.add(new String(buf));
                                mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                            }

                            else {
                                byte[] encrypted = Utils.encrypt(myPlate + " " + latitude + " " + longitude,
                                        sharedLocationKey);
                                String header = "S " + Utils.rand(16) + " " + BRAND_ID + " " + sharedLocationPlate
                                        + " L ";
                                ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                                baoStream.write(header.getBytes());
                                baoStream.write(encrypted);
                                byte[] buf = baoStream.toByteArray();
                                sentMessages.add(new String(buf));
                                mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                            }
                        }

                        if (calling == -1) {
                            if (callKey == null) {
                                try {
                                    Certificate cert = cFactory.generateCertificate(new FileInputStream(
                                            "certificates/" + callPlate.replace("-", "").toLowerCase() + ".cert"));
                                    callKey = cert.getPublicKey();
                                }

                                catch (FileNotFoundException | CertificateException e) {
                                    // e.printStackTrace();
                                    Path myCert = Paths
                                            .get("certificates/" + myPlate.replace("-", "").toLowerCase() + ".cert");
                                    byte[] myCertBytes = Files.readAllBytes(myCert);
                                    byte[] header = ("S " + Utils.rand(16) + " " + BRAND_ID + " allVehicles C "
                                            + callPlate + " " + myPlate + " ").getBytes();
                                    ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                                    baoStream.write(header);
                                    baoStream.write(myCertBytes);
                                    byte[] buf = baoStream.toByteArray();
                                    sentMessages.add(new String(buf));
                                    mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                                }
                            }

                            else {
                                if (callSymKey == null) {
                                    callSymKey = Utils.generateSymKey();
                                }

                                byte[] header = ("S " + Utils.rand(16) + " " + BRAND_ID + " " + callPlate + " V ")
                                        .getBytes();
                                ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                                baoStream.write((myPlate + " ").getBytes());
                                baoStream.write(callSymKey);
                                byte[] encrypted = Utils.encrypt(baoStream.toByteArray(), callKey);
                                ByteArrayOutputStream baoStream2 = new ByteArrayOutputStream();
                                baoStream2.write(header);
                                baoStream2.write(encrypted);
                                byte[] buf = baoStream2.toByteArray();
                                sentMessages.add(new String(buf));
                                mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                            }
                        }
                    }

                    catch (InterruptedException | IOException | InvalidKeyException | IllegalBlockSizeException
                            | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private static Runnable location() {
        return new Runnable() {
            @Override
            public void run() {
                while (mSocket.isBound()) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        byte[] encrypted = Utils.encrypt(myPlate, caKey);
                        String header = "S " + Utils.rand(16) + " " + BRAND_ID + " brand O ";
                        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                        baoStream.write(header.getBytes());
                        baoStream.write(encrypted);
                        byte[] toBrandBuf = baoStream.toByteArray();
                        sentMessages.add(new String(toBrandBuf));
                        mSocket.send(new DatagramPacket(toBrandBuf, toBrandBuf.length, mcAddress, PORT));
                        
                    } catch (InterruptedException | IOException | InvalidKeyException | IllegalBlockSizeException
                            | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                
            }
        };
    }

    private static Runnable receiver() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while (mSocket.isBound()) {
                        byte[] buf = new byte[PCKT_SIZE + 512];
                        DatagramPacket dPacket = new DatagramPacket(buf, buf.length);
                        mSocket.receive(dPacket);
                        buf = Arrays.copyOf(buf, dPacket.getLength());
                        String data = new String(buf);

                        if (!sentMessages.contains(data)) {
                            sentMessages.add(data);
                            String[] params = data.split(" ", 4);

                            if (params[0].equals("A")) {
                                int id = Integer.parseInt(params[2]);
                                params = params[3].split(" ", 6);

                                processAlert(id, Long.parseLong(params[0]), Long.parseLong(params[1]),
                                        Double.parseDouble(params[2]), Double.parseDouble(params[3]), Integer.parseInt(params[4]), params[5]);
                                mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                            }

                            else if (params[0].equals("S") && Integer.parseInt(params[2]) == BRAND_ID) {
                                params = params[3].split(" ", 3);
                                
                                if (params[0].equals("allVehicles")) {
                                    if (params[1].equals("C")) {
                                        params = params[2].split(" ", 3);
                                        String requestedPlate = params[0];
                                        String senderPlate = params[1];
                                        String senderCert = params[2];
                                        File senderCertFile = new File("certificates/" + senderPlate.replace("-", "").toLowerCase() + ".cert");
                                        
                                        if (senderCertFile.createNewFile()) {
                                            FileWriter fWriter = new FileWriter(senderCertFile);
                                            fWriter.write(senderCert);
                                            fWriter.close();
                                        }

                                        if (requestedPlate.equals(myPlate)) {
                                            File myCert = new File("certificates/" + myPlate.replace("-", "").toLowerCase() + ".cert");
                                            byte[] myCertBytes = Files.readAllBytes(myCert.toPath());
                                            byte[] header = ("S " + Utils.rand(16) + " " + BRAND_ID + " allVehicles C - "
                                                    + myPlate + " ").getBytes();
                                            ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                                            baoStream.write(header);
                                            baoStream.write(myCertBytes);
                                            byte[] buf2 = baoStream.toByteArray();
                                            sentMessages.add(new String(buf2));
                                            mSocket.send(new DatagramPacket(buf2, buf2.length, mcAddress, PORT));
                                        }
                                    }

                                    mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                                }

                                else if (params[0].equals(myPlate)) {
                                    if (params[1].equals("M")) {
                                        byte[] encrypted = Arrays.copyOfRange(buf, Utils.ordinalIndexOf(data, " ", 5) + 1, buf.length);

                                        try {
                                            String decrypted = Utils.decrypt(encrypted, prKey);
                                            params = decrypted.split(" ");
                                            maintenanceAlert = true;
                                            lastMaintenance = Long.parseLong(params[0]);
                                            nextMaintenance = Long.parseLong(params[1]);
                                            
                                            if (listing == 0) {
                                                System.out.print(Utils.SAVE);

                                                for (int i = 0; i < 2; i ++) {
                                                    System.out.println();
                                                }

                                                printMaintenanceDates();
                                                System.out.print(Utils.LOAD);
                                            }
                                        }
                                        
                                        catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                                                | IllegalBlockSizeException | BadPaddingException e) {
                                            e.printStackTrace();
                                        }                                        
                                    }
                                    
                                    else if (params[1].equals("V")) {
                                        int asymEncryptedStart = Utils.ordinalIndexOf(data, " ", 5) + 1;
                                        byte[] asymEncrypted = Arrays.copyOfRange(buf, asymEncryptedStart, asymEncryptedStart + 256);
                                        byte[] symEncrypted = Arrays.copyOfRange(buf, asymEncryptedStart + 256, buf.length);

                                        try {
                                            byte[] asymDecrypted = Utils.decryptBytes(asymEncrypted, prKey);
                                            int plateEnd = Utils.ordinalIndexOf(new String(asymDecrypted), " ", 1);
                                            String plate = new String(Arrays.copyOfRange(asymDecrypted, 0, plateEnd));
                                            byte[] key = Arrays.copyOfRange(asymDecrypted, plateEnd + 1, asymDecrypted.length);
                                            if ((calling == -1 || calling == 1) && plate.equals(callPlate)) {
                                                calling = 1;
                                                
                                                if (symEncrypted.length > 5 && callSymKey != null) {
                                                    playCall(symEncrypted);
                                                }
                                            }

                                            else {
                                                callPlate = plate;
                                                callSymKey = key;
                                                calling = -2;

                                                if (listing == 0) {
                                                    System.out.print(Utils.SAVE);
                                        
                                                    for (int i = 0; i < 4; i++) {
                                                        System.out.println();
                                                    }
                                        
                                                    printCall();
                                                    System.out.print(Utils.LOAD);
                                                }
                                            }                                            
                                        }
                                        
                                        catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                                                | IllegalBlockSizeException | BadPaddingException e) {
                                            e.printStackTrace();
                                        }
                                        
                                    }
                                    
                                    else if (params[1].equals("VC")) {                                        
                                        calling = 0;
                                        callPlate = callAudioIntensity = callVoiceIntensity = "";

                                        if (listing == 0) {
                                            updateScreen();
                                        }
                                    }

                                    else if (params[1].equals("L")) {
                                        byte[] encrypted = Arrays.copyOfRange(buf, Utils.ordinalIndexOf(data, " ", 5) + 1, buf.length);                                     
                                        
                                        try {
                                            String decrypted = Utils.decrypt(encrypted, prKey);
                                            params = decrypted.split(" ");
                                            processSharedLocation(params[0], params.length > 2 ? Double.parseDouble(params[1]) : 0.0,
                                                    params.length > 2 ? Double.parseDouble(params[2]) : 0.0);
                                        }
                                        
                                        catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                                                | IllegalBlockSizeException | BadPaddingException e) {
                                            e.printStackTrace();
                                        }
                                        
                                    }

                                    else if (params[1].equals("LC")) {                             
                                        sharingLocation = false;
                                        sharedLocationPlate = "";
                                        sharedLocationKey = null;
                                        sharedBearing = sharedDistance = -1;

                                        if (listing == 0) {
                                            updateScreen();
                                        }
                                    }
                                }

                                else {
                                    mSocket.send(new DatagramPacket(buf, buf.length, mcAddress, PORT));
                                }
                            }

                            else {
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