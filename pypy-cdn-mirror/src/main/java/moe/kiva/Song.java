package moe.kiva;

import org.jetbrains.annotations.NotNull;

public record Song(
  int id,
  int category,
  @NotNull String name,
  @NotNull String categoryName,
  boolean flip,
  int start,
  int end,
  boolean skipRandom,
  float volume
) {
  public @NotNull String prettyCategoryName() {
    var trim = categoryName.trim();
    return trim.isEmpty() ? "Category %d".formatted(category) : categoryName;
  }
}
