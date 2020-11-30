package sample;

import com.sun.xml.internal.bind.api.impl.NameConverter;
import com.sun.xml.internal.stream.writers.UTF8OutputStreamWriter;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jssc.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.server.LogStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import jssc.SerialPortException;



public class Controller implements Initializable {

    static byte [] byteFileLength;
    static int fileLength;
    static String fileName = "";
    static int fileContentLength = 0;
    static boolean count = false;
    public static SerialPort serialPort;
    static byte [] byteFile;
    static ArrayList<Byte> AL = new ArrayList <Byte> ();
    static ArrayList <Byte> fileLengthName = new ArrayList <Byte> ();
    static int fileLengthCounter = 0;
    static ByteArrayOutputStream receivedStream = new ByteArrayOutputStream ();
    static ByteArrayOutputStream sentStream = new ByteArrayOutputStream ();
    static boolean firsttime = true;
    static boolean last = false;
    static boolean first = true;
    static int myTotalLength = 0;
    static long start = 0;
    static long menu = 1 ;
    static boolean startSaving = false;
    static byte[] text;
    static String sendText;
    static boolean gettype = true;
    static String PortName;
    static String type;

    boolean startSending = true;
    byte lineinbytes[]=new byte[29];
    File file ;
    public static ObservableList<String> chatList = FXCollections.observableArrayList();//create observablelist for listview
    public static ObservableList<String> portList = FXCollections.observableArrayList();

    @FXML
    private AnchorPane anchorpane;
    @FXML
    private ResourceBundle resources;
    @FXML
    public ListView<String> list;
    @FXML
    private Button send;
    @FXML
    private Button choosefile;
    @FXML
    private TextField textField;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Text progBarText;
    @FXML
    private ComboBox<String> coms;

