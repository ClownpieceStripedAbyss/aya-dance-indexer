package moe.kiva;

import kala.control.Option;
import org.jetbrains.annotations.NotNull;

public record Song(
  int id,
  int category,
  @NotNull String name,
  @NotNull Option<String> categoryName,
  boolean flip,
  int start,
  int end,
  boolean skipRandom,
  float volume
) {
  public @NotNull String prettyCategoryName() {
    return categoryName
      .filter(String::isBlank)
      .getOrElse(() -> "Category %d".formatted(category));
  }
}
