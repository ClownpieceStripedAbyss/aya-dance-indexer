package moe.kiva;

import com.google.gson.Gson;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.value.primitive.IntVar;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public record PyPySongDownloader(
  @NotNull ImmutableSeq<PyPySongListParser.Song> songs,
  @NotNull MutableList<PyPySongListParser.Song> failed,
  @NotNull String outputDir,
  @NotNull ExecutorService executor,
  @NotNull Sync sync,
  @NotNull HttpClient httpClient
) {
  private static @NotNull String jdApiForSong(int id) {
    return "https://jd.pypy.moe/api/v1/videos/%d.mp4".formatted(id);
  }

  private static final @NotNull ImmutableSeq<String> CDN_LIST = ImmutableSeq.of(
    "http://pypy.qwertyuiop.nz",
    "http://storage-kr1.llss.io",
    "http://storage-cf.llss.io"
  );

  public static @NotNull PyPySongDownloader create(
    @NotNull ImmutableSeq<PyPySongListParser.Song> songs,
    @NotNull String outputDir
  ) {
    var client = HttpClient.newBuilder()
      // .followRedirects(HttpClient.Redirect.ALWAYS)
      .version(HttpClient.Version.HTTP_2)
      .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 10809)))
      .build();
    return new PyPySongDownloader(songs, MutableList.create(), outputDir,
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
      new Sync(songs.size(), new IntVar(0), new CountDownLatch(songs.size())),
      client);
  }

  public void downloadAllMulti() {
    System.out.println("Spawning download tasks...");
    songs.forEach(song -> executor.submit(() -> {
      downloadOne(song);

      try {
        Thread.sleep(10000);
      } catch (InterruptedException ignored) {
      }
    }));

    // wait for all to finish
    try {
      sync.latch.await();
    } catch (InterruptedException e) {
      System.err.printf("Download tasks interrupted: %s%n", e.getMessage());
    }
    executor.shutdown();
    System.out.printf("Download tasks completed, failed: %d%n", failed.size());
  }

  public void downloadAllSingle() {
    System.out.println("Downloading songs...");
    songs.forEach(song -> {
      if (downloadOne(song)) {
        try {
          Thread.sleep(30 * 1000);
        } catch (InterruptedException ignored) {
        }
      }
    });
    System.out.printf("Downloaded %d songs, failed: %d%n", sync.current(), failed.size());
  }

  public boolean downloadOne(@NotNull PyPySongListParser.Song song) {
    var basedir = Path.of(outputDir, String.valueOf(song.id()));
    var video = basedir.resolve("video.mp4");
    var metadata = basedir.resolve("metadata.json");
    var downloadUrl = basedir.resolve("download.txt");

    if (Files.exists(metadata)) {
      System.out.printf("[%d/%d] Skipping id: %d, name: %s, already downloaded%n",
        sync.current(), sync.total,
        song.id(), song.name());
      sync.increment();
      return false;
    }

    try {
      System.out.printf(
        "[%d/%d] Start id: %d, name: %s, saving to: %s%n",
        sync.current(), sync.total, song.id(), song.name(), video
      );

      Files.createDirectories(basedir);
      var videoUrl = downloadVideoFromCDN(song.id(), video);
      var json = new Gson().toJson(song);
      Files.writeString(metadata, json, StandardCharsets.UTF_8);
      Files.writeString(downloadUrl, videoUrl, StandardCharsets.UTF_8);
      System.out.printf(
        "[%d/%d] OK id: %d, name: %s, from: %s%n",
        sync.current(), sync.total, song.id(), song.name(), videoUrl
      );
      sync.increment();
    } catch (Exception e) {
      System.err.printf("Failed to download song %d: %s%n", song.id(), e.getMessage());
      markFailed(song);
    }
    return true;
  }

  private @NotNull HttpRequest makeRequest(int chromeVer, @NotNull String url) {
    return HttpRequest.newBuilder()
      .uri(java.net.URI.create(url))
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
      .header("Accept-Encoding", "gzip, deflate, br, zstd")
      .header("Accept-Language", "en-US,en;q=0.9")
      .header("Cache-Control", "no-cache")
      .header("Dnt", "1")
      .header("Pragma", "no-cache")
      .header("Sec-Ch-Ua", "\"Google Chrome\";v=\"%d\", \"Not:A-Brand\";v=\"99\", \"Chromium\";v=\"%d\"".formatted(chromeVer, chromeVer))
      .header("Sec-Ch-Ua-Mobile", "?0")
      .header("Sec-Ch-Ua-Platform", "\"Windows\"")
      .header("Sec-Fetch-Dest", "document")
      .header("Sec-Fetch-Mode", "navigate")
      .header("Sec-Fetch-Site", "none")
      .header("Sec-Fetch-User", "?1")
      .header("Upgrade-Insecure-Requests", "1")
      .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.0.0 Safari/537.36".formatted(chromeVer))
      .GET()
      .build();
  }

  private @NotNull String downloadVideoFromCDN(int id, @NotNull Path video) {
    var apiUrl = jdApiForSong(id);
    var chromeVer = 123;
    try {
      // this response may be a 302 redirect
      var apiRes = httpClient.send(
        makeRequest(chromeVer, apiUrl),
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
      );

      if (apiRes.statusCode() != 302) {
        throw new RuntimeException("Failed to get video url for song %d from %s (status != 302): "
          .formatted(id, apiUrl));
      }
      var location = apiRes.headers().firstValue("Location");
      if (location.isEmpty()) {
        throw new RuntimeException("Failed to get video url for song %d from %s (no Location header): "
          .formatted(id, apiUrl));
      }

      // The location looks like: http://jd.pypy.moe/api/v1/videos/43LCI6KmSKM.mp4
      // We need to extract the 43LCI6KmSKM.mp4
      var videoHash = location.get().substring(location.get().lastIndexOf('/') + 1);

      // Now download the video from the CDN
      return downloadVideoFromCDN(id, video, videoHash);

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private @NotNull String downloadVideoFromCDN(int id, @NotNull Path video, @NotNull String videoHash) {
    for (var cdn : CDN_LIST) {
      var url = "%s/%s".formatted(cdn, videoHash);
      try {
        var res = httpClient.send(
          makeRequest(123, url),
          HttpResponse.BodyHandlers.ofFile(video)
        );
        if (res.statusCode() == 200) {
          // Ok, we got the video
          return url;
        }
      } catch (IOException | InterruptedException e) {
        System.err.printf("Failed to download song %d from CDN %s: %s, trying another in 10s...%n", id, url, e.getMessage());
        try {
          Thread.sleep(10 * 1000);
        } catch (InterruptedException ignored) {
        }
      }
    }
    throw new RuntimeException("Failed to download song %d from all CDNs: %s".formatted(id, videoHash));
  }

  private synchronized void markFailed(@NotNull PyPySongListParser.Song song) {
    failed.append(song);
    try {
      Files.writeString(Path.of(outputDir, "failed.txt"),
        "%d,%d,%s\n".formatted(song.id(), song.category(), song.name()),
        StandardCharsets.UTF_8,
        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    } catch (IOException ignored) {
    }
  }

  private record Sync(
    int total,
    @NotNull IntVar downloaded,
    @NotNull CountDownLatch latch
  ) {
    public synchronized void increment() {
      downloaded.getAndAdd(1);
      latch.countDown();
    }

    public synchronized int current() {
      return downloaded.get();
    }
  }
}
