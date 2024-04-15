package moe.kiva;

import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public class GoogleDocParser {
  public static final @NotNull String DEFAULT_SONG_LIST_URL = "https://docs.google.com/spreadsheets/u/1/d/e/2PACX-1vQAvsUeoYncuBCN3iJs6RpNFONmUvWumoK4SqKWsJ3svLAY_t0cPvneaGrDQwxzGj4k1RaJ-EhkrRFY/pubhtml";

  public static @NotNull ImmutableSeq<Song> parseSongList() {
    return parseSongList(DEFAULT_SONG_LIST_URL);
  }

  public static @NotNull ImmutableSeq<Song> parseSongList(@NotNull String url) {
    var html = getHtml(url);
    return Jsoup.parse(html)
      .select("tr")
      .stream()
      .map(tr -> {
        var tds = tr.select("td");
        if (tds.size() != 3) {
          return null;
        }
        try {
          var id = Integer.parseInt(tds.get(0).text());
          var category = Integer.parseInt(tds.get(1).text());
          var name = tds.get(2).text();
          return new Song(id, category, name,
            "", false, 0, 0, false, 1.0f
          );
        } catch (NumberFormatException e) {
          return null;
        }
      })
      .filter(Objects::nonNull)
      .collect(ImmutableSeq.factory());
  }

  private static @NotNull String getHtml(@NotNull String url) {
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
