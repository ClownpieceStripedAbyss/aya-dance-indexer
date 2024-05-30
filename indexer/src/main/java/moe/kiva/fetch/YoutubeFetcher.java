package moe.kiva.fetch;

import moe.kiva.Song;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum YoutubeFetcher implements Fetcher {
  INSTANCE;

  static @Nullable String extractVideoId(@NotNull String url) {
    var index = url.indexOf("v=");
    return index == -1 ? null : url.substring(index + 2);
  }

  @Override public boolean canFetch(@NotNull String url) {
    return url.startsWith("https://youtube.com/watch?v=")
      || url.startsWith("https://www.youtube.com/watch?v=");
  }

  @Override public @NotNull SongMetadata doComputeMetadata(@NotNull SongInput input) throws Exception {
    return new SongMetadata(
      new Song(
        input.id(),
        input.categoryId(),
        "title",
        input.categoryName(),
        null,
        null,
        "titleSpell",
        0,
        input.volume(),
        0,
        600,
        input.flip(),
        false,
        List.of(input.originalUrl()),
        "checksum"
      ),
      input.originalUrl()
    );
  }
}
