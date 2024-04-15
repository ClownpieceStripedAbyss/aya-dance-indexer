package moe.kiva;

import kala.control.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Song(
  int id,
  int category,
  @NotNull String name,
  @Nullable String categoryName,
  boolean flip,
  int start,
  int end,
  boolean skipRandom,
  float volume
) {
  public @NotNull String prettyCategoryName() {
    return Option.ofNullable(categoryName)
      .filter(String::isBlank)
      .getOrElse(() -> "Category %d".formatted(category));
  }
}
