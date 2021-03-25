package main.java.app;

import java.util.ArrayList;

import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.OctetString;

public class Genre {
    private int artists, albums, songs;
    private String name;
    private ArrayList<Integer> playlist = new ArrayList<>();

    public Genre(String name) {
        this.name = name;
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

    public int getArtists() {
        return this.artists;
    }

    public Counter32 getC32Artists() {
        return new Counter32(this.artists);
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

    public void incArtists() {
        this.artists ++;
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
        Genre g = (Genre) o;

        return this.name.equals(g.name);
    }
}