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

  public static void main(String[] args) throws IOException {
    var songList = GoogleDocParser.parseSongList();

    downloadVideos(songList);
    generateVidViz(songList);
  }

  private static void downloadVideos(ImmutableSeq<Song> songList) {
    var downloader = Downloader.create(songList, "./pypydance-song");
    downloader.downloadAllMulti();
  }

  private static void generateVidViz(@NotNull ImmutableSeq<Song> songList) throws IOException {
    var grouped = ImmutableMap.from(songList.stream()
        .collect(Collectors.groupingBy(Song::category)))
      .view()
      .map((cat, songs) -> {
        var catName = songs.isEmpty() ? "Category %d".formatted(cat) :
          songs.getFirst().prettyCategoryName();
        return new VidVizSongList(
          catName,
          songs.stream()
            .map(VidVizSong::new)
            .collect(Collectors.toList())
        );
      })
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
    public VidVizSong(@NotNull Song song) {
      this(
        "%d: %s".formatted(song.id(), song.name()),
        "https://jd-testing.kiva.moe/api/v1/videos/%d.mp4".formatted(song.id()),
        "",
        0
      );
    }
  }
}
