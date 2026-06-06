package data;

import java.util.Objects;

public class Interaction {
    private User user;
    private Song song;
    private double playCount;

    public Interaction(User user, Song song, double playCount) {
        this.user = user;
        this.song = song;
        this.playCount = playCount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = song;
    }

    public double getPlayCount() {
        return playCount;
    }

    public void setPlayCount(double playCount) {
        this.playCount = playCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Interaction that = (Interaction) o;
        return playCount == that.playCount && Objects.equals(user, that.user) && Objects.equals(song, that.song);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, song, playCount);
    }
}
