/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package musicmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.util.Vector;
import java.util.stream.Collectors;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Variable;
/**
 *
 * @author leandro
 */
public class MusicManager {
    
    Snmp snmp = null;
    String address = null;
    String port = null;
    ArrayList<String> allArtists = new ArrayList<>();
    ArrayList<String> allGenres = new ArrayList<>(); 
    ArrayList<String[]> allAlbums = new ArrayList<>();
    ArrayList<String[]> allSongs = new ArrayList<>();
    ArrayList<Integer> favorites = new ArrayList<>();
    ArrayList<OID> SongsOids = new ArrayList<OID>(){{
        add(new OID(".1.3.6.1.3.42.7.1.1.1"));  //Index
        add(new OID(".1.3.6.1.3.42.7.1.1.2"));  //Genre
        add(new OID(".1.3.6.1.3.42.7.1.1.3"));  //Artist
        add(new OID(".1.3.6.1.3.42.7.1.1.4"));  //Album
        add(new OID(".1.3.6.1.3.42.7.1.1.5"));  //Name
        add(new OID(".1.3.6.1.3.42.7.1.1.7"));  //Length
        add(new OID(".1.3.6.1.3.42.7.1.1.10")); //Favorite
        
    }};
    ArrayList<OID> QueueOids = new ArrayList<OID>(){{
        add(new OID(".1.3.6.1.3.42.9.1.1.5"));  //Index song
        add(new OID(".1.3.6.1.3.42.9.1.1.3"));  //Artist
        add(new OID(".1.3.6.1.3.42.9.1.1.4"));  //Album
        add(new OID(".1.3.6.1.3.42.9.1.1.6"));  //Name
        add(new OID(".1.3.6.1.3.42.9.1.1.7"));  //Length
        add(new OID(".1.3.6.1.3.42.9.1.1.1"));  //Position
    }};
    ArrayList<OID> ArtistOids = new ArrayList<OID>(){{
        add(new OID(".1.3.6.1.3.42.5.1.1.1"));  //Index
        add(new OID(".1.3.6.1.3.42.5.1.1.3"));  //Artist
        
    }};
    ArrayList<OID> GenresOids = new ArrayList<OID>(){{
        add(new OID(".1.3.6.1.3.42.4.1.1.1"));  //Index
        add(new OID(".1.3.6.1.3.42.4.1.1.2"));  //Genre
        
    }};
    ArrayList<OID> AlbumsOids = new ArrayList<OID>(){{
        add(new OID(".1.3.6.1.3.42.6.1.1.1"));  //Index
        add(new OID(".1.3.6.1.3.42.6.1.1.3"));  //Artist
        add(new OID(".1.3.6.1.3.42.6.1.1.4"));  //Album
        
    }};
    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) throws IOException {
        MusicManager manager = new MusicManager("localhost", "1337");
        manager.start();
        
        manager.getArtists();
        manager.getGenres();
        manager.getAlbums();
        manager.getSongsTable();
        //manager.getJSONSongs();
    }
    public MusicManager(final String add,final String p) {
        address = add;
        port = p;
    }

    public void start() throws IOException {

        final TransportMapping transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);

        // Do not forget this line!

        transport.listen();
        
    }
    public JsonObject refresh(String scan) throws IOException{
        System.out.println("REFRESH: "+scan);
        JsonObject status = new JsonObject();
        int statusValue = 0;
        if(scan.equals("false")){
            statusValue = getStatusScan();
            if(statusValue == 1){
                setNewSongs();
                status.addProperty("status", statusValue);
                status.addProperty("time",getScanTime().get("time").getAsString());
                System.out.println("STATUS ");
                return status;
            }
        }else{
            set(2, ".1.3.6.1.3.42.1.1.0");
        }
        return status;
    }
    public int getStatusScan() throws IOException {
        int status = 0;
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(".1.3.6.1.3.42.1.1.0")));
        pdu.setType(PDU.GET);
        
        ResponseEvent event = snmp.send(pdu, getTarget());
        if (event != null) {
            status = event.getResponse().get(0).getVariable().toInt();
        }
        return  status;
    }
    public JsonObject getScanTime() throws IOException {
        JsonObject scanTime = new JsonObject();
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(".1.3.6.1.3.42.1.2.0")));
        pdu.setType(PDU.GET);
        
        ResponseEvent event = snmp.send(pdu, getTarget());
        if (event != null) {
            scanTime.addProperty("time", event.getResponse().get(0).getVariable().toString());
        }
        return  scanTime;
    }
    public void setNewSongs() throws IOException{
        allAlbums.clear();
        allArtists.clear();
        allGenres.clear();
        allSongs.clear();
        getArtists();
        getGenres();
        getAlbums();
        getSongsTable();
    }
    public JsonObject setupPlayer() throws IOException {
        System.out.println("SETUP");
        JsonObject initial = new JsonObject();
        JsonObject order = getStatusOrder();
        JsonObject currentSong = getCurrentSong();
        System.out.println("SETUP 2");
        initial.add("orderStatus", order);
        System.out.println("JSON "+currentSong.entrySet().isEmpty());
        if(!currentSong.entrySet().isEmpty()) initial.add("currentSong", currentSong);
        return initial;
    }
    public void playSong(String s, String type) throws IOException {
        int index = 0;
        String oid = "";
        if(type.equals("play")){
            oid = ".1.3.6.1.3.42.2.1.1.4.0";
            index = Integer.parseInt(s);
        }
        if(type.equals("pause")){
            oid = ".1.3.6.1.3.42.2.1.1.8.0";
            index = 2;
        }
        if(type.equals("resume")){
            oid = ".1.3.6.1.3.42.2.1.1.8.0";
            index = 3;
        }
        set(index, oid);
    }
    public void startPlaylist(String index, String type) throws IOException{
        String oid = "";
        if(type.equals("album")){
            oid = ".1.3.6.1.3.42.2.1.1.3.0";
        }
        if(type.equals("artist")){
            oid = ".1.3.6.1.3.42.2.1.1.2.0";
        }
        if(type.equals("genre")){
            oid = ".1.3.6.1.3.42.2.1.1.1.0";
        }
        if(type.equals("favorite")){
            oid = ".1.3.6.1.3.42.2.1.1.10.0";
        }
        set(Integer.parseInt(index), oid);
    }
    public void changeOrder(String s, String type) throws IOException {
        int index = 0;
        String oid = ".1.3.6.1.3.42.2.1.1.9.0";
        index = Integer.parseInt(s);
        set(index, oid);
        
    }
    public JsonObject skipSong(String type) throws IOException {
        String oid = ".1.3.6.1.3.42.2.1.1.8.0";
        System.out.println("TYPE: "+type);
        int index = 1;
        if(type.equals("skip")) index = 6;
        if(type.equals("previous")) index = 4;
        if(type.equals("sameSong")) index = 5;
        set(index, oid);
        return getCurrentSong();
    }
    public JsonObject getStatusOrder() throws IOException {
        JsonObject statusOrder = new JsonObject();
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(".1.3.6.1.3.42.2.1.1.9.0")));
        pdu.setType(PDU.GET);
        
        ResponseEvent event = snmp.send(pdu, getTarget());
        if (event != null) {
            statusOrder.addProperty("order",event.getResponse().get(0).getVariable().toInt() );
        }
        return  statusOrder;
    }
    public void setFavorite(String i, String fav) throws IOException {
        String oid = ".1.3.6.1.3.42.7.1.1.10."+i;
        set(Integer.parseInt(fav), oid);
        
    }
    public void addToQueue(String i, String r) throws IOException {
        String oid = "";
        int index = 0;
        
        if(r.equals("true")){
            oid = ".1.3.6.1.3.42.9.1.1.5."+i;
            
        }else{
            oid =".1.3.6.1.3.42.9.2.0";
            index = Integer.parseInt(i);
        }
        set(index, oid);
    }
    public void clearQueue() throws IOException{
        String oid = ".1.3.6.1.3.42.2.1.1.12.0";
        set(0, oid);
    }
    public JsonArray getQueueSongs(){
        JsonArray songs = getJSONQueueSongs();
        System.out.println("JSON: ");
        System.out.println(songs);
        return songs;
    }
    public void set(int index, String oid) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid), new Integer32(index)));
        pdu.setType(PDU.SET);
        
        ResponseEvent event = snmp.send(pdu, getTarget());
        if (event != null) {
            pdu = event.getResponse();
            if (pdu.getErrorStatus() == PDU.noError) {
              System.out.println("SNMPv3 SET Successful!");
            } else {
              System.out.println("SNMPv3 SET Unsuccessful.");
            }
        } else {
          System.out.println("SNMP send unsuccessful.");
        }
    }
    public JsonObject getCurrentSong() throws IOException{
        JsonObject currentSong = new JsonObject();
        int checkSong = 0;
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(".1.3.6.1.3.42.2.1.0")));
        pdu.setType(PDU.GETBULK);
        pdu.setMaxRepetitions(11);
        pdu.setNonRepeaters(0);
        ResponseEvent event = snmp.send(pdu, getTarget());
        System.out.println("GET CURRENT SONG");
        if (event != null) {
            
                System.out.println("GET CURRENT SONG 2: "+event.getResponse().getVariable(new OID(".1.3.6.1.3.42.2.1.1.8.0")).toString());
                checkSong = event.getResponse().getVariable(new OID(".1.3.6.1.3.42.2.1.1.8.0")).toInt();
                if(checkSong != 1){     //not stopped
                    Vector<? extends VariableBinding> vbs= event.getResponse().getVariableBindings();
                    currentSong.addProperty("name", vbs.get(4).getVariable().toString());
                    currentSong.addProperty("artist", allArtists.get(vbs.get(1).getVariable().toInt()-1));
                    currentSong.addProperty("length", vbs.get(6).getVariable().toString().split(":", 2)[1]);
                    currentSong.addProperty("time", vbs.get(5).getVariable().toString().split(":", 2)[1]);
                    currentSong.addProperty("status", vbs.get(7).getVariable().toString());
                    int index = vbs.get(9).getVariable().toInt();
                    if( index != 1 && index != 5){
                        String playlist = "";
                        if(index == 2){
                            playlist = allGenres.get(vbs.get(10).getVariable().toInt()-1);
                        }
                        if(index == 3){
                            playlist = allArtists.get(vbs.get(10).getVariable().toInt()-1);
                        }
                        if(index == 4){
                            playlist = allAlbums.get(vbs.get(10).getVariable().toInt()-1)[1];
                        }
                        currentSong.addProperty("playlist", playlist);
                    }else if (index == 5) {
                        currentSong.addProperty("playlist", "Favoritos");
                    }
                    else{
                        currentSong.addProperty("playlist", "--");
                    }
                }
        }
        System.out.println("GET CURRENT SONG 3");
        System.out.println("CurrentSong: "+currentSong);
        return currentSong;
    }
    public void getArtists() throws IOException {
        
        List<TreeEvent> l = null;
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        OID[] rootOIDs = new OID[2];
        for (int i = 0; i<ArtistOids.size(); i++) {
            rootOIDs[i] = ArtistOids.get(i);
        }
        l = treeUtils.walk(getTarget(), rootOIDs);
        
        for(TreeEvent t : l){
            VariableBinding[] vbs= t.getVariableBindings();
            if ((vbs != null) && (vbs.length != 0)){
                allArtists.add( vbs[0].getVariable().toInt()-1,vbs[1].getVariable().toString() );
            }    
	} 
        System.out.println("allArtists"+allArtists);
    }
    public void getGenres() throws IOException {
        List<TreeEvent> l = null;
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        OID[] rootOIDs = new OID[2];
        for (int i = 0; i<GenresOids.size(); i++) {
            rootOIDs[i] = GenresOids.get(i);
        }
        l = treeUtils.walk(getTarget(), rootOIDs);
        
        for(TreeEvent t : l){
            VariableBinding[] vbs= t.getVariableBindings();
            if ((vbs != null) && (vbs.length != 0)){
                allGenres.add( vbs[0].getVariable().toInt()-1,vbs[1].getVariable().toString() );
            }    
	} 
        System.out.println("allGenres"+allGenres);
    }
    public void getAlbums() throws IOException {
        List<TreeEvent> l = null;
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        OID[] rootOIDs = new OID[3];
        for (int i = 0; i<AlbumsOids.size(); i++) {
            rootOIDs[i] = AlbumsOids.get(i);
        }
        l = treeUtils.walk(getTarget(), rootOIDs);
        
        for(TreeEvent t : l){
            VariableBinding[] vbs= t.getVariableBindings();
            if ((vbs != null) && (vbs.length != 0)){
                allAlbums.add( vbs[0].getVariable().toInt()-1, new String[] {allArtists.get(vbs[1].getVariable().toInt()-1),vbs[2].getVariable().toString()} );
            }    
	}
        System.out.println("allAlbums"+allAlbums);
    }
    public JsonArray getSongsByAlbum(String album) {
        JsonArray albumSongs = new JsonArray();
        for(String[] songs: allSongs){
            if(album.equals(songs[2])){
                
                JsonObject tempSong = new JsonObject();
                tempSong.addProperty("index", allSongs.indexOf(songs)+1);
                tempSong.addProperty("name", songs[3]);
                tempSong.addProperty("length", songs[4]);
                tempSong.addProperty("favorite", Integer.parseInt(songs[5]));
                albumSongs.add(tempSong);
            }
        }
        System.out.println("album Musics: " + albumSongs);
        return albumSongs;
    }
    public JsonArray getSongsByArtist(String artist) {
        JsonArray artistSongs = new JsonArray();
        for(String[] album: allAlbums){
            if(artist.equals(album[0])){
                
                JsonObject artistAlbums = new JsonObject();
                artistAlbums.addProperty("album", album[1]);
                JsonArray albumSongs = getSongsByAlbum(album[1]);
                artistAlbums.add("albumSongs", albumSongs);
                artistSongs.add(artistAlbums);
            }
        }
        JsonObject artistAlbums = new JsonObject();
        JsonArray albumSongs = new JsonArray();
        for(String[] song: allSongs){
            if(song[2].equals("--") && song[1].equals(artist)){
                JsonObject tempSong = new JsonObject();
                tempSong.addProperty("index", allSongs.indexOf(song)+1);
                tempSong.addProperty("name", song[3]);
                tempSong.addProperty("length", song[4]);
                tempSong.addProperty("favorite", Integer.parseInt(song[5]));
                albumSongs.add(tempSong);
            }
        }
        if(albumSongs.size() != 0){
            artistAlbums.addProperty("album", 0);
            artistAlbums.add("albumSongs", albumSongs);
            artistSongs.add(artistAlbums);
        }
        System.out.println("album Musics: " + artistSongs);
        return artistSongs;
    }
    public JsonArray getSongsByGenre(String genre) {
        JsonArray genreSongs = new JsonArray();
        for(String[] songs: allSongs){
            if(genre.equals(songs[0])){
                
                JsonObject tempSong = new JsonObject();
                tempSong.addProperty("index", allSongs.indexOf(songs)+1);
                tempSong.addProperty("name", songs[3]);
                tempSong.addProperty("artist", songs[1]);
                tempSong.addProperty("album", songs[2]);
                tempSong.addProperty("length", songs[4]);
                tempSong.addProperty("favorite", Integer.parseInt(songs[5]));
                genreSongs.add(tempSong);
            }
        }
        System.out.println("songs Musics: " + genreSongs);
        return genreSongs;
    }
    public void getSongsTable() throws IOException {
        List<TreeEvent> l = null;
        VariableBinding vb;
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        OID[] rootOIDs = new OID[7];
        for (int i = 0; i<SongsOids.size(); i++) {
            rootOIDs[i] = SongsOids.get(i);
        }
        l = treeUtils.walk(getTarget(), rootOIDs);
        System.out.println("size="+l.size());
        for(TreeEvent t : l){
            VariableBinding[] vbs= t.getVariableBindings();
            if ((vbs != null) && (vbs.length != 0)){
                for(int i = 0; i<7; i++){
                    System.out.println(vbs[i].getVariable().toString());
                }
                allSongs.add( vbs[0].getVariable().toInt()-1, new String[] {allGenres.get(vbs[1].getVariable().toInt()-1),
                                                                            allArtists.get(vbs[2].getVariable().toInt()-1),
                                                                            vbs[3].getVariable().toInt() == 0 ? "--": allAlbums.get(vbs[3].getVariable().toInt()-1)[1],
                                                                            vbs[4].getVariable().toString(),
                                                                            vbs[5].getVariable().toString().split(":", 2)[1],
                                                                            vbs[6].getVariable().toString()
                                                                            });
                
                
            }    
	}
    }
    public JsonArray getJSONQueueSongs() {
        System.out.println("JSON QUEUE");
        JsonArray queueSongs = new JsonArray();
        List<TreeEvent> l = null;
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        OID[] rootOIDs = new OID[6];
        for (int i = 0; i<QueueOids.size(); i++) {
            rootOIDs[i] = QueueOids.get(i);
        }
        l = treeUtils.walk(getTarget(), rootOIDs);
        System.out.println("size="+l.size());
        for(TreeEvent t : l){
            VariableBinding[] vbs= t.getVariableBindings();
            if ((vbs != null) && (vbs.length != 0)){
                JsonObject song = new JsonObject();
                song.addProperty("index", vbs[0].getVariable().toInt());
                song.addProperty("artist", allArtists.get(vbs[1].getVariable().toInt()-1));
                song.addProperty("album", vbs[2].getVariable().toInt() == 0 ? "--": allAlbums.get(vbs[2].getVariable().toInt()-1)[1]);
                song.addProperty("name", vbs[3].getVariable().toString());
                song.addProperty("length", vbs[4].getVariable().toString().split(":", 2)[1]);
                song.addProperty("position", vbs[5].getVariable().toInt());
                song.addProperty("favorite", favorites.contains(vbs[0].getVariable().toInt()));
                System.out.println("JSON QUEUE");
                System.out.println(song);
                queueSongs.add(song);
            }    
	}
        return queueSongs;
    }
    public JsonArray getJSONSongs(){
        JsonArray songs = new JsonArray();
        allSongs.forEach(song ->{
            JsonObject tempSong = new JsonObject();
            tempSong.addProperty("index", allSongs.indexOf(song)+1);
            tempSong.addProperty("genre", song[0]);
            tempSong.addProperty("artist", song[1]);
            tempSong.addProperty("album", song[2]);
            tempSong.addProperty("name", song[3]);
            tempSong.addProperty("length", song[4]);
            tempSong.addProperty("favorite", Integer.parseInt(song[5]));
            songs.add(tempSong);
        });
        System.out.println("JSONSongs: "+songs);
        return songs;
    }
    public JsonArray getJSONFavoriteSongs() throws IOException{
        allSongs.clear();
        getSongsTable();
        
        JsonArray favSongs = new JsonArray();
        allSongs.forEach(song ->{
            if(Integer.parseInt(song[5]) == 2){
                JsonObject tempSong = new JsonObject();
                favorites.add(allSongs.indexOf(song)+1);
                tempSong.addProperty("index", allSongs.indexOf(song)+1);
                tempSong.addProperty("genre", song[0]);
                tempSong.addProperty("artist", song[1]);
                tempSong.addProperty("album", song[2]);
                tempSong.addProperty("name", song[3]);
                tempSong.addProperty("length", song[4]);
                tempSong.addProperty("favorite", Integer.parseInt(song[5]));
                favSongs.add(tempSong);
            }
            
        });
        System.out.println("JSONSongs: "+favSongs);
        return favSongs;
    }
    public JsonArray getJSONAlbums(){
        JsonArray albums = new JsonArray();
        allAlbums.forEach(album ->{
            JsonObject tempAlbum = new JsonObject();
            tempAlbum.addProperty("artist", album[0]);
            tempAlbum.addProperty("album", album[1]);
            tempAlbum.addProperty("index", allAlbums.indexOf(album)+1);
            albums.add(tempAlbum);
        });
        System.out.println("JSONSongs: "+albums);
        return albums;
    }
    public JsonArray getJSONArtists(){
        JsonArray artists = new JsonArray();
        allArtists.forEach(artist ->{
            JsonObject tempArtist = new JsonObject();
            tempArtist.addProperty("name", artist);
            tempArtist.addProperty("index", allArtists.indexOf(artist)+1);
            artists.add(tempArtist);
        });
        System.out.println("JSONSongs: "+artists);
        return artists;
    }
    public JsonArray getJSONGenres(){
        JsonArray genres = new JsonArray();
        allGenres.forEach(genre ->{
            JsonObject tempGenre = new JsonObject();
            tempGenre.addProperty("name", genre);
            tempGenre.addProperty("index", allGenres.indexOf(genre)+1);
            genres.add(tempGenre);
        });
        System.out.println("JSONSongs: "+genres);
        return genres;
    }
    public String getAddress(){
        return this.address;
    }
    public String getPort(){
        return this.port;
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
