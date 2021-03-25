package main.java.app;

import java.util.ArrayList;

import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;

public class Artist {
    private int genre, albums, songs;
    private String name;
    private ArrayList<Integer> playlist = new ArrayList<>();

    public Artist(String name, int genre) {
        this.genre = genre;
        this.name = name;
    }

    public int getGenre() {
        return this.genre;
    }

    public Integer32 getI32Genre() {
        return new Integer32(this.genre);
    }

    public String getName() {
        return this.name;
    }

    public OctetString get8SName() {
        if (this.name != null) {
            return new OctetString(this.name);
        }

        else {
            return null;
        }
    }

    public int getAlbums() {
        return this.songs;
    }

    public Counter32 getC32Albums() {
        return new Counter32(this.albums);
    }

    public int getSongs() {
        return this.songs;
    }

    public Counter32 getC32Songs() {
        return new Counter32(this.songs);
    }

    public ArrayList<Integer> getPlaylist() {
        return this.playlist;
    }

    public void incAlbums() {
        this.albums ++;
    }

    public void incSongs() {
        this.songs ++;
    }

    public void addSong(int songIndex) {
        this.playlist.add(songIndex);
    }

    @Override
    public boolean equals(Object o) {
        Artist a = (Artist) o;

        return (
            this.name.equals(a.name) &&
            this.genre == a.genre
        );
    }
}