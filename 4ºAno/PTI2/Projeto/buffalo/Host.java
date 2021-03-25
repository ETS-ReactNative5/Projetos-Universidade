package buffalo;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import imtt.TCPMessage;
import imtt.Utils;

public class Host {
    private static final int PORT_RSU = 1337, PORT_IMTT = 1336, PORT_CERTIF = 4337, BRAND_ID = 1;
    private static final String ADDR_IMTT = "imtt.auto.pt",
            DB_DRIVER = "com.mysql.cj.jdbc.Driver",
            DB_URL = "jdbc:mysql://imtt.auto.pt:3306/Buffalo?serverTimezone=UTC", DB_USER = "root", DB_PASSWORD = "root",
            FILE_P12 = "buffalo_ca.p12", P12_PASSWORD = "totallysecurepassword", FILE_CA = "certificates/buffalo_ca.cert";
    private static PrivateKey prKey;
    private static PublicKey caKey;
    private static CertificateFactory cFactory;
    private static HashMap<String, PublicKey> pbKeys = new HashMap<>();
    private static final TCPMessage content = new TCPMessage();
    private static ServerSocket sSocket;
    private static Socket imttSocket;
    private static DatagramSocket certificaterSocket;
    private static int preInputMessages = 0;

    private static void menu() {
        System.out.println("\n\t--- [ " + Utils.BOLD + "Main menu" + Utils.NF + " ] ---\n\n"
                + Utils.CYAN + "[0]" + Utils.NF + " Main menu\n"
                + Utils.CYAN + "[1]" + Utils.NF + " List of registered vehicles\n"
                + Utils.CYAN + "[2]" + Utils.NF + " List of active " + Utils.YELLOW + "IMTT" + Utils.NF + " alerts\n"
                + Utils.CYAN + "[3]" + Utils.NF + " List of all events\n"
                + Utils.CYAN + "[4 " + Utils.ITALIC + "<address> <licence plate> (<country> <year> <month>)" + Utils.NF + Utils.CYAN + "]" + Utils.NF
                        + " Register vehicle\n"
                + Utils.CYAN + "[5 " + Utils.ITALIC + "<licence plate> <yyyy-MM-dd>" + Utils.NF + Utils.CYAN + "]" + Utils.NF
                        + " Schedule vehicle maintenance date\n"
                + Utils.CYAN + "[6 " + Utils.ITALIC + "<licence plate>" + Utils.NF + Utils.CYAN + "]" + Utils.NF + " Do maintenance");
    }

    private static void listVehicles() {
        System.out.println("\n--- [ " + Utils.BOLD + "List of registered vehicles" + Utils.NF + " ] ---\n");

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement statement = connection.createStatement();
            String query = "select * from Vehicle order by lastOnline desc, nextMaintenance - current_timestamp;";
            ResultSet rSet = statement.executeQuery(query);
            boolean emptyList = true;

            while (rSet.next()) {
                emptyList = false;
                String plate = rSet.getString("licencePlate");
                String lastOnline = rSet.getString("lastOnline");
                String lastMaint = rSet.getString("lastMaintenance");
                String nextMaint = rSet.getString("nextMaintenance");

                String plateStr = Utils.stylePlate(plate), lastOnlineStr, lastMaintStr,
                        nextMaintStr;
                boolean warning = false;

                if (lastOnline == null) {
                    lastOnlineStr = "LO: " + Utils.ITALIC + "never" + Utils.NF;
                }

                else if (Utils.unix(lastOnline) < Utils.now() - 60) {
                    lastOnlineStr = "LO: " + lastOnline;
                }

                else {
                    lastOnlineStr = Utils.GREEN + "LO: " + lastOnline + Utils.NF;
                }

                if (lastMaint == null) {
                    lastMaintStr = "LM: " + Utils.ITALIC + "never" + Utils.NF;
                }

                else {
                    lastMaintStr = "LM: " + lastMaint;
                }

                if (nextMaint == null) {
                    nextMaintStr = "NM: " + Utils.ITALIC + "not scheduled" + Utils.NF;
                }

                else if (Utils.unix(nextMaint) < Utils.now()) {
                    nextMaintStr = Utils.RED + "NM: " + nextMaint.split(" ")[0] + Utils.NF;
                    warning = true;
                }

                else if (Utils.unix(nextMaint) <= Utils.now() + 604800) {
                    nextMaintStr = Utils.YELLOW + "NM: " + nextMaint.split(" ")[0] + Utils.NF;
                    warning = true;
                }

                else {
                    nextMaintStr = "NM: " + nextMaint.split(" ")[0];
                }

                System.out.println((warning ? "! " : "> ") + plateStr + " " + lastOnlineStr + ", " + lastMaintStr
                        + ", " + nextMaintStr);
            }

            if (emptyList) {
                System.out.println(Utils.RED + "x No vehicles found..." + Utils.NF);
            }

            System.out.println();
            connection.close();
            statement.close();
            rSet.close();
        }

        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void listAlerts() {
        System.out.println("\n--- [ " + Utils.BOLD + "List of active " + Utils.YELLOW + "IMTT" + Utils.NF + " alerts"
                + Utils.NF + " ] ---\n");

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement statement = connection.createStatement();
            String query = "select * from Alert where ttl > current_timestamp;";
            ResultSet rSet = statement.executeQuery(query);
            boolean emptyList = true;

            while (rSet.next()) {
                emptyList = false;
                int id = rSet.getInt("id");
                String tstamp = rSet.getString("tstamp");
                String ttl = rSet.getString("ttl");
                double lat = rSet.getDouble("latitude");
                double lng = rSet.getDouble("longitude");
                int rad = rSet.getInt("radius");
                String message = rSet.getString("message");
                System.out.println("> #" + id + " [" + tstamp + "] " + message + " at (" + lat + "º, " + lng + "º) ±"
                        + rad + " m until " + ttl);
            }

            if (emptyList) {
                System.out.println(Utils.RED + "x No alerts found..." + Utils.NF);
            }

            System.out.println();
            connection.close();
            statement.close();
            rSet.close();
        }

        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void listEvents() {
        System.out.println("\n--- [ " + Utils.BOLD + "List of events" + Utils.NF + " ] ---\n");

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement statement = connection.createStatement();
            String query = "select * from Event";
            ResultSet rSet = statement.executeQuery(query);
            boolean emptyList = true;

            while (rSet.next()) {
                emptyList = false;
                int id = rSet.getInt("id");
                String tstamp = rSet.getString("tstamp");
                String type = rSet.getString("type");
                String src = rSet.getString("source");
                String dest = rSet.getString("destination");
                String ttl = rSet.getString("ttl");
                double lat = rSet.getDouble("latitude");
                double lng = rSet.getDouble("longitude");
                int rad = rSet.getInt("radius");
                String message = rSet.getString("message");

                switch (type.charAt(0)) {
                    case 'A':
                        System.out.println("> #" + id + " [" + tstamp + "]\n"
                                + " - type: Alert #" + type.substring(1) + "\n"
                                + " - source: IMTT\n"
                                + " - destination: all vehicles\n"
                                + (ttl != null ? " - TTL: " + ttl + "\n" : "")
                                + (lat != 0 ? " - latitude: " + lat + "º\n" : "")
                                + (lng != 0 ? " - longitude: " + lng + "º\n" : "")
                                + (rad > 0 ? " - radius: " + rad + " m\n" : "")
                                + " - message: " + message + "\n");
                        break;
                    
                    case 'V':
                        System.out.println("> #" + id + " [" + tstamp + "]\n"
                                + " - type: Vehicle registration\n"
                                + " - source: Buffalo\n"
                                + " - destination: IMTT\n"
                                + " - message: " + message + "\n");
                        break;
                    
                    case 'D':
                        System.out.println("> #" + id + " [" + tstamp + "]\n"
                                + " - type: Vehicle DNS\n"
                                + " - source: IMTT\n"
                                + " - destination: Buffalo\n"
                                + " - message: " + message + "\n");
                        break;
                    
                    case 'E':
                        System.out.println("> #" + id + " [" + tstamp + "]\n"
                                + " - type: Error\n"
                                + " - source: IMTT\n"
                                + " - destination: Buffalo\n"
                                + " - message: " + message + "\n");
                        break;
                    
                    case 'M':
                        System.out.println("> #" + id + " [" + tstamp + "]\n"
                                + " - type: Maintenance\n"
                                + " - source: Buffalo\n"
                                + " - destination: " + dest + "\n");
                        break;
                    
                    case 'S':
                        System.out.println("> #" + id + " [" + tstamp + "]\n"
                                + " - type: Maintenance scheduling\n"
                                + " - source: Buffalo\n"
                                + " - destination: " + dest + "\n"
                                + " - message: " + message + "\n");
                        break;
                    
                    case 'L':
                        System.out.println("> #" + id + " [" + tstamp + "]\n"
                                + " - type: Maintenance date surpassing\n"
                                + " - source: " + src + "\n"
                                + " - destination: Buffalo\n"
                                + " - message: " + message + "\n");
                        break;

                    default:
                        break;
                }
            }

            if (emptyList) {
                System.out.println(Utils.RED + "x No events found..." + Utils.NF);
            }

            System.out.println();
            connection.close();
            statement.close();
            rSet.close();
        }

        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void registerVehicle(String[] params) {
        try {
            PrintWriter pWriter = new PrintWriter(imttSocket.getOutputStream(), true);
            String address = params[0], licencePlate = params[1], country = "P";
            int year = 0, month = 0;

            if (params.length > 2) {
                country = params[2];

                if (params.length > 3) {
                    year = Integer.parseInt(params[3]);

                    if (params.length > 4) {
                        month = Integer.parseInt(params[4]);
                    }
                }
            }

            String yearS = "" + year;
            yearS = Utils.format(Integer.parseInt(yearS.length() > 2 ? yearS.substring(yearS.length() - 2) : yearS));
            String monthS = Utils.format(month);
            System.out.println("> Registering " + Utils.B_BLUE + " " + Utils.WHITE + country + Utils.BLACK + " "
                    + Utils.B_WHITE + " " + licencePlate + " " + Utils.B_YELLOW
                    + (year > 0 ? " " + yearS + "/" + monthS + " " : " ") + Utils.NF);
            pWriter.println("V " + address + " " + licencePlate + " " + country + " " + year + " " + month);
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String query = "insert into Vehicle(licencePlate) values(?)";
            PreparedStatement pStatement = connection.prepareStatement(query);
            pStatement.setString(1, licencePlate);
            pStatement.executeUpdate();
            query = "insert into Event(type, source, destination, message) values(?, ?, ?, ?)";
            pStatement = connection.prepareStatement(query);
            pStatement.setString(1, "V");
            pStatement.setString(2, "brand");
            pStatement.setString(3, "imtt");
            pStatement.setString(4, address + ", " + licencePlate + ", " + country + ", " + yearS + ", " + monthS);
            pStatement.executeUpdate();
            //new ProcessBuilder("bash", "-c", "python3 CertificateBuilder.py " + FILE_P12 + " " + licencePlate).start();
        }

        catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void scheduleMaintenance(String plate, String date) {
        System.out.println("> Scheduling " + Utils.stylePlate(plate) + "'s maintenance to " + date + Utils.NF);

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String query = "update Vehicle set nextMaintenance = ? where licencePlate = '" + plate + "'";
            PreparedStatement pStatement = connection.prepareStatement(query);
            pStatement.setString(1, date + " 23:59:59");
            pStatement.executeUpdate();
            query = "insert into Event(type, source, destination, message) values(?, ?, ?, ?)";
            pStatement = connection.prepareStatement(query);
            pStatement.setString(1, "S");
            pStatement.setString(2, "brand");
            pStatement.setString(3, plate);
            pStatement.setString(4, date + " 23:59:59");
            pStatement.executeUpdate();
        }

        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void doMaintenance(String plate) {
        System.out.println("> Registering " + Utils.stylePlate(plate) + "'s maintenance" + Utils.NF);

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String query = "update Vehicle set lastMaintenance = current_timestamp where licencePlate = '" + plate + "'";
            PreparedStatement pStatement = connection.prepareStatement(query);
            pStatement.executeUpdate();
            query = "insert into Event(type, source, destination) values(?, ?, ?)";
            pStatement = connection.prepareStatement(query);
            pStatement.setString(1, "M");
            pStatement.setString(2, "brand");
            pStatement.setString(3, plate);
            pStatement.executeUpdate();
        }

        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void processAlert(int id, long ttl, double lat, double lng, int rad, String message) {
        String ttlDate = Utils.date(ttl);
        preInputMessages++;

        if (lat == 0 && lng == 0) {
            Utils.printon(preInputMessages,
                    "> " + Utils.YELLOW + "IMTT" + Utils.NF + " alert: #" + id + " " + message + " until " + ttlDate);
        }

        else {
            Utils.printon(preInputMessages, "> " + Utils.YELLOW + "IMTT" + Utils.NF + " alert: #" + id + " " + message
                    + " at (" + lat + "º, " + lng + "º) ±" + rad + " m until " + ttlDate);
        }

        content.setBytes(("A " + Utils.rand(16) + " " + id + " " + Utils.now() + " " + ttl + " " + lat + " " + lng
                + " " + rad + " " + message).getBytes());

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String query = "insert into Event(type, source, destination, ttl, latitude, longitude, radius, message) values(?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pStatement = connection.prepareStatement(query);
            pStatement.setString(1, "A" + id);
            pStatement.setString(2, "imtt");
            pStatement.setString(3, "allVehicles");
            pStatement.setString(4, ttlDate);
            pStatement.setDouble(5, lat);
            pStatement.setDouble(6, lng);
            pStatement.setInt(7, rad);
            pStatement.setString(8, message);
            pStatement.executeUpdate();
            query = "replace into Alert(id, ttl, maxTtl, latitude, longitude, radius, message) values (?, ?, "
                    + "greatest(ifnull((select * from(select maxTtl from Alert where id = " + id + ") tempAlert), 0), '"
                    + ttlDate + "'), ?, ?, ?, ?)";
            pStatement = connection.prepareStatement(query);
            pStatement.setInt(1, id);
            pStatement.setString(2, ttlDate);
            pStatement.setDouble(3, lat);
            pStatement.setDouble(4, lng);
            pStatement.setInt(5, rad);
            pStatement.setString(6, message);
            pStatement.executeUpdate();
        }

        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void processOnlineSignal(String plate) {
        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement statement = connection.createStatement();
            String query = "select unix_timestamp(lastOnline) as lastOnline, unix_timestamp(nextMaintenance) as nextMaintenance from Vehicle where licencePlate = '"
                    + plate + "'";
            ResultSet rSet = statement.executeQuery(query);
            boolean found = false;

            while (rSet.next()) {
                found = true;
                long oldLastOnline = rSet.getLong("lastOnline");
                long nextMaintenance = rSet.getLong("nextMaintenance");

                if (Utils.now() > nextMaintenance && oldLastOnline < nextMaintenance) {
                    preInputMessages++;
                    Utils.printon(preInputMessages,
                            Utils.RED + "! Maintenance date: " + Utils.stylePlate(plate) + Utils.RED
                                    + " as surpassed its scheduled maintenance date ("
                                    + Utils.date(nextMaintenance).split(" ")[0] + ")!" + Utils.NF);
                }
            }

            if (found) {
                query = "update Vehicle set lastOnline = current_timestamp where licencePlate = '" + plate + "'";
                PreparedStatement pStatement = connection.prepareStatement(query);
                pStatement.executeUpdate();
            }
        }

        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void vehicleDomain(String message) {
        preInputMessages++;
        Utils.printon(preInputMessages, "> Vehicle DNS: " + message + Utils.NF);

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String query = "insert into Event(type, source, destination, message) values(?, ?, ?, ?)";
            PreparedStatement pStatement = connection.prepareStatement(query);
            pStatement.setString(1, "D");
            pStatement.setString(2, "imtt");
            pStatement.setString(3, "brand");
            pStatement.setString(4, message);
            pStatement.executeUpdate();
            new ProcessBuilder("bash", "-c", "/etc/init.d/bind9 restart").start();
        }

        catch (ClassNotFoundException | SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void error(String source, String destination, String message) {
        preInputMessages++;
        Utils.printon(preInputMessages, Utils.RED + "x Error: " + message + "..." + Utils.NF);

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String query = "insert into Event(type, source, destination, message) values(?, ?, ?, ?)";
            PreparedStatement pStatement = connection.prepareStatement(query);
            pStatement.setString(1, "E");
            pStatement.setString(2, source);
            pStatement.setString(3, destination);
            pStatement.setString(4, message);
            pStatement.executeUpdate();
        }

        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, CertificateException,
            KeyStoreException, UnrecoverableKeyException {
        System.out.println(">>> Initializing " + Utils.RED + "Buffalo" + Utils.NF + "'s Hosting Application...\n");
        KeyStore kStore = KeyStore.getInstance("PKCS12");
        kStore.load(new FileInputStream(FILE_P12), P12_PASSWORD.toCharArray());
        prKey = (PrivateKey) kStore.getKey("1", P12_PASSWORD.toCharArray());
        cFactory = CertificateFactory.getInstance("X.509");
        Certificate ca = cFactory.generateCertificate(new FileInputStream(FILE_CA));
        caKey = ca.getPublicKey();
        pbKeys.put("Buffalo", caKey);
        Utils.clearScreen();
        System.out.println("\n\n\n\n\n\n\n\t=== [ " + Utils.RED + "Buffalo" + Utils.NF + " Host ] ===");
        boolean connected = false;
        String imttAddress = args.length != 0 ? args[0] : ADDR_IMTT;
        System.out.print("\n> Connecting to " + Utils.YELLOW + "IMTT" + Utils.NF + "'s TCP Server @" + imttAddress + ":"
                + PORT_IMTT + "..." + Utils.RED);

        while (!connected) {
            try {
                imttSocket = new Socket(imttAddress, PORT_IMTT);
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

        System.out.println(Utils.NF + "\n> Successfully connected to " + Utils.YELLOW + "IMTT" + Utils.NF
                + "'s Server @" + imttAddress + ":" + PORT_IMTT);
        menu();
        new Thread(input()).start();
        sSocket = new ServerSocket(PORT_RSU);
        System.out.print(Utils.SAVE + "\n> TCP Server initialized - listening on port " + PORT_RSU + "\n"
                + "> Waiting for RSU connections..." + Utils.LOAD);
        preInputMessages += 2;
        certificaterSocket = new DatagramSocket();
        certificaterSocket.setSoTimeout(5000);
        new Thread(imttReceiver()).start();
        new Thread(loopSender()).start();

        while (sSocket.isBound() && imttSocket.isBound()) {
            Socket socket = sSocket.accept();
            String[] hexatects = socket.getInetAddress().getHostName().split(":");
            preInputMessages++;
            Utils.printon(preInputMessages, "> RSU connection: 0x" + hexatects[hexatects.length - 1]);
            Thread receiver = new Thread(rsuReceiver(socket));
            receiver.start();
            new Thread(rsuSender(socket, receiver)).start();
        }

        sSocket.close();
    }

    private static Runnable loopSender() {
        return new Runnable() {
            @Override
            public void run() {
                while (sSocket.isBound() && imttSocket.isBound()) {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                        Class.forName(DB_DRIVER);
                        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                        Statement statement = connection.createStatement();
                        String query = "select id, unix_timestamp(ttl) as ttl, ttl as ttlDate, "
                                + "latitude, longitude, radius, message from Alert where maxTtl >= current_timestamp";
                        ResultSet rSet = statement.executeQuery(query);

                        while (rSet.next()) {
                            int id = rSet.getInt("id");
                            long ttl = rSet.getLong("ttl");
                            double lat = rSet.getDouble("latitude");
                            double lng = rSet.getDouble("longitude");
                            int rad = rSet.getInt("radius");
                            String message = rSet.getString("message");

                            String alert = "A " + Utils.rand(16) + " " + id + " " + Utils.now() + " " + ttl + " "
                                    + lat + " " + lng + " " + rad + " " + message;

                            content.setBytes(alert.getBytes());
                        }
                    }

                    catch (ClassNotFoundException | SQLException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {
                        TimeUnit.SECONDS.sleep(5);
                        Class.forName(DB_DRIVER);
                        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                        Statement statement = connection.createStatement();
                        String query = "select licencePlate, unix_timestamp(lastMaintenance) as lastMaintenance, "
                                + "unix_timestamp(nextMaintenance) as nextMaintenance from Vehicle where "
                                + "unix_timestamp(lastOnline) > unix_timestamp(current_timestamp) - 60 "
                                + "and unix_timestamp(nextMaintenance) < unix_timestamp(current_timestamp) + 604800";
                        ResultSet rSet = statement.executeQuery(query);

                        while (rSet.next()) {
                            String plate = rSet.getString("licencePlate");
                            long lastMaint = rSet.getLong("lastMaintenance");
                            long nextMaint = rSet.getLong("nextMaintenance");

                            try {                                
                                if (!pbKeys.containsKey(plate)) {
                                    Certificate cert = cFactory.generateCertificate(new FileInputStream("certificates/" + plate.replace("-", "").toLowerCase() + ".cert"));
                                    pbKeys.put(plate, cert.getPublicKey());
                                }
                                
                                byte[] encrypted = Utils.encrypt(lastMaint + " " + nextMaint, pbKeys.get(plate));
                                String header = "S " + Utils.rand(16) + " " + BRAND_ID + " " + plate + " M ";
                                ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                                baoStream.write(header.getBytes());
                                baoStream.write(encrypted);
                                byte[] buf = baoStream.toByteArray();
                                content.setBytes(buf);
                            }
                            
                            catch (CertificateException | InvalidKeyException
                                    | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException
                                    | NoSuchPaddingException | IOException e) {
                                e.printStackTrace();
                            }

                            TimeUnit.MILLISECONDS.sleep(100);
                        }
                    }
        
                    catch (ClassNotFoundException | SQLException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public static Runnable imttReceiver() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader bReader = new BufferedReader(new InputStreamReader(imttSocket.getInputStream()));
                    String data;
                    
                    while ((data = bReader.readLine()) != null) {
                        String[] params = data.split(" ", 2);
                        
                        switch(params[0]) {
                            case "A":
                                // A <id = auto> <ttl> <latitude> <longitude> <radius> <message>
                                params = params[1].split(" ", 6);
                                processAlert(Integer.parseInt(params[0]), Long.parseLong(params[1]), Double.parseDouble(params[2]), Double.parseDouble(params[3]), Integer.parseInt(params[4]), params[5]);
                                break;

                            case "D":
                                // D <licencePlate> <dnsAddress>
                                vehicleDomain(params[1]);
                                break;

                            case "E":
                                // E <message>
                                error("imtt", "brand", params[1]);
                                break;
                            
                            default:
                                break;
                        }
                    }
                }
                
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public static Runnable input() {
        return new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);
                String nLine;
                System.out.print(Utils.CYAN + ">>>" + Utils.NF + " ");                
                
                while ((nLine = scanner.nextLine()) != null) {
                    for (int i = 0; i < preInputMessages; i ++) {
                        System.out.println();
                    }
                    
                    preInputMessages = 0;
                    String[] params = nLine.split(" ", 2);

                    switch (params[0]) {
                        case "0":
                            menu();
                            break;

                        case "1":
                            listVehicles();
                            break;

                        case "2":
                            listAlerts();
                            break;

                        case "3":
                            listEvents();
                            break;

                        case "4":
                            if ((params = params[1].split(" ")).length > 1) {
                                registerVehicle(params);
                            }

                            else {
                                System.out.println(Utils.RED + "x Incomplete parameters for vehicle registration...\n" + Utils.NF);                                    
                            }

                            break;

                        case "5":
                            if ((params = params[1].split(" ")).length > 1) {
                                scheduleMaintenance(params[0], params[1]);
                            }

                            else {
                                System.out.println(Utils.RED + "x Incomplete parameters for maintenance scheduling...\n" + Utils.NF);                                    
                            }
                            
                            break;

                        case "6":
                            doMaintenance(params[1]);
                            break;

                        default:
                            System.out.println(Utils.RED + "x Unknown command...\n" + Utils.NF);
                            break;
                    }

                    System.out.print(Utils.CYAN + ">>>" + Utils.NF + " ");
                }

                scanner.close();
            }
        };
    }

    public static Runnable rsuReceiver(Socket socket) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buf = new byte[2048];
                    int size = 0;
                    
                    while ((size = socket.getInputStream().read(buf)) > -1) {
                        buf = Arrays.copyOf(buf, size);
                        String data = new String(buf);
                        String[] params = data.split(" ");

                        // S <rand(16)> 2 brand O {<licencePlate>}
                        if (params[0].equals("S") && params[4].equals("O")) {
                            byte[] encrypted = Arrays.copyOfRange(buf, Utils.ordinalIndexOf(data, " ", 5) + 1, buf.length);

                            try {
                                String plate = Utils.decrypt(encrypted, prKey);
                                processOnlineSignal(plate);
                            }
                            
                            catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                                    | IllegalBlockSizeException | BadPaddingException e) {
                                //e.printStackTrace();
                            }
                        }

                        buf = new byte[2048];
                    }
                }
                
                catch (IOException e) {
                    e.printStackTrace();
                }

                String[] hexatects = socket.getInetAddress().getHostName().split(":");    
                preInputMessages ++;
                Utils.printon(preInputMessages, Utils.RED + "x RSU disconnection: 0x" + hexatects[hexatects.length - 1] + Utils.NF);
            }
        };
    }

    public static Runnable rsuSender(Socket socket, Thread receiver) {
        return new Runnable() {
			@Override
			public void run() {
                try {                    
                    while (receiver.isAlive()) {
                        byte[] buf = content.getBytes();
                        socket.getOutputStream().write(buf);
                    }
                }

                catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
			}
        };
    }
}