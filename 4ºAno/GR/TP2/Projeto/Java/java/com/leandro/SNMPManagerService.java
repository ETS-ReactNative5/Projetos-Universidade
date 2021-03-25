/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.leandro;

/**
 *
 * @author leandro
 */

import java.io.IOException;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.snmp4j.smi.Variable;

public class SNMPManagerService {
    Snmp snmp = null;
    String address = null;
    String port = null;
    String nextUpdate = null;
    ArrayList<OID> oids = new ArrayList<OID>(){{
        add(new OID(".1.3.6.1.2.1.25.1.6.0"));
        add(new OID(".1.3.6.1.2.1.25.1.1.0"));
        add(new OID(".1.3.6.1.2.1.25.4.2.1.2"));
        add(new OID(".1.3.6.1.2.1.25.5.1.1.1"));
        add(new OID(".1.3.6.1.2.1.25.5.1.1.2"));
        add(new OID(".1.3.6.1.2.1.25.4.2.1.7"));
        add(new OID(".1.3.6.1.2.1.25.4.2.1.1"));
        add(new OID(".1.3.6.1.2.1.25.2.2.0"));
        
    }};
    int ramSize = 0;
    String sysUpTime = "";
    HashMap<Integer,List> processes = new HashMap<>();
    HashMap<Integer,List> historyProcs = new HashMap<>();
    final String[] status = new String[]{"","running","runnable","notRunnable","invalid"};

    public SNMPManagerService(final String add,final String p, final String n) {
        address = add;
        port = p;
        nextUpdate = n;
    }

    public void start() throws IOException {

        final TransportMapping transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);

        // Do not forget this line!

