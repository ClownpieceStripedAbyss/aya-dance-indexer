package moe.kiva;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.control.Try;
import kala.value.primitive.IntVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public record Downloader(
  @NotNull ImmutableSeq<Song> songs,
  @NotNull ImmutableSeq<Song> ayaSongs,
  @NotNull MutableList<Song> failed,
  @NotNull String outputDir,
  @NotNull ExecutorService executor,
  @NotNull Sync sync,
  @NotNull HttpClient httpClient,
  long delaySeconds,
  boolean trustLocalFiles
) {
  private static @NotNull String jdApiForSong(int id) {
    return "https://jd.pypy.moe/api/v1/videos/%d.mp4".formatted(id);
  }

  private static final @NotNull ImmutableSeq<String> CDN_LIST = ImmutableSeq.of(
    "http://storage-kr1.llss.io",
    "http://storage-kr2.llss.io"
  );

  public static @NotNull Downloader create(
    @NotNull ImmutableSeq<Song> songs,
    @NotNull ImmutableSeq<Song> ayaSongs,
    @NotNull String outputDir,
    long delaySeconds,
    @Nullable InetSocketAddress proxy,
    boolean trustLocalFiles
  ) {
    var client = ApiHelper.httpClient(false, proxy);
    return new Downloader(songs, ayaSongs, MutableList.create(), outputDir,
      Executors.newFixedThreadPool(4),
      new Sync(songs.size(), new IntVar(0), new CountDownLatch(songs.size())),
      client, delaySeconds, trustLocalFiles);
  }

  public void downloadAllMulti() {
    System.out.println("Spawning download tasks...");
    songs.forEach(song -> executor.submit(() -> downloadWithSleep(song)));

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
    songs.forEach(this::downloadWithSleep);
    System.out.printf("Downloaded %d songs, failed: %d%n", sync.current(), failed.size());
  }

  public void downloadWithSleep(Song song) {
    if (downloadOne(song)) {
      try {
        Thread.sleep(delaySeconds * 1000);
      } catch (InterruptedException ignored) {
      }
    }
  }

  private boolean alreadyDownloaded(@NotNull Song song, @NotNull Path metadata, @NotNull Path video) {
    if (!Files.exists(video)) return false;
    if (!Files.exists(metadata)) return false;
    try {
      var contents = Files.readString(metadata, StandardCharsets.UTF_8);
      var fromMetadata = Try.of(() -> new Gson().fromJson(contents, Song.class)).getOrNull();

      // `fromMetadata == null` implies there's a breaking change in the format of the json,
      // we should always fix it.
      var already = fromMetadata == null
        || isMetadataForSameSong(song, fromMetadata);
      var needFixMetadata = fromMetadata == null
        || needFixMetadata(song, fromMetadata);
      var needFixMetadataForTrustLocalFiles = trustLocalFiles
        && fromMetadata != null
        && fromMetadata.checksum() == null;

      // if trustLocalFiles, don't check checksum
      var checksumMismatch = !trustLocalFiles
        && song.checksum() != null
        && !Objects.equals(song.checksum(), Song.computeChecksum(video));

      if (already && checksumMismatch) {
        System.out.printf("WARN: Checksum mismatch for song: %d, name: %s, deleting %n", song.id(), song.title());
        Files.deleteIfExists(video);
        Files.deleteIfExists(metadata);
        return false;
      }
      if (already && (needFixMetadata || needFixMetadataForTrustLocalFiles)) {
        System.out.printf("[%d/%d] Patching downloaded id: %d, name: %s, metadata mismatch%n",
          sync.current(), sync.total,
          song.id(), song.title());
        if (trustLocalFiles) {
          if (fromMetadata != null && fromMetadata.checksum() != null) {
            // if trustLocalFiles, use the old checksum if it exists
            song = song.withChecksum(fromMetadata.checksum());
          } else {
            // Ok, now we have to compute the checksum
            var checksum = Song.computeChecksum(video);
            song = checksum == null ? song : song.withChecksum(checksum);
          }
        }
        // Ok, patch it
        saveMetadata(song, metadata);
      }
      return already;
    } catch (IOException e) {
      System.err.printf("WARN: Failed to check metadata for song: %d: %s%n", song.id(), e.getMessage());
    } catch (JsonSyntaxException e) {
      System.err.printf("WARN: Failed to parse metadata for song: %d: %s%n", song.id(), e.getMessage());
      return false;
    }
    return false;
  }

  private boolean isMetadataForSameSong(@NotNull Song song, @NotNull Song fromMetadata) {
    return fromMetadata.id() == song.id();
  }

  private boolean needFixMetadata(@NotNull Song song, @NotNull Song fromMetadata) {
    return !Objects.equals(song.categoryName(), fromMetadata.categoryName())
      || !Objects.equals(song.title(), fromMetadata.title())
      || !Objects.equals(song.titleSpell(), fromMetadata.titleSpell())
      || !Objects.equals(song.originalUrl(), fromMetadata.originalUrl())
      // only patch local checksum if we have a reliable always-correct one from AyaDance
      || (song.checksum() != null && !Objects.equals(song.checksum(), fromMetadata.checksum()))
      || fromMetadata.category() != song.category()
      || fromMetadata.start() != song.start()
      || fromMetadata.end() != song.end()
      || fromMetadata.flip() != song.flip()
      || fromMetadata.skipRandom() != song.skipRandom()
      || fromMetadata.volume() != song.volume();
  }

  private @NotNull Song prepareSong(@NotNull Song rawSong, @NotNull Path metadata, @NotNull Path video) {
    // trustLocalFiles should only be used by Kiva for bootstrapping aya-dance-cf.kiva.moe
    if (trustLocalFiles) {
      // Delay the checksum computation to the last moment!!!
      // if (Files.exists(metadata) && Files.exists(video))
      //   return rawSong.withChecksumFromFile(video);
      if (!Files.exists(metadata) || !Files.exists(video)) {
        System.out.printf("[INFO] Hi Kiva, looks like this video id: %d, name: %s is not downloaded yet%n", rawSong.id(), rawSong.title());
      }
      return rawSong;
    }
    var ayaSong = ayaSongs.findFirst(s -> s.id() == rawSong.id());
    // Ok, the file is not found in the Aya Dance Index,
    // we have no reliable source for the checksum, so we just return the raw song,
    // and hope the user's network is good enough to download the video.
    return ayaSong.getOrElse(() -> {
      System.err.printf("WARN: No reliable source for checksum for id: %d, name: %s%n", rawSong.id(), rawSong.title());
      return rawSong;
    });
  }

  public boolean downloadOne(@NotNull Song rawSong) {
    var basedir = Path.of(outputDir, String.valueOf(rawSong.id()));
    var video = basedir.resolve("video.mp4");
    var metadata = basedir.resolve("metadata.json");
    var downloadUrl = basedir.resolve("download.txt");

    try {
      var song = prepareSong(rawSong, metadata, video);

      if (alreadyDownloaded(song, metadata, video)) {
        System.out.printf("[%d/%d] Skipping id: %d, name: %s, already downloaded%n",
          sync.current(), sync.total,
          song.id(), song.title());
        return false;
      }

      System.out.printf(
        "[%d/%d] Start id: %d, name: %s, saving to: %s%n",
        sync.current(), sync.total, song.id(), song.title(), video
      );

      Files.createDirectories(basedir);
      var videoUrl = downloadVideoFromCDN(song.id(), video);
      var checkedSong = song.withChecksumFromFile(video);

      if (song.checksum() == null || Objects.equals(song.checksum(), checkedSong.checksum())) {
        saveMetadata(trustLocalFiles ? checkedSong : song, metadata);
        Files.writeString(downloadUrl, videoUrl, StandardCharsets.UTF_8);
        System.out.printf(
          "[%d/%d] OK id: %d, name: %s, from: %s%n",
          sync.current(), sync.total, checkedSong.id(), checkedSong.title(), videoUrl
        );
      } else {
        System.err.printf(
          "[%d/%d] ERROR id: %d, name: %s, checksum mismatch: from remote: %s, from local: %s%n",
          sync.current(), sync.total, song.id(), song.title(),
          song.checksum(), checkedSong.checksum()
        );
        markFailed(rawSong);
      }
    } catch (Exception e) {
      System.err.printf("ERROR: Failed to download song %d: %s%n", rawSong.id(), e.getMessage());
      markFailed(rawSong);
    } finally {
      sync.increment();
    }
    return true;
  }

  private void saveMetadata(@NotNull Song song, @NotNull Path metadata) throws IOException {
    var json = new GsonBuilder().setPrettyPrinting().create().toJson(song);
    Files.writeString(metadata, json, StandardCharsets.UTF_8);
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
    var list = CDN_LIST.stream().collect(Collectors.toList());
    Collections.shuffle(list);
    for (var cdn : list) {
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
        System.err.printf("WARN: Failed to download song %d from CDN %s: %s, trying another in 10s...%n", id, url, e.getMessage());
        try {
          Thread.sleep(10 * 1000);
        } catch (InterruptedException ignored) {
        }
      }
    }
    throw new RuntimeException("Failed to download song %d from all CDNs: %s".formatted(id, videoHash));
  }

  private synchronized void markFailed(@NotNull Song song) {
    failed.append(song);
    try {
      Files.writeString(Path.of(outputDir, "failed.txt"),
        "%d,%d,%s\n".formatted(song.id(), song.category(), song.title()),
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
