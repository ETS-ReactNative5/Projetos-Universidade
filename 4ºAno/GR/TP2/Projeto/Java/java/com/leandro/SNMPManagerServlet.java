/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.leandro;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletContext;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author leandro
 */
@WebServlet(
        name = "snmpmanagerservlet",
        urlPatterns = "/Dashboard"
)
public class SNMPManagerServlet extends HttpServlet {
    SNMPManagerService service = null;
    Singleton s = Singleton.getInstance();
    String myAddress = "";
    @Override
    @SuppressWarnings("empty-statement")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String question = req.getParameter("q");
        System.out.println("AllClients: "+s.allClients);
        if(question == null){
            
            myAddress = req.getParameter("address");
            String port = ("".equals(req.getParameter("port")   ) ? "161": req.getParameter("port"));
            String time = req.getParameter("time");
            System.out.println("udp:"+myAddress+"/"+("".equals(port) ? "161": port));
            new Thread( new MultipleClients(myAddress, port, time)).start();
            service = s.allClients.get(myAddress);
            service.start();
            RequestDispatcher view = req.getRequestDispatcher("dashboard.html");
            view.forward(req, resp);
        }
        if("getProcs".equals(question)){
            int numberProcesses = service.getNumberProcesses();
            service.getRAMSize();
            HashMap<Integer, List> serviceProcesses = service.getProcessesTable(numberProcesses);
            HashMap<String, String> cpuUsage = service.getCpuUsage();
            HashMap<String, String> ramUsage = service.getRamUsage();
            HashMap<Integer, List<String[]>> history = service.getHistory();
            //System.out.println("Procs: "+serviceProcesses);
            //System.out.println("History: "+history);
            JsonObject newJson = new JsonObject();
            newJson.addProperty("length", serviceProcesses.size());
            newJson.add("mostCPUUsage", new Gson().toJsonTree(cpuUsage));
            newJson.add("mostRAMUsage", new Gson().toJsonTree(ramUsage));
            newJson.add("processes", new Gson().toJsonTree(serviceProcesses));
            newJson.add("history", new Gson().toJsonTree(history));
            PrintWriter out = resp.getWriter();
            out.print(newJson);
            out.flush();
            out.close();
        }
        if("getInfo".equals(question)){
            String add = service.address;
            String port = service.port;
            String nextUpdateStr = service.nextUpdate;
            int numberProcesses = service.getNumberProcesses();
            String time = service.getSysUpTime().toString().replace(".", "-").split("-")[0];
            JsonObject json = new JsonObject();
            json.addProperty("address", add);
            json.addProperty("port", port);
            json.addProperty("totalProcs", numberProcesses);
            try {
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = dateFormat.parse(time);
                Date nextUpdate = dateFormat.parse("00:"+nextUpdateStr);
                long seconds = date.getTime() / 1000L;
                long nextUp = nextUpdate.getTime() / 1000L;
                json.addProperty("time", seconds);
                json.addProperty("nextUpdate", nextUp);
            } catch (ParseException ex) {
                Logger.getLogger(SNMPManagerServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            PrintWriter out = resp.getWriter();
            out.print(json);
            out.flush();
            out.close();
        }
        
        if("getInfoClients".equals(question)){
            JsonArray arr = new JsonArray();
            JsonObject obj = new JsonObject();
            for(String a: s.allClients.keySet()){
                
                if(!myAddress.equals(a)){
                    //System.out.println("addresses: "+s.allClients.get(a).address+ " port: "+s.allClients.get(a).port);
                    //System.out.println("processes: "+s.allClients.get(a).getNumberProcesses()+ " port: "+s.allClients.get(a).getSysUpTime().toString());
                    obj.addProperty("address", s.allClients.get(a).address);
                    obj.addProperty("port", s.allClients.get(a).port);
                    obj.addProperty("numberProcs", s.allClients.get(a).getNumberProcesses());
                    obj.addProperty("sysUpTime", s.allClients.get(a).getSysUpTime().toString().replace(".", "-").split("-")[0]);
                    arr.add(obj);
                }
                obj = new JsonObject();
            }
            PrintWriter out = resp.getWriter();
            out.print(arr);
            out.flush();
            out.close();
        }

    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletContext servletContext = getServletContext();
    }
}