        transport.listen();
        
    }
    public void getRAMSize() throws IOException {
        final PDU pdu = new PDU();
        final ResponseEvent event;
        pdu.add(new VariableBinding(getOids().get(7)));
        pdu.setType(PDU.GET);
        event = snmp.send(pdu, getTarget(), null);
        double number = Double.parseDouble(event.getResponse().get(0).getVariable().toString());
        this.ramSize = (int) (int) number/1024;
    }
    public int getNumberProcesses() throws IOException {
        final PDU pdu = new PDU();
        final ResponseEvent event;
        pdu.add(new VariableBinding(getOids().get(0)));
        pdu.setType(PDU.GET);
        event = snmp.send(pdu, getTarget(), null);
        int number = Integer.parseInt(event.getResponse().get(0).getVariable().toString());
        return number;

    }
    public HashMap<String, String> getCpuUsage() {
        HashMap<String,String> mostCPUUsage = new HashMap<>();
        int newIndex = 0;
        int lastCpuUsage = 0;
        for (Integer key : this.processes.keySet().stream()
          .sorted((o1,o2) -> 
                  Integer.compare(Integer.parseInt(this.processes.get(o2).get(1).toString()), Integer.parseInt(this.processes.get(o1).get(1).toString()))
        )
        .collect(Collectors.toList())){
            if(newIndex < 8){
               int CPUUsage = Integer.parseInt(this.processes.get(key).get(1).toString());  
               if(mostCPUUsage.get(this.processes.get(key).get(0).toString()) != null){
                   
                    int last = Integer.parseInt(mostCPUUsage.get(this.processes.get(key).get(0).toString()));
                    int total = last + CPUUsage;
                    mostCPUUsage.put(this.processes.get(key).get(0).toString(), Double.toString(total));
                    
                }else{
                     
                     mostCPUUsage.put(this.processes.get(key).get(0).toString(), Integer.toString(CPUUsage));
                }
               
                newIndex++;
            }else{
                lastCpuUsage +=Integer.parseInt(this.processes.get(key).get(1).toString());
                mostCPUUsage.put("Outros", Integer.toString(lastCpuUsage));
            }  
        }
        //System.out.println("CPU "+ mostCPUUsage);
        
        return mostCPUUsage;
    }
    public HashMap<String, String> getRamUsage() {
        HashMap<String,String> mostRAMUsage = new HashMap<>();
        int newIndex = 0;
        double lastRAMUsage = 0;
        for (Integer key : this.processes.keySet().stream()
          .sorted((o1,o2) -> 
                  Double.compare(Double.parseDouble(this.processes.get(o2).get(2).toString()), Double.parseDouble(this.processes.get(o1).get(2).toString()))
        )
        .collect(Collectors.toList())){
            if(newIndex < 10){
                double RAMUsage = Double.parseDouble(this.processes.get(key).get(2).toString())/1024;
                if(mostRAMUsage.get(this.processes.get(key).get(0).toString()) != null){
                    double last = Double.parseDouble(mostRAMUsage.get(this.processes.get(key).get(0).toString()));
                    double total = last + RAMUsage;
                    mostRAMUsage.put(this.processes.get(key).get(0).toString(), Double.toString(total));
                    
                }else{
                    mostRAMUsage.put(this.processes.get(key).get(0).toString(), Double.toString(RAMUsage));
                }
                    
                newIndex++;
            }else{
                lastRAMUsage +=Double.parseDouble(this.processes.get(key).get(2).toString())/1024;
                mostRAMUsage.put("Outros", Double.toString(lastRAMUsage));
            }  
        }
        //System.out.println("RAM "+ mostRAMUsage);
        return mostRAMUsage;
    }
    public HashMap getHistory() {
        return this.historyProcs;
    }
    public Variable getSysUpTime() throws IOException {
        final PDU pdu = new PDU();
        final ResponseEvent event;
        pdu.add(new VariableBinding(getOids().get(1)));
        pdu.setType(PDU.GET);
        event = snmp.send(pdu, getTarget(), null);
        return event.getResponse().get(0).getVariable();

    }
    public HashMap getProcessesTable(int max) throws IOException {
        List<TreeEvent> l = null;
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        OID[] rootOIDs = new OID[5];
        for (int i = 2; i<oids.size()-1; i++) {
            rootOIDs[i-2] = oids.get(i);
        }
        l = treeUtils.walk(getTarget(), rootOIDs);
        int index = 0;
        
        long time = this.getSysUpTime().toLong();
        
        for(TreeEvent t : l){
            List<String> temp = new ArrayList<>();
            List<String[]> tempHistory = new ArrayList<>();
            VariableBinding[] vbs= t.getVariableBindings();
            if ((vbs != null) && (vbs.length != 0)){
                String[] tempArr = new String[3];
                int pidProc = Integer.parseInt(vbs[4].getVariable().toString());

                for(int i = 0; i<vbs.length;i++){
                    if( i == 3 ){
                        temp.add(this.status[Integer.parseInt(vbs[i].getVariable().toString())]);
                    }else if( i == 1 ){
                        temp.add(Long.toString((long)((float)vbs[i].getVariable().toLong()/time*100)));
                    }else{
                            temp.add(vbs[i].getVariable().toString());
                    }
                }
                if(historyProcs.get(pidProc) == null){
                    tempArr[0] = temp.get(1);
                    tempArr[1] = Double.toString(Double.parseDouble(temp.get(2).toString())/(1024*this.ramSize));
                    tempArr[2] = this.getSysUpTime().toString().replace(".", "-").split("-")[0];
                    tempHistory.add(tempArr);
                    historyProcs.put(pidProc, tempHistory);
                }else{
                    tempHistory = historyProcs.get(pidProc);
                    tempArr[0] = temp.get(1);
                    tempArr[1] = Double.toString(Double.parseDouble(temp.get(2).toString())/(1024*this.ramSize));
                    tempArr[2] = this.getSysUpTime().toString().replace(".", "-").split("-")[0];
                    historyProcs.get(pidProc).add(tempArr);
                    
                }
                processes.put(index, temp);
                index++;
            }
            
        }
        //System.out.println("History: "+this.historyProcs);
        //System.out.println("procs: "+processes);
        return processes;
    }

    public void setOids(String[] oid) {
        for (String newOid : oid) {
            oids.add(new OID(newOid));
        }
    }
    public String getAddress(){
        return this.address;
    }
    public String getPort(){
        return this.port;
    }
    public ArrayList<OID> getOids() {
        return oids;
    }

    /**
     * This method returns a Target, which contains information about where the data
     * should be fetched and how. * @return
     */
    private Target getTarget() {
        final Address targetAddress = GenericAddress.parse("udp:"+address+"/"+port);
        final CommunityTarget target = new CommunityTarget();

        target.setCommunity(new OctetString("public"));

        target.setAddress(targetAddress);

        target.setRetries(2);

        target.setTimeout(1500);

        target.setVersion(SnmpConstants.version2c);
        
        target.setMaxSizeRequestPDU(65536);

        return target;

    }
}
