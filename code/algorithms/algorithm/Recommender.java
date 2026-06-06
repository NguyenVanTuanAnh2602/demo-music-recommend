package algorithm;

import data.*;

import java.util.List;

public interface Recommender {
    List<Song> recommend(User user, int k);
}
