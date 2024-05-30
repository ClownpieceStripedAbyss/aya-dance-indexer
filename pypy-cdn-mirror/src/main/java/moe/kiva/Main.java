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
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {
  record AppOpts(
    @NotNull String outputDir,
    @Nullable InetSocketAddress proxy,
    int downloadDelay,
    boolean trustLocalFiles,
    boolean generateVidViz
  ) {
    public static @NotNull AppOpts parseEnv() {
      try {
        var envContents = Files.readString(Path.of(".env"), StandardCharsets.UTF_8);
        var p = new Properties();
        p.load(new StringReader(envContents));

        var mirrorDownloadProxy = p.getProperty("MIRROR_DOWNLOAD_PROXY", "");
        var videoPath = p.getProperty("VIDEO_PATH", "./pypydance-song");
        var trustLocalFiles = Boolean.parseBoolean(p.getProperty("TRUST_LOCAL_FILES", "false"));
        var generateVidViz = Boolean.parseBoolean(p.getProperty("GENERATE_VIDVIZ_JSON", "false"));
        var mirrorDownloadDelay = p.getProperty("MIRROR_DOWNLOAD_DELAY", "30");

        if (mirrorDownloadProxy.isBlank()) {
          return new AppOpts(
            videoPath,
            null,
            Integer.parseInt(mirrorDownloadDelay),
            trustLocalFiles,
            generateVidViz
          );
        }

        var split = mirrorDownloadProxy.split(":");
        if (split.length != 2) throw new IllegalArgumentException("Invalid proxy format");

        var proxyHost = split[0];
        var proxyPort = Integer.parseInt(split[1]);

        return new AppOpts(
          videoPath,
          new InetSocketAddress(proxyHost, proxyPort),
          Integer.parseInt(mirrorDownloadDelay),
          trustLocalFiles,
          generateVidViz
        );
      } catch (IOException e) {
        return new AppOpts("./pypydance-song", null, 30, false, false);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    var opts = AppOpts.parseEnv();
    System.out.printf("Video path: %s%n", opts.outputDir);
    System.out.printf("Proxy: %s%n", opts.proxy);
    System.out.printf("Download delay: %d%n", opts.downloadDelay);

    var songList = PyPyApi.parse();
    var ayaSongList = opts.trustLocalFiles ? ImmutableSeq.<Song>empty() : AyaApi.parse();

    downloadVideos(opts, songList, ayaSongList);
    if (opts.generateVidViz) generateVidViz(songList);
  }

  private static void downloadVideos(@NotNull AppOpts opts, @NotNull ImmutableSeq<Song> songList, @NotNull ImmutableSeq<Song> ayaSongList) {
    if (opts.trustLocalFiles) {
      System.out.println("Trust local files enabled, but if you're not sure, please disable it.");
    }
    var downloader = Downloader.create(
      songList,
      ayaSongList,
      opts.outputDir,
      opts.downloadDelay,
      opts.proxy,
      opts.trustLocalFiles
    );
    downloader.downloadAllMulti();
  }

  private static void generateVidViz(@NotNull ImmutableSeq<Song> songList) throws IOException {
    var grouped = ImmutableMap.from(songList.stream()
        .collect(Collectors.groupingBy(Song::categoryName)))
      .view()
      .map(SongList::new)
      .toImmutableSeq()
      .view()
      .prepended(new SongList(
        "Song's Family | VRChat Dance",
        songList.view()
          .filter(x -> x.title().contains("[Song]"))
          .toImmutableSeq()
          .sorted(Comparator.comparingInt(Song::id))
          .asJava()
      ))
      .prepended(new SongList(
        "All Songs",
        songList.asJava()
      ))
      .appended(new SongList(
        "Kiva's Test List",
        ImmutableSeq.of(
            findById(songList, 1589),
            findById(songList, 3552),
            findById(songList, 1840),
            findById(songList, 1430),
            findById(songList, 1333),
            findById(songList, 3470)
          )
          .sorted(Comparator.comparingInt(Song::id))
          .asJava()
      ))
      .toImmutableSeq()
      .asJava();

    var json = new GsonBuilder()
      .setPrettyPrinting()
      .create()
      .toJson(grouped);
    Files.writeString(Path.of("vidviz-songs.json"), json, StandardCharsets.UTF_8);
  }

  private static @NotNull Song findById(@NotNull ImmutableSeq<Song> songList, int id) {
    return songList.find(s -> s.id() == id)
      .getOrThrow(() -> new IllegalArgumentException("Song not found: %d".formatted(id)));
  }
}
