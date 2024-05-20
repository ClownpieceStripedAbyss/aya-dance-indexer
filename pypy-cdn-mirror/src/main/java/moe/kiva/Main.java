package moe.kiva;

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum;
import com.github.houbb.pinyin.util.PinyinHelper;
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
      .view()
      .prepended(new VidVizSongList(
        "All Songs",
        songList.stream()
          .map(VidVizSong::new)
          .collect(Collectors.toList())
      ))
      .appended(new VidVizSongList(
        "Kiva's Test List",
        ImmutableSeq.of(
            findById(songList, 1589),
            findById(songList, 3552),
            findById(songList, 1840),
            findById(songList, 1430),
            findById(songList, 1333),
            findById(songList, 3470)
          )
          .sorted(Comparator.comparingInt(VidVizSong::id))
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

  private static @NotNull VidVizSong findById(@NotNull ImmutableSeq<Song> songList, int id) {
    return songList.find(s -> s.id() == id)
      .map(VidVizSong::new)
      .getOrThrow(() -> new IllegalArgumentException("Song not found: %d".formatted(id)));
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
    @NotNull String titleSpell,
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
        "https://aya-dance-cf.kiva.moe/api/v1/videos/%d.mp4".formatted(song.id()),
        "",
        spell(song.name()),
        0,
        song.volume(),
        song.start(),
        song.end(),
        song.flip()
      );
    }

    private static @NotNull String spell(@NotNull String name) {
      try {
        var s = PinyinHelper.toPinyin(name, PinyinStyleEnum.FIRST_LETTER);
        var bytes = s.getBytes(StandardCharsets.UTF_8);
        return new String(bytes, StandardCharsets.UTF_8);
      } catch (Throwable t) {
        return name;
      }
    }
  }
}
