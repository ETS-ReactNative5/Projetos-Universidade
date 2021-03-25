package main.java.app;

import java.util.ArrayList;

import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;

public class Album {
    private int genre, artist, songs;
    private String name;
    private ArrayList<Integer> playlist = new ArrayList<>();

    public Album(String name, int genre, int artist) {
        this.genre = genre;
        this.artist = artist;
        this.name = name;
    }

    public int getGenre() {
        return this.genre;
    }

    public Integer32 getI32Genre() {
        return new Integer32(this.genre);
    }

    public int getArtist() {
        return this.artist;
    }

    public Integer32 getI32Artist() {
        return new Integer32(this.artist);
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

    public int getSongs() {
        return this.songs;
    }

    public Counter32 getC32Songs() {
        return new Counter32(this.songs);
    }

    public ArrayList<Integer> getPlaylist() {
        return this.playlist;
    }

    public void incSongs() {
        this.songs ++;
    }

    public void addSong(int songIndex) {
        this.playlist.add(songIndex);
    }

    @Override
    public boolean equals(Object o) {
        Album a = (Album) o;

        return (
            this.name.equals(a.name) &&
            this.genre == a.genre &&
            this.artist == a.artist
        );
    }
}