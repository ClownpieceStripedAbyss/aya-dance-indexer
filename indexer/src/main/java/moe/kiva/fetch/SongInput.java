package moe.kiva.fetch;

import kala.control.Option;
import org.jetbrains.annotations.NotNull;

public record SongInput(
  @NotNull String originalUrl,
  int id,
  int categoryId,
  @NotNull String categoryName,
  float volume,
  boolean flip,

  @NotNull Option<String> titleOverride,
  @NotNull Option<Integer> startOverride,
  @NotNull Option<Integer> endOverride
) {
}
