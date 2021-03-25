import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.net.util.SubnetUtils;
import org.pcap4j.core.*;
import org.pcap4j.packet.namednumber.DataLinkType;
import org.pcap4j.util.LinkLayerAddress;
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.NifSelector;

import java.lang.reflect.Array;
import java.net.*;
import java.util.*;
import java.util.stream.IntStream;

import org.pcap4j.core.PcapNetworkInterface;

public class App {
    static PcapHandle[] handle;
    static final int snapshotLength = 65536; // in bytes
    static final int readTimeout = 50; // in milliseconds
    static HashMap<Integer,ArrayList<String>> forwardingTable = new HashMap<>(); // Tabela de encaminhamento
    static HashMap<Integer,ArrayList<String>> table = new HashMap<>();
    static HashMap<String, String> allMACaddreses = new HashMap<String, String>() {{ // Endereços MAC de todos os computadores
        put("10.0.0.20", "00:00:00:aa:00:00");
        put("10.0.0.21", "00:00:00:aa:00:01");
        put("10.0.1.20", "00:00:00:aa:00:03");
        put("10.0.1.21", "00:00:00:aa:00:04");
        put("10.0.2.20", "00:00:00:aa:00:06");
        put("10.0.2.21", "00:00:00:aa:00:07");
        put("10.0.3.20", "00:00:00:aa:00:09");
        put("10.0.3.21", "00:00:00:aa:00:0a");
    }};
    static InetAddress networkIP;
    static String addressString,tempMask,tempNetworkAddress,ethString,maskString;
    static int maskVal, ethIndex;

    static HashMap<Integer, ArrayList<String>> manualTable() throws IOException { // Encaminhamento manual
        HashMap<Integer, ArrayList<String>> table = new HashMap<Integer, ArrayList<String>>(); // Tabela de encaminhamento
        BufferedReader reader = new BufferedReader(new FileReader("forwarding.txt")); // Ficheiro de configuração
        String line;                                                    // Linha do ficheiro: Rede dest. + Int. saída + Mask + Eth
        while ((line = reader.readLine()) != null) {
            table.put(Integer.parseInt(line.split(" ")[3].substring(3)), new ArrayList<String>(Arrays.asList(line.split(" "))));
        }
        for (Integer i: table.keySet()) {
            for (String s: table.get(i)) {
                System.out.print(s + " ");
            }
            System.out.println();
        }
        reader.close();
        return table;
    }

    // Tabela de encaminhamento dinâmico
    public static HashMap<Integer, ArrayList<String>> forwardingTable (InetAddress interfaceAddress, InetAddress interfaceMask, String eth) throws UnknownHostException {
        ethIndex = Integer.parseInt(eth.substring(3));                  // Identificador da interface
        addressString = interfaceAddress.getHostAddress();
        maskString = interfaceMask.getHostAddress();
        maskVal = convertNetmaskToPrefixLength(interfaceMask);

        String subnet = addressString + "/" + maskVal;
        SubnetUtils utils = new SubnetUtils(subnet);
        tempNetworkAddress = utils.getInfo().getNetworkAddress();       // Endereço de rede da interface

        // Rede destino + Interface de saída + Máscara + Eth
        if (table.get(ethIndex) == null){
            ArrayList<String> temp = new ArrayList<>();
            temp.add(tempNetworkAddress);
            temp.add(addressString);
            temp.add(maskString);
            temp.add(eth);
            table.put(ethIndex, temp);
        }
        return table;
    }

    public static int convertNetmaskToPrefixLength (InetAddress netmask) { // Conversão de endereço de máscara para valor numérico
        byte[] netmaskBytes = netmask.getAddress();
        int PrefixLength = 0;
        boolean zero = false;
        for (byte b : netmaskBytes) {
            int mask = 0x80;
            for (int i = 0; i < 8; i++){
                int result = b & mask;
                if (result == 0) {
                    zero = true;
                } else if (zero) {
                    throw new IllegalArgumentException("Invalid netmask.");
                } else {
                    PrefixLength ++;
                }
                mask >>>= 1;
            }
        }
        return PrefixLength;
    }

    public HashMap<Integer, ArrayList<String>> getForwardingTable() {   // Método para obter tabela de encaminhamento
        return forwardingTable;
    }

    public static void main(String[] args) throws Exception {
        // The code we had before
        ArrayList<PcapNetworkInterface> allDevs = new ArrayList<>();    // lista de interfaces
        InetAddress interfaceAddress, interfaceMask;
        String eth;

        List<PcapNetworkInterface> devices = Pcaps.findAllDevs();       // lista de dispositivos
        for (PcapNetworkInterface dev : devices) {
            if (dev.getName().contains("eth")) {                        // Adicionar interfaces de 'devices' a 'allDevs'
                allDevs.add(dev);                                       
                System.out.println(dev.getName());
                eth = dev.getName();                                    // Nome da interface
                interfaceAddress = dev.getAddresses().get(0).getAddress(); // Endereço IP da interface
                interfaceMask = dev.getAddresses().get(0).getNetmask(); // Máscara da interface

                // Tabela dinâmica
                forwardingTable = forwardingTable(interfaceAddress, interfaceMask, eth); // Encaminhamento dinâmico da interface
            }

        }

        // Tabela manual
        // forwardingTable = manualTable(); // Encaminhamento manual das interfaces

        System.out.println("test allDevs: " + allDevs.size());
        handle = new PcapHandle[allDevs.size()];                        // Array de sockets, um para cada interface
        int n = 0;
        for (PcapNetworkInterface e : allDevs) {
            System.out.println("e: " + e.getName());
            handle[n] = e.openLive(snapshotLength, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, readTimeout); // Abertura do socket e atribuição de uma interface ao mesmo
            new Thread(new InterfaceListener(handle, n, allMACaddreses)).start(); // Thread com o array de sockets, identificador da interface atual, e mapa com os MACs de todos os computadores
            n ++;
        }
    }

}
