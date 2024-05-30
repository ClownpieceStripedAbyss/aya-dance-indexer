package moe.kiva.fetch;

import moe.kiva.Song;
import org.jetbrains.annotations.NotNull;

public record SongMetadata(
  @NotNull Song song,
  @NotNull String downloadUrl
) {
}
