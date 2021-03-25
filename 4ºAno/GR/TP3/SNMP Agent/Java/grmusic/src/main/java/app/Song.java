package main.java.app;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;

public class Song {
    private int genre, artist, album;
    private String name, format, path, relativePath;
    private long length = -1, size = -1;
    private boolean favorited = false;

    public Song(String path, String relativePath, String name, String format, long length, long size, int genre, int artist, int album, boolean favorited) {
        this.genre = genre;
        this.artist = artist;
        this.album = album;
        this.name = name;
        this.format = format;
        this.length = length;
        this.size = size;
        this.path = path;
        this.relativePath = relativePath;
        this.favorited = favorited;
    }

    public File getFile() {
        return new File(this.path);
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

    public int getAlbum() {
        return this.album;
    }

    public Integer32 getI32Album() {
        return new Integer32(this.album);
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

    public String getFormat() {
        return this.format;
    }

    public OctetString get8SFormat() {
        if (this.format != null) {
            return new OctetString(this.format);
        }

        else {
            return null;
        }
    }

    public long getLength() {
        return this.length;
    }

    public TimeTicks getTTLength() {
        if (this.length >= 0) {
            return new TimeTicks(this.length * 100);
        }

        else {
            return null;
        }
    }

    public long getSize() {
        return this.size;
    }

    public Integer32 getI32Size() {
        if (this.size >= 0) {
            return new Integer32((int) this.size / 1024);
        }

        else {
            return null;
        }
    }

    public String getPath() {
        return this.path;
    }

    public OctetString get8SPath() {
        if (this.path != null) {
            return new OctetString(this.path);
        }

        else {
            return null;
        }
    }

    public String getRelativePath() {
        return this.relativePath;
    }

    public boolean isFavorite() {
        return this.favorited;
    }

    public Integer32 getI32Favorited() {
        if (!this.favorited) {
            return new Integer32(1);
        }

        else {
            return new Integer32(2);
        }
    }

    public void setFavorite(boolean favorited) {
        this.favorited = favorited;
    }
}