import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.factory.PacketFactories;
import org.pcap4j.packet.namednumber.ArpHardwareType;
import org.pcap4j.packet.namednumber.ArpOperation;
import org.pcap4j.packet.namednumber.DataLinkType;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.util.ByteArrays;
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.Packets;

import javax.crypto.Mac;
import java.io.EOFException;
import java.lang.reflect.Array;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

class InterfaceListener implements Runnable {
    App main = new App();
    static HashMap<Integer, ArrayList<String>> forwardingTable = new HashMap<>();
    static HashMap<String, String> allMACaddresses = new HashMap<>();
    PcapHandle[] handle;
    PcapHandle myHandle;
    boolean flag = true;
    int index = 0;

    @Override
    public void run() {
        while (flag) {
            try {
                Packet packet = myHandle.getNextPacket();               // Espera por pacote Ethernet no socket

                if (packet != null && packet.contains(IpV4Packet.class)) { // Filtra pacotes com IPv4
                    String dstIPAddress = packet.get(IpV4Packet.class).getHeader().getDstAddr().getHostAddress(); // Endereço IP destino
                    String dstMACaddress = allMACaddresses.get(dstIPAddress); // Endereço MAC do IP destino
                    MacAddress dstMac = null;
                    if (dstMACaddress != null) {
                        dstMac = MacAddress.getByName(dstMACaddress);
                    }
                    int in = searchTable(dstIPAddress);                 // Descobrir a interface de saída na tabela de encaminhamento
                    IpV4Packet.Builder ipPacket = packet.get(IpV4Packet.class).getBuilder(); // Pacote recebido é desmontado
                    EthernetPacket.Builder ether = new EthernetPacket.Builder(); // Pacote Ethernet para enviar

                    if (in != 4) {                                      // Se a interface de saída não for a redirecionada para à Internet:
                        ether.dstAddr(dstMac);                          // Endereço MAC destino (MAC do IP destino)
                        ether.srcAddr(packet.get(EthernetPacket.class).getHeader().getDstAddr()); // Endereço MAC origem (MAC destino do pacote anterior = MAC da interface atual)

                    } else {                                            // Se estiver redirecionada para a Internet:
                        MacAddress myMAC = MacAddress.getByName("00:00:00:aa:00:0c"); // Endereço MAC da interface atual
                        MacAddress myDstMAC = MacAddress.getByName("00:00:00:aa:00:0d"); // Endereço MAC do router ligado à Internet
                        ether.dstAddr(myDstMAC);                        // Endereço MAC destino (MAC do router)
                        ether.srcAddr(myMAC);                           // Endereço MAC origem (MAC destino do pacote anterior = MAC da interface atual)
                    }
                    ether.type(EtherType.IPV4);                         // Tipo IPv4
                    ether.payloadBuilder(ipPacket);                     // Pacote IPv4 será o recebido
                    ether.paddingAtBuild(true);
                    Packet sendPacket = ether.build();                  // Pacote Ethernet é construído com os parâmetros referidos
                    handle[in].sendPacket(sendPacket);                  // Pacote Ethernet é enviado pelo socket
                }
            }  catch (NotOpenException e) {
                e.printStackTrace();
            } catch (PcapNativeException e) {
                e.printStackTrace();
            }
        }
        myHandle.close();
    }
    public InterfaceListener(PcapHandle[] h, int i, HashMap<String, String> allMACs) { // Construtor
        handle = h;                                                     // Array de sockets de todas as interfaces
        myHandle = h[i];                                                // Socket da própria interface
        index = i;                                                      // Identificador da própria interface
        allMACaddresses = allMACs;                                      // Endereços MAC de todos os computadores
    }

    public static boolean sameNetwork(String ip1, String ip2, String mask) throws Exception { // Verificar se 'ip2' (IP destino) pertence à rede 'ip1'
        byte[] a1 = InetAddress.getByName(ip1).getAddress();
        byte[] a2 = InetAddress.getByName(ip2).getAddress();
        byte[] m = InetAddress.getByName(mask).getAddress();

        for (int i = 0; i < a1.length; i ++)
            if ((a1[i] & m[i]) != (a2[i] & m[i]))                       // Se dentro da máscara os bits de ambos os endereços IP forem diferentes forem diferentes, 'ip2' não pertence a 'ip1'
                return false;

        return true;                                                    // Se não forem detetadas diferenças, assume-se que 'ip2' pertence a 'ip1'
    }

    public int searchTable(String dstAddress) {                         // Procurar interface destino para o IP destino
        boolean same = false;
        forwardingTable = main.getForwardingTable();                    // Obtem a tabela de encaminhamento
        for (Integer in : forwardingTable.keySet()) {
            ArrayList<String> temp = forwardingTable.get(in);           // Interface de saída identificada por 'in'
            try {
                same = sameNetwork(temp.get(0),dstAddress,temp.get(2)); // Verifica se o IP destino está na rede da interface 'in'
                if (same) {                                             // Se estiver na mesma interface, é retornado o identificador ('in') da mesma
                    return in;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return 4;                                                       // Se não estiver em nenhuma interface da tabela, então deve ter como destino a Internet, através da interface 4
    }
}
