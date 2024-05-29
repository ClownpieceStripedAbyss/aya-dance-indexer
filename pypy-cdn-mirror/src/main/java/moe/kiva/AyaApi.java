package moe.kiva;

import com.google.gson.GsonBuilder;
import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class AyaApi {
  private static final String API_URL = "https://aya-dance.kiva.moe/aya-api/v1/songs";

  record ApiSongList(
    long updatedAt,
    List<SongList> categories
  ) {
  }

  public static @NotNull ImmutableSeq<Song> parse() {
    var html = PyPyApi.getHtml(API_URL);
    var apiSongs = new GsonBuilder().create()
      .fromJson(html, ApiSongList.class);
    return apiSongs.categories
      .stream()
      .flatMap(c -> c.entries().stream())
      .sorted(Comparator.comparingInt(Song::id))
      .collect(ImmutableSeq.factory())
      .distinctBy(ImmutableSeq.factory(), Song::id);
  }
}
