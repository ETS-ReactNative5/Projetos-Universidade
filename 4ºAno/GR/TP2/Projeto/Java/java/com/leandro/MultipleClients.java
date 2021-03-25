package com.leandro;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author leandro
 */
public class MultipleClients implements Runnable{
    String address = null;
    String port = null;
    String time = null;
    SNMPManagerService service = null;
    Singleton s = Singleton.getInstance();
    HashMap<String, SNMPManagerService> allClients = new HashMap<>();

    MultipleClients() {
        
    }
    @Override
    public void run() {
        try {
            this.service = s.allClients.get(this.address);
            this.service.start();
        } catch (IOException ex) {
            Logger.getLogger(MultipleClients.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public MultipleClients (String a, String p, String t){
        this.address = a;
        this.port = p;
        this.time = t;
        this.s.allClients.put(this.address, new SNMPManagerService(this.address, this.port, this.time));
    }
    
    public HashMap getAllClients(){
        return this.allClients;
    }
    
}