    @FXML
    void SendTextOnTextField(ActionEvent event) throws InterruptedException, SerialPortException, IOException {
        chat();
    }
    @FXML
    void SelectFile(ActionEvent event) throws InterruptedException, SerialPortException, IOException {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");

        Stage stage = (Stage) anchorpane.getScene().getWindow();
        file = fileChooser.showOpenDialog(stage);

        if(file!=null) {
            String s = "2[]i\n";
            serialPort.writeBytes(s.getBytes());
            menu = 2;
            //list.getItems().add(file.getAbsolutePath());
            System.out.println("Length: "+file.length()+"Name: "+file.getName());;
            sendFile();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        System.out.println("init");
        list.setItems(chatList);
        String [] availablePorts = SerialPortList.getPortNames();
        portList.addAll(availablePorts);
        coms.setItems(portList);

        coms.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                PortName = newValue;
                System.out.println("com " + PortName);
                serialPort = new SerialPort (PortName);
                try {
                    serialPort.openPort ();
                    serialPort.setParams (57600, 8, 1, 0); // baud rate, data bits, stop bit, sem bit de paridade
                    serialPort.setEventsMask (SerialPort.MASK_RXCHAR);
                    serialPort.addEventListener (new SerialPortReader());

                }
                catch (SerialPortException ex) {System.out.println (ex);}
            }
        });

        textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)){
                    try {

                        chat();
                        list.scrollTo(list.getItems().size()-1);
                        //SendText();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (SerialPortException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }



    public void chat () throws SerialPortException, IOException, InterruptedException {
        String line = "";
        StringBuilder stringbuilder = new StringBuilder();
        line = textField.getText()+"\n";
        StringBuilder sb = new StringBuilder();

        for (byte b : line.getBytes()) {

            sb.append(String.format("%02X ", b));
        }
        System.out.println(sb.toString());

        if(!line.equals("\n")){
            String s = "2[]c\n";
            serialPort.writeBytes(s.getBytes());

            if (firsttime) {
                System.out.println("entrou");
                stringbuilder.append(line.length());
                stringbuilder.append("[]");
                sentStream.write(stringbuilder.toString().getBytes());
                firsttime = false;
            }
            if (line.length() > 0) {

                sentStream.write(line.getBytes());
                byte[] textline = sentStream.toByteArray();


                serialPort.writeBytes(textline);
                TimeUnit.MILLISECONDS.sleep(150);
                chatList.add("User 1: " + line);
                textField.clear();
                sentStream.reset();
                firsttime = true;
            }
        }

    }

    public void sendFile() throws IOException, SerialPortException, InterruptedException {
        int counter=0;
        int flag=0;
        int namesize = 0;

        byte [] fileContent = readBytesFromFile (file.toString ());

        while(counter < fileContent.length) {

            if(firsttime) {
                String fileLengthName = fileContent.length + "[" + file.getName() + "]";
                namesize = fileLengthName.length();
                firsttime=false;
                flag=1;
                sentStream.write(fileLengthName.getBytes ());
            }

            for(int i=0;i<29;i++) {
                if(flag==1) {
                    flag=0;
                    i=namesize;
                }
                if(counter<fileContent.length) {
                    AL.add(fileContent[counter]);
                    counter ++ ;
                }
            }

            byte subFileContent[] = new byte[AL.size()];

            for (int i = 0; i < AL.size(); i++) {
                subFileContent[i] = AL.get(i);
            }

            sentStream.write (subFileContent);
            byte [] byteFileContent = sentStream.toByteArray ();

            StringBuilder sb = new StringBuilder();

            for (byte b : byteFileContent) {

                sb.append(String.format("%02X ", b));
            }
            System.out.println(sb.toString());

            serialPort.writeBytes (byteFileContent);

            TimeUnit.MILLISECONDS.sleep(150);

            AL.clear();
            sentStream.reset ();

        }
        chatList.add("User 1: "+ file.getName()+" ("+file.length()+" B) sent.");
        firsttime = true;
        startSending = true;
    }

    public void putText(String receivedText){
        chatList.add("User 2: "+ receivedText);
    }

    private static byte [] readBytesFromFile (String filePath) {
        FileInputStream fileInputStream = null;
        byte [] bytesArray = null;
        try {
            File file = new File (filePath);
            bytesArray = new byte [(int) file.length ()];
            fileInputStream = new FileInputStream (file);
            fileInputStream.read (bytesArray);
        }
        catch (IOException e) {e.printStackTrace ();}
        finally {
            if (fileInputStream != null) {
                try {fileInputStream.close ();}
                catch (IOException e) {e.printStackTrace ();}
            }
        }
        return bytesArray;
    }

    class SerialPortReader implements SerialPortEventListener {
        private boolean startDownloading = true;
        private boolean startSaving;
        private byte[] text;
        byte buffer[];

        public void serialEvent (SerialPortEvent event) {
            System.out.println("type: "+ gettype);
            if (event.isRXCHAR () && event.getEventValue () > 0) {
                if(gettype) {
                    gettype=false;
                    try {
                        buffer = serialPort.readBytes(5);
                        StringBuilder sb = new StringBuilder();
                        for (byte b : buffer) {
                            sb.append(String.format("%02X ", b));
                        }
                        System.out.println("ok " + sb.toString());

                        String s = new String(buffer);

                        System.out.println("string: "+ s);

                        if(s.equals("2[]i\n")){
                            menu = 2;
                        }else{
                            menu = 1;
                        }
                    } catch (Exception e) {}

                }else {
                    if(menu==1) {
                        System.out.println("yeet");

                        try {

                            buffer = serialPort.readBytes();
                            for (byte b : buffer){

                                if (b == ']') startSaving = true;
                                if (b == '\n'){
                                    text = receivedStream.toByteArray();
                                    System.out.println(new String(text));
                                    Controller.chatList.add("User 2: "+new String(text));
                                    receivedStream.reset();
                                    startSaving = false;
                                    gettype = true;
                                }
                                if (startSaving && b != ']'){
                                    receivedStream.write(b);
                                }
                            }
                        }
                        catch(Exception e) {
                            System.out.println("nada a ler");
                        }

                    }
                    else{

                        try {
                            if(first){
                                start = System.currentTimeMillis();
                                System.out.println(start);
                                first= false;
                            }

                            byte buffer[];
                            if (last) {
                                buffer = serialPort.readBytes(+fileLength-fileLengthCounter);//ler
                            }else{
                                buffer = serialPort.readBytes(29);//ler
                            }
                            //System.out.println(buffer);

                            StringBuilder sb = new StringBuilder();
                            for (byte b : buffer) {
                                sb.append(String.format("%02X ", b));
                            }
                            System.out.println(sb.toString());

                            receivedStream.write (buffer); // guardar array de bytes
                            if (!count) {
                                for (byte b: buffer) {
                                    myTotalLength++;
                                    fileLengthName.add (b);
                                    if (b == '[') {     //antes de '[' está o tamanho da imagem
                                        count = false;
                                        byteFileLength = Arrays.copyOfRange (receivedStream.toByteArray (), 0, fileLengthName.size () - 1);  //copia para um array novo os bytes que contém o tamanho
                                        fileLength = Integer.parseInt (new String (byteFileLength));  //converte para inteiro a String que tem o tamanho
                                    }
                                    else if (b == ']') {    //entre o '[' e ']' está o nome da imagem
                                        count = true;   // boolean para começar a contagem dos bytes da imagem
                                        byte [] byteFileName = Arrays.copyOfRange (receivedStream.toByteArray (), byteFileLength.length + 1, fileLengthName.size () - 1); //copia para um array novo os bytes que contém o nome
                                        fileName = new String (byteFileName);           // converte para String o nome da imagem
                                        fileContentLength = receivedStream.size () - fileLengthName.size ();     // guardar o numero de bytes que foram guardados no inicio após se encontrar o caracter ']'
                                        fileLengthCounter = fileContentLength;
                                        System.out.println("fileContentLength: "+ fileContentLength);
                                        System.out.println("fileLengthCOUNTER: "+ fileLengthCounter);
                                        System.out.println("Mytotallength: "+ (myTotalLength+fileLength));
                                    }
                                }
                                if(fileLengthCounter + 29 > fileLength){
                                    System.out.println(fileLength-fileLengthCounter);
                                    last = true;
                                }
                            }
                            else {
                                fileLengthCounter += buffer.length;      //contador para o tamanho da imagem
                                System.out.println("fileLengthCounter+29: "+ (fileLengthCounter + 29));
                                System.out.println("fileLength: "+ (fileLength));
                                System.out.println("total: "+ (myTotalLength+fileLength-fileContentLength));
                                if(fileLengthCounter + 29 >= fileLength){
                                    System.out.println(fileLength-fileLengthCounter);
                                    last = true;
                                }


                                System.out.println("User 2: Downloading " + fileName + " (" + fileLengthCounter + "/" + fileLength + " B).");


                                Double d = new Double(fileLengthCounter);
                                Double d1 = new Double(fileLength);
                                System.out.println("progress: "+d/d1);

                                progBarText.setText("Downloading "+ fileName +" ("+fileLengthCounter+"/"+fileLength+" B)");
                                progBarText.setTextAlignment(TextAlignment.CENTER);
                                progressBar.setProgress(d/d1);
                                //progressBar.set

                                if (fileLengthCounter == fileLength) {
                                    byte c [] = receivedStream.toByteArray ();  //transforma a trama de todos os bytes para array
                                    byteFile = Arrays.copyOfRange (c, fileLengthName.size () - fileContentLength, c.length);//copia para um array novo todos os bytes que pertencem á imagem
                                    Files.write (Paths.get ("E:\\LTI\\" + fileName), byteFile);
                                    long end = System.currentTimeMillis();
                                    System.out.println ("\n" + fileName + " received!");
                                    progBarText.setText("");
                                    chatList.add("User 2: " + fileName + " ("+fileLength+" B) received! ("+TimeUnit.MILLISECONDS.toSeconds(end-start)+ " s)");
                                    //Controller.chatList.remove(0,Controller.chatList.size());
                                    gettype=true;
                                    menu = 1;
                                    count = false; //reset às variáveis usadas
                                    fileLengthName.clear ();
                                    receivedStream.reset ();
                                    fileContentLength = 0;
                                    fileLengthCounter = 0;

                                    System.out.println(end-start);
                                    myTotalLength = 0;
                                    last = false;
                                    first = true;
                                    startDownloading = true;
                                    progressBar.setProgress(0.0);
                                }
                            }
                        }
                        catch (SerialPortException ex) {System.out.println("Error in receiving string from COM-port: " + ex);}
                        catch (IOException ex) {
                            Logger.getLogger(Controller.class.getName()).log (Level.SEVERE, null, ex);}
                    }
                }
            }
        }
    }
}




