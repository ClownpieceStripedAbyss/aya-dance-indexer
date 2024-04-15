package moe.kiva;

import com.google.gson.GsonBuilder;
import kala.collection.immutable.ImmutableMap;
import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
  public static void main(String[] args) throws IOException {
    var songList = SongPyPyApiParser.parse();

    downloadVideos(songList);
    generateVidViz(songList);
  }

  private static void downloadVideos(ImmutableSeq<Song> songList) {
    var downloader = Downloader.create(
      songList,
      "./pypydance-song",
      30,
      // Better use an IP pool, as we are crawling a lot of videos
      new InetSocketAddress("127.0.0.1", 10809)
    );
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
    int id,
    @NotNull String title,
    @NotNull String url,
    @NotNull String urlForQuest,
    int playerIndex,
    float volume,
    int start,
    int end,
    boolean flip
  ) {
    public VidVizSong(@NotNull Song song) {
      this(
        song.id(),
        song.name(),
        "https://jd-testing.kiva.moe/api/v1/videos/%d.mp4".formatted(song.id()),
        "",
        0,
        song.volume(),
        song.start(),
        song.end(),
        song.flip()
      );
    }
  }
}
