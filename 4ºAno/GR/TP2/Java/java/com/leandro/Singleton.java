/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.leandro;

import java.util.HashMap;

/**
 *
 * @author leandro
 */
public class Singleton {
    private static Singleton instance;
    
    HashMap<String, SNMPManagerService> allClients = new HashMap<>();
    
    private Singleton(){}
    
    public static Singleton getInstance(){
        if(instance == null){
            instance = new Singleton();
        }
        return instance;
    }
    
    
    public void put(String a, SNMPManagerService s) {
        allClients.put(a,s);       
    }
    
}
