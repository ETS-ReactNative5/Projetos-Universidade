package imtt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Scanner;

//  export CLASSPATH=".:/home/edsantos/PTI2/mysql-connector-java-8.0.19.jar"

public class Host {
    private static final int PORT = 1336;
    private static final String ADDR_OBU_PREFIX = "2001:690:2280:821:", FILE_BRANDS = "imtt/brands.txt",
            FILE_DNS = "/etc/bind/db.auto.pt",
            DB_DRIVER = "com.mysql.cj.jdbc.Driver",
            DB_URL = "jdbc:mysql://localhost:3306/IMTT?serverTimezone=UTC",
            DB_USER = "root", DB_PASSWORD = "root";
    private static final TCPMessage content = new TCPMessage();
    private static final HashMap<InetAddress, Brand> brands = new HashMap<>();
    private static final HashMap<Integer, InetAddress> brandAddresses = new HashMap<>();
    private static int preInputMessages = 0;

    public static void loadBands() throws IOException {
        BufferedReader bReader = new BufferedReader(new FileReader(FILE_BRANDS));
        String rLine;
        System.out.println("\n     --- [ " + Utils.BOLD + "Loading brands" + Utils.NF + " ] ---\n");

        for (int i = 1; (rLine = bReader.readLine()) != null; i++) {
            String[] rlSplit = rLine.split(" ");
            String brandName = rlSplit[1];

            if (rlSplit.length > 1) {
                try {
                    InetAddress brandAddress = InetAddress.getByName(rlSplit[0]);
                    brands.put(brandAddress, new Brand(i, brandName));
                    brandAddresses.put(i, brandAddress);
                    System.out.println("> #" + i + " " + brandName + "\t@ " + brandAddress.getHostName());
                }

                catch (UnknownHostException e) {
                    System.out.println(Utils.RED + "x #" + i + " " + Utils.STRIKE + brandName + Utils.NF + Utils.RED + "\t| Name or service unknown for " + rlSplit[0] + Utils.NF);
                }
            }
        }

        bReader.close();
    }

    public static void menu() {
        System.out.println("\n\t--- [ " + Utils.BOLD + "Main menu" + Utils.NF + " ] ---\n\n"
                + Utils.CYAN + "[0]" + Utils.NF + " Main menu\n"
                + Utils.CYAN + "[1]" + Utils.NF + " List of loaded brands\n"
                + Utils.CYAN + "[2]" + Utils.NF + " List of registered vehicles\n"
                + Utils.CYAN + "[3]" + Utils.NF + " List of active alerts\n"
                + Utils.CYAN + "[4 " + Utils.ITALIC + "<message>" + Utils.NF + Utils.CYAN + "]" + Utils.NF + " Send custom alert without params\n"
                + Utils.CYAN + "[5 " + Utils.ITALIC + "<ttl> <lat> <lng> <radius> <message>" + Utils.NF + Utils.CYAN + "]" + Utils.NF + " Send custom alert with params\n"
                + Utils.CYAN + "[6 " + Utils.ITALIC + "<id> (<ttl> <lat> <lng> <radius> <message>)" + Utils.NF + Utils.CYAN + "]" + Utils.NF + " Update existing alert\n"
                + Utils.CYAN + "[7 " + Utils.ITALIC + "<id>" + Utils.NF + Utils.CYAN + "]" + Utils.NF + " Disable alert");
    }
    
    public static void listBrands() {
        System.out.println("\n     --- [ " + Utils.BOLD + "List of brands" + Utils.NF + " ] ---\n");

        for (InetAddress i: brands.keySet()) {
            Brand brand = brands.get(i);
            System.out.println("> #" + brand.getId() + " " + brand.getName() + "\t@ " + i.getHostName());
        }

        System.out.println();
    }

