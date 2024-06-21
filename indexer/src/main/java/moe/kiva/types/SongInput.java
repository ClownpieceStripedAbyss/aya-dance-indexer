package moe.kiva.types;

import kala.control.Option;
import moe.kiva.Song;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

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
  public @NotNull SongMetadata toMetadata(@NotNull Supplier<String> titleFetcher, IntSupplier durationInSecondsFetcher) {
    var title = this.titleOverride().getOrElse(titleFetcher);
    var end = this.endOverride().getOrElse(durationInSecondsFetcher::getAsInt);
    var spell = Song.spell(title);

    return new SongMetadata(
      new Song(
        this.id(),
        this.categoryId(),
        title,
        this.categoryName(),
        null,
        null,
        spell,
        0,
        this.volume(),
        this.startOverride().getOrDefault(0),
        end,
        this.flip(),
        false,
        List.of(this.originalUrl()),
        "CHECKSUM-PLACEHOLDER"
      ),
      this.originalUrl()
    );
  }
}
