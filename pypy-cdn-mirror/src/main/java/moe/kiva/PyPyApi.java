package moe.kiva;

import com.google.gson.GsonBuilder;
import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import kala.control.Try;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class PyPyApi {
  private static final String API_URL = "https://jd.pypy.moe/api/v2/songs";

  record ApiSongList(
    long updatedAt,
    List<ApiSong> songs,
    List<String> groups
  ) {}

  record ApiSong(
    int id,
    int group,
    float volume,
    String name,
    boolean flip,
    int start,
    int end,
    boolean skipRandom,
    List<String> originalUrl
  ) {}

  public static @NotNull ImmutableSeq<Song> parse() {
    var html = getHtml(API_URL);
    var apiSongs = new GsonBuilder().create()
      .fromJson(html, ApiSongList.class);
    return apiSongs.songs
      .stream()
      .map(s -> {
        var catName = Try.of(() -> apiSongs.groups.get(s.group)).getOrNull();
        var prettyCatName = Option.ofNullable(catName)
          .filterNot(String::isBlank)
          .getOrElse(() -> "Category %d".formatted(s.group));
        return new Song(
          s.id,
          s.group,
          s.name,
          prettyCatName,
          "https://aya-dance-cf.kiva.moe/api/v1/videos/%d.mp4".formatted(s.id),
          "",
          Song.spell(s.name),
          0,
          s.volume,
          s.start,
          s.end,
          s.flip,
          s.skipRandom,
          s.originalUrl
        );
      })
      .collect(ImmutableSeq.factory());
  }

  public static @NotNull String getHtml(@NotNull String url) {
    try (var httpClient = HttpClient.newHttpClient()) {
      var request = HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .GET()
        .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return response.body();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}