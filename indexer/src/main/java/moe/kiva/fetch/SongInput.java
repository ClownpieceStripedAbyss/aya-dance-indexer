package moe.kiva.fetch;

import org.jetbrains.annotations.NotNull;

public record SongInput(
  @NotNull String originalUrl,
  int id,
  int categoryId,
  @NotNull String categoryName,
  float volume,
  boolean flip
) {
}
