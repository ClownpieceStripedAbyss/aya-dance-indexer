package moe.kiva;

import com.google.gson.GsonBuilder;
import kala.collection.immutable.ImmutableMap;
import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {
  record AppOpts(
    @NotNull String outputDir,
    @Nullable InetSocketAddress proxy,
    int downloadDelay
  ) {
    public static @NotNull AppOpts parseEnv() {
      try {
        var envContents = Files.readString(Path.of(".env"), StandardCharsets.UTF_8);
        var p = new Properties();
        p.load(new StringReader(envContents));

        var mirrorDownloadProxy = p.getProperty("MIRROR_DOWNLOAD_PROXY", "");
        var videoPath = p.getProperty("VIDEO_PATH", "./pypydance-song");
        var mirrorDownloadDelay = p.getProperty("MIRROR_DOWNLOAD_DELAY", "30");

        if (mirrorDownloadProxy.isBlank()) {
          return new AppOpts(
            videoPath,
            null,
            Integer.parseInt(mirrorDownloadDelay)
          );
        }

        var split = mirrorDownloadProxy.split(":");
        if (split.length != 2) throw new IllegalArgumentException("Invalid proxy format");

        var proxyHost = split[0];
        var proxyPort = Integer.parseInt(split[1]);

        return new AppOpts(
          videoPath,
          new InetSocketAddress(proxyHost, proxyPort),
          Integer.parseInt(mirrorDownloadDelay)
        );
      } catch (IOException e) {
        return new AppOpts("./pypydance-song", null, 30);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    var opts = AppOpts.parseEnv();
    System.out.printf("Video path: %s%n", opts.outputDir);
    System.out.printf("Proxy: %s%n", opts.proxy);
    System.out.printf("Download delay: %d%n", opts.downloadDelay);

    var songList = SongPyPyApiParser.parse();

    downloadVideos(opts, songList);
    generateVidViz(songList);
  }

  private static void downloadVideos(@NotNull AppOpts opts, @NotNull ImmutableSeq<Song> songList) {
    var downloader = Downloader.create(
      songList,
      opts.outputDir,
      opts.downloadDelay,
      opts.proxy
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
