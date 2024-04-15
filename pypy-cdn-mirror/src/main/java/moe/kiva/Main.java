package moe.kiva;

import com.google.gson.GsonBuilder;
import kala.collection.immutable.ImmutableMap;
import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
  public static final @NotNull String DEFAULT_SONG_LIST_URL = "https://docs.google.com/spreadsheets/u/1/d/e/2PACX-1vQAvsUeoYncuBCN3iJs6RpNFONmUvWumoK4SqKWsJ3svLAY_t0cPvneaGrDQwxzGj4k1RaJ-EhkrRFY/pubhtml";

  public static void main(String[] args) throws IOException {
    var songListUrl = args.length == 1
      ? args[0]
      : DEFAULT_SONG_LIST_URL;
    var songList = PyPySongListParser.parseSongList(songListUrl);

    downloadVideos(songList);
    generateVidViz(songList);
  }

  private static void downloadVideos(ImmutableSeq<PyPySongListParser.Song> songList) {
    var downloader = PyPySongDownloader.create(songList, "./pypydance-song");
    downloader.downloadAllMulti();
  }

  private static void generateVidViz(@NotNull ImmutableSeq<PyPySongListParser.Song> songList) throws IOException {
    var grouped = ImmutableMap.from(songList.stream()
        .collect(Collectors.groupingBy(PyPySongListParser.Song::category)))
      .view()
      .map((cat, songs) -> new VidVizSongList(
        "Category %d".formatted(cat),
        songs.stream()
          .map(VidVizSong::new)
          .collect(Collectors.toList())
      ))
      .toImmutableSeq()
      .prepended(new VidVizSongList(
        "All Songs",
        songList.stream()
          .map(VidVizSong::new)
          .collect(Collectors.toList())
      ))
      .asJava();

    var json = new GsonBuilder()
      .setPrettyPrinting()
      .create()
      .toJson(grouped);
    Files.writeString(Path.of("vidviz-songs.json"), json, StandardCharsets.UTF_8);
  }

  record VidVizSongList(
    @NotNull String title,
    @NotNull List<VidVizSong> entries
  ) {
  }

  record VidVizSong(
    @NotNull String title,
    @NotNull String url,
    @NotNull String urlForQuest,
    int playerIndex
  ) {
    public VidVizSong(@NotNull PyPySongListParser.Song song) {
      this(
        "%d: %s".formatted(song.id(), song.name()),
        "https://jd-testing.kiva.moe/api/v1/videos/%d.mp4".formatted(song.id()),
        "",
        0
      );
    }
  }
}
