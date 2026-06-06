package data;

import java.util.Objects;

public class Song {
    private String artistId;
    private String artistName;
    private String trackId;
    private String trackName;

    public Song(String trackId, String trackName, String artistId, String artistName) {
        this.artistId = artistId;
        this.artistName = artistName;
        this.trackId = trackId;
        this.trackName = trackName;
    }

    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return Objects.equals(artistId, song.artistId) && Objects.equals(artistName, song.artistName) && Objects.equals(trackId, song.trackId) && Objects.equals(trackName, song.trackName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artistId, artistName, trackId, trackName);
    }
}