    public static void listVehicles() {
        System.out.println("\n--- [ " + Utils.BOLD + "List of registered vehicles" + Utils.NF + " ] ---\n");

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement statement = connection.createStatement();
            String query = "select * from Vehicle;";
            ResultSet rSet = statement.executeQuery(query);
            boolean emptyList = true;

            while (rSet.next()) {
                emptyList = false;
                String plate = rSet.getString("licencePlate");
                int brandId = rSet.getInt("brandId");
                String country = rSet.getString("country");
                int year = rSet.getInt("year");
                int month = rSet.getInt("month");
                Brand brand = brands.get(brandAddresses.get(brandId));
                System.out.println("> " + Utils.B_BLUE + " " + Utils.WHITE + country + Utils.BLACK + " "
                        + Utils.B_WHITE + " " + plate + " "
                        + Utils.B_YELLOW + (year > 0 ? " " + Utils.format(year) + "/" + Utils.format(month) + " " : " ")
                        + Utils.NF + "\t| " + brand.getName());
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

    public static void listAlerts() {
        System.out.println("\n--- [ " + Utils.BOLD + "List of active alerts" + Utils.NF + " ] ---\n");

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
                System.out.println("> #" + id + " [" + tstamp + "] " + message + " at (" + lat + "º, " + lng + "º) ±" + rad + " m until " + ttl);
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

    public static void sendAlert(long ttl, double lat, double lng, int rad, String message) {       
        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String query = "insert into Alert(ttl, latitude, longitude, radius, message) values(from_unixtime(?), ?, ?, ?, ?)";
            PreparedStatement pStatement = connection.prepareStatement(query);
            pStatement.setLong(1, ttl);
            pStatement.setDouble(2, lat);
            pStatement.setDouble(3, lng);
            pStatement.setInt(4, rad);
            pStatement.setString(5, message);
            pStatement.executeUpdate();
            query = "select last_insert_id() as id, from_unixtime(" + ttl + ") as ttl";
            Statement statement = connection.createStatement();
            ResultSet rSet = statement.executeQuery(query);
            rSet.next();
            int id = rSet.getInt("id");
            String ttlString = rSet.getString("ttl");
            content.setMessage("A " + id + " " + ttl + " " + lat + " " + lng + " " + rad + " " + message);
            System.out.println("> Sending alert: #" + id + " " + message + " at (" + lat + "º, " + lng + "º) ±" + rad + " m until " + ttlString + "\n");
        }
        
        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

    }

    public static void updateAlert(int id, String[] params) {
        if (params.length > 0) {
            try {
                Class.forName(DB_DRIVER);
                Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String query = "";
                
                switch (params.length) {
                    case 1:
                        query = "update Alert set ttl = from_unixtime(?) where id = ?";
                        break;

                    case 2:
                        query = "update Alert set ttl = from_unixtime(?), lat = ? where id = ?";
                        break;

                    case 3:
                        query = "update Alert set ttl = from_unixtime(?), lat = ?, lng = ? where id = ?";
                        break;

                    case 4:
                        query = "update Alert set ttl = from_unixtime(?), lat = ?, lng = ?, rad = ? where id = ?";
                        break;

                    case 5:
                        query = "update Alert set ttl = from_unixtime(?), lat = ?, lng = ?, rad = ?, message = ? where id = ?";
                        break;
                    
                    default:
                        break;
                }

                PreparedStatement pStatement = connection.prepareStatement(query);            
                long ttl = Utils.now() + Integer.parseInt(params[0]);
                pStatement.setLong(1, ttl);

                if (params.length > 1) {
                    double lat = Double.parseDouble(params[1]);
                    pStatement.setDouble(2, lat);

                    if (params.length > 2) {
                        double lng = Double.parseDouble(params[2]);
                        pStatement.setDouble(3, lng);

                        if (params.length > 3) {
                            int rad = Integer.parseInt(params[3]);
                            pStatement.setInt(4, rad);

                            if (params.length > 4) {
                                String message = params[4];
                                pStatement.setString(5, message);
                            }
                        }
                    }
                }
                
                pStatement.setInt(params.length + 1, id);
                pStatement.executeUpdate();
                query = "select ttl, latitude, longitude, radius, message from Alert where id = " + id;
                Statement statement = connection.createStatement();
                ResultSet rSet = statement.executeQuery(query);
                
                if (rSet.next()) {
                    String ttlString = rSet.getString("ttl");
                    double lat = rSet.getDouble("latitude");
                    double lng = rSet.getDouble("longitude");
                    int rad = rSet.getInt("radius");
                    String message = rSet.getString("message");
                    content.setMessage("A " + id + " " + ttl + " " + lat + " " + lng + " " + rad + " " + message);
                    System.out.println("> Updating alert: #" + id + " " + message + " at (" + lat + "º, " + lng + "º) ±" + rad + " m until " + ttlString + "\n");
                }

                else {
                    System.out.println(Utils.RED + "x Alert #" + id + " not found...\n" + Utils.NF);
                }
            }
            
            catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void registerVehicle(String address, String licencePlate, String country, int year, int month, Socket socket, Brand brand) throws IOException {
        PrintWriter pWriter = new PrintWriter(socket.getOutputStream(), true);    
        InetAddress iAddress = InetAddress.getByName(address);
        String yearS = "" + year;
        yearS = Utils.format(Integer.parseInt(yearS.length() > 2 ? yearS.substring(yearS.length() - 2) : yearS));
        String monthS = Utils.format(month);
        preInputMessages ++;
        Utils.printon(preInputMessages, "> Registering " + Utils.B_BLUE + " " + Utils.WHITE + country + Utils.BLACK + " " + Utils.B_WHITE + " " + licencePlate + " " + Utils.B_YELLOW + (year > 0 ? " " + yearS + "/" + monthS + " " : " ") + Utils.NF + " by " + brand.getName());

        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String query = "insert into Vehicle values(?, ?, ?, ?, ?)";
            PreparedStatement pStatement = connection.prepareStatement(query);
            pStatement.setString(1, licencePlate);
            pStatement.setInt(2, brand.getId());
            pStatement.setString(3, country);
            pStatement.setInt(4, year);
            pStatement.setInt(5, month);
            pStatement.executeUpdate();

            File dnsFile = new File(FILE_DNS);

            if (dnsFile.exists()) {
                BufferedWriter bWriter = new BufferedWriter(new FileWriter(dnsFile, true));
                bWriter.write("\n" + licencePlate.replace("-", "").toLowerCase() + "\tIN\tAAAA\t" + iAddress.getHostAddress());
                bWriter.close();
                new ProcessBuilder("bash", "-c", "/etc/init.d/bind9 restart").start();
                String dnsAddress = licencePlate.replace("-", "").toLowerCase() + ".auto.pt";
                preInputMessages ++;
                Utils.printon(preInputMessages, "> " + Utils.stylePlate(licencePlate) + "'s address (" + iAddress.getHostAddress() + ") registered @" + dnsAddress);
                pWriter.println("D " + licencePlate + ", " + dnsAddress);
            }

            else {
                preInputMessages ++;
                Utils.printon(preInputMessages, "" + Utils.RED + "x DNS File (" + FILE_DNS + ") not found..." + Utils.NF);
                pWriter.println("E Problem registering vehicle - DNS File not found");
            }
        }
        
        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(">>> Initializing " + Utils.YELLOW + "IMTT" + Utils.NF + "'s Hosting Application... ");        
        Utils.clearScreen();
        System.out.println("\n\n\n\t=== [ " + Utils.YELLOW + "IMTT" + Utils.NF + " Host ] ===");
        loadBands();
        menu();
        new Thread(input()).start();
        ServerSocket sSocket = new ServerSocket(PORT);
        System.out.print(Utils.SAVE + "\n> TCP Server initialized - listening on port " + PORT + "\n"
                + "> Waiting for brand new connections..." + Utils.LOAD);
        preInputMessages += 2;

        while (sSocket.isBound()) {
            Socket socket = sSocket.accept();
            Brand brand = brands.get(socket.getInetAddress());
            preInputMessages ++;
            Utils.printon(preInputMessages, "> Brand new connection: #" + brand.getId() + " " + brand.getName());
            Thread receiver = new Thread(receiver(socket, brand));
            receiver.start();
            new Thread(sender(socket, receiver)).start();
        }

        sSocket.close();
    }

    public static Runnable input() {
        return new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);
                String data;
                System.out.print(Utils.CYAN + ">>>" + Utils.NF + " ");

                while ((data = scanner.nextLine()) != null) {
                    for (int i = 0; i < preInputMessages; i ++) {
                        System.out.println();
                    }
                    
                    preInputMessages = 0;
                    String[] params = data.split(" ", 2);

                    switch (params[0]) {
                        case "0":
                            menu();
                            break;

                        case "1":
                            listBrands();
                            break;

                        case "2":
                            listVehicles();
                            break;

                        case "3":
                            listAlerts();
                            break;
                        
                        case "4":
                            sendAlert(Utils.now() + 60, 0, 0, 0, params[1]);
                            break;

                        case "5":
                            params = params[1].split(" ", 5);

                            try {
                                long ttl = Utils.now() + Integer.parseInt(params[0]);
                                double lat = Double.parseDouble(params[1]);
                                double lng = Double.parseDouble(params[2]);
                                int rad = Integer.parseInt(params[3]);
                                sendAlert(ttl, lat, lng, rad, params[4]);
                            }

                            catch (NumberFormatException e) {
                                System.out.println(Utils.RED + "x Invalid parameters for alert...\n" + Utils.NF);
                            }

                            break;
                                            
                        case "6":
                            if ((params = params[1].split(" ", 2)).length > 1) {
                                int id = Integer.parseInt(params[0]);
                                
                                if ((params = params[1].split(" ", 5)).length > 0) {
                                    updateAlert(id, params);
                                }

                                else {
                                    System.out.println(Utils.RED + "x Incomplete parameters for alert...\n" + Utils.NF);
                                }
                            }

                            else {
                                System.out.println(Utils.RED + "x Incomplete parameters for alert...\n" + Utils.NF);
                            }

                            break;
                        
                        case "7":
                            if (params.length > 1) {
                                updateAlert(Integer.parseInt(params[1]), new String[]{"0"});
                            }

                            else {
                                System.out.println(Utils.RED + "x Invalid parameters for alert...\n" + Utils.NF);
                            }

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

    public static Runnable receiver(Socket socket, Brand brand) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String data;

                    while ((data = bReader.readLine()) != null) {
                        String[] params = data.split(" ", 2);

                        switch (params[0]) {
                            case "V": // vehicle registration
                                // V <IPv6address> <licencePlate> <country> <year> <month>
                                params = params[1].split(" ");
                                registerVehicle(ADDR_OBU_PREFIX + params[0], params[1], params[2], Integer.parseInt(params[3]), Integer.parseInt(params[4]), socket, brand);
                                break;

                            case "A": // brand alert
                                // A <latitude> <longitude> <radius> <message>
                                params = params[1].split(" ", 4);
                                sendAlert(Utils.now() + 18000, Double.parseDouble(params[0]), Double.parseDouble(params[1]), Integer.parseInt(params[2]), params[3]);
                                break;
                            
                            default:
                                break;
                        }

                    }
                }
                
                catch (IOException e) {
                    e.printStackTrace();
                }

                preInputMessages ++;
                Utils.printon(preInputMessages, Utils.RED + "x Brand disconnection: #" + brand.getId() + " " + brand.getName() + Utils.NF);
            }
        };
    }

    public static Runnable sender(Socket socket, Thread receiver) {
        return new Runnable() {
			@Override
			public void run() {
                try {
                    PrintWriter pWriter = new PrintWriter(socket.getOutputStream(), true);
                    
                    while (receiver.isAlive()) {
                        String data = content.getMessage();
                        pWriter.println(data);
                    }
                }

                catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
			}
        };
    }
}