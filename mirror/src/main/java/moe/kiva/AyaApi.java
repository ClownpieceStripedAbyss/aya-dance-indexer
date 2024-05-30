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
    try {
      return tryParse();
    } catch (Throwable e) {
      System.err.println("WARN: failed to fetch Aya Song Index, skipping checksum: " + e.getMessage());
      return ImmutableSeq.empty();
    }
  }

  public static @NotNull ImmutableSeq<Song> tryParse() {
    var html = ApiHelper.getHtml(API_URL);
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
