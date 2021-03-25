/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import musicmanager.MusicManager;

/**
 *
 * @author leandro
 */
@WebServlet(name = "MusicServlet", urlPatterns = {""})
public class MusicServlet extends HttpServlet {
    MusicManager manager;
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void init(){
        System.out.println("INIT");
        try {
            
            manager = new MusicManager("localhost", "1337");
            manager.start();
            
            manager.getArtists();
            manager.getGenres();
            manager.getAlbums();
            manager.getSongsTable();
        } catch (IOException ex) {
            Logger.getLogger(MusicServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("here");
        String question = request.getParameter("q");
        System.out.println("question"+ question);
        
        if(question == null){
            RequestDispatcher view = request.getRequestDispatcher("index.html");
            view.forward(request, response);
        }
        if(question.equals("scanTime")){
            PrintWriter out = response.getWriter();
            out.print(manager.getScanTime());
            out.flush();
            out.close();
        }
        if(question.equals("refresh")){
            String scan = request.getParameter("scan");
            
            PrintWriter out = response.getWriter();
            out.print(manager.refresh(scan));
            out.flush();
            out.close();
        }
        if(question.equals("getCurrentStatus")){
            System.out.println("getCurrent");
            PrintWriter out = response.getWriter();
            out.print(manager.setupPlayer());
            out.flush();
            out.close();
        }
        if(question.equals("getQueue")){
            PrintWriter out = response.getWriter();
            out.print(manager.getJSONQueueSongs());
            out.flush();
            out.close();
        }
        if(question.equals("songs")){
            System.out.println("songs");
            PrintWriter out = response.getWriter();
            out.print(manager.getJSONSongs());
            out.flush();
            out.close();
        }
        if(question.equals("albums")){
            System.out.println("albums");
            PrintWriter out = response.getWriter();
            out.print(manager.getJSONAlbums());
            out.flush();
            out.close();
        }
        if(question.equals("artists")){
            System.out.println("artist");
            PrintWriter out = response.getWriter();
            out.print(manager.getJSONArtists());
            out.flush();
            out.close();
        }
        if(question.equals("genres")){
            System.out.println("genres");
            PrintWriter out = response.getWriter();
            out.print(manager.getJSONGenres());
            out.flush();
            out.close();
        }
        if(question.equals("albumSongs")){
            System.out.println("albumSongs");
            String album = request.getParameter("album");
            System.out.println(album);
            PrintWriter out = response.getWriter();
            out.print(manager.getSongsByAlbum(album)    );
            out.flush();
            out.close();
        }
        if(question.equals("artistSongs")){
            System.out.println("artistSongs");
            String artist = request.getParameter("artist");
            System.out.println(artist);
            PrintWriter out = response.getWriter();
            out.print(manager.getSongsByArtist(artist)    );
            out.flush();
            out.close();
        }
        if(question.equals("genreSongs")){
            System.out.println("genreSongs");
            String genre = request.getParameter("genre");
            System.out.println(genre);
            PrintWriter out = response.getWriter();
            out.print(manager.getSongsByGenre(genre)    );
            out.flush();
            out.close();
        }
        if(question.equals("statusSong")) {
            String index = request.getParameter("index");
            String type = request.getParameter("type");
            System.out.println("index: "+ index);
            System.out.println("type: "+ type);
            if(type.equals("shuffle") || type.equals("repeat")) {
                manager.changeOrder(index, type);
            }else{
                manager.playSong(index, type);
            }
           
        }
        if(question.equals("startPlaylist")) {
            String index = request.getParameter("index");
            String type = request.getParameter("type");
            System.out.println("index: "+ index);
            System.out.println("type: "+ type);
            manager.startPlaylist(index, type);
           
        }
        if(question.equals("skipSong")){
            String type = request.getParameter("type");
            PrintWriter out = response.getWriter();
            if(type.equals("skip") || type.equals("previous") || type.equals("sameSong")){
                System.out.println("Type:  "+type );
                out.print(manager.skipSong(type));
            }else{
                out.print(manager.getCurrentSong());
            }
            out.flush();
            out.close();
           
        }
        if(question.equals("addFavorite")){
            System.out.println("addFavorite");
            String index = request.getParameter("index");
            String favorite = request.getParameter("favorite");
            manager.setFavorite(index, favorite);
            PrintWriter out = response.getWriter();
            out.print(manager.getJSONFavoriteSongs());
            out.flush();
            out.close();
        }
        if(question.equals("addToQueue")){
            String index = request.getParameter("index");
            String remove = request.getParameter("remove");
            manager.addToQueue(index, remove);
            PrintWriter out = response.getWriter();
            out.print(manager.getQueueSongs());
            out.flush();
            out.close(); 
        }
        if(question.equals("clearQueue")){
            manager.clearQueue();
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("POST: "+request.getParameterValues("q"));
        String question = request.getParameter("q");
        System.out.println("question"+ question);
        
        if(question.equals("playSong")) {
            String song = request.getParameter("name");
            System.out.println("play song: "+ song);
        }
    }
    
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
         
        
    }
    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
