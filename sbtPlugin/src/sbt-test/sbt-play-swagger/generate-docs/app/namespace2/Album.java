package namespace2;

import namespace1.Artist;
import java.util.List;


public class Album{
    public List<Track> trackList;
    public Artist artist;

    public Album(List<Track> trackList, Artist artist) {
//        this(trackList);
        this.artist = artist;
    }

    public Album(List<Track> trackList) {
        this();
        this.trackList = trackList;
    }

    public Album() {
        System.out.println(toString());
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Album{" +
                "trackList=" + trackList +
                ", artist=" + artist +
                '}';
    }

    public List<Track> getTrackList() {
        return trackList;
    }

    public void setTrackList(List<Track> trackList) {
        this.trackList = trackList;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }
}