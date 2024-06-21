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
  @NotNull Option<String> titlePrefix,
  @NotNull Option<String> titleSuffix,
  @NotNull Option<String> titleContentRemove,
  @NotNull Option<Integer> startOverride,
  @NotNull Option<Integer> endOverride
) {
  public @NotNull SongMetadata toMetadata(@NotNull Supplier<String> titleFetcher, @NotNull IntSupplier durationInSecondsFetcher) {
    // If titleOverride is present, use it directly and skip all title mappers
    var title = this.titleOverride().getOrElse(() -> {
      var mappedTitle = titleFetcher.get();
      // Remove content first, or we may mistakenly remove some prefix or suffix
      if (this.titleContentRemove().isDefined()) mappedTitle = mappedTitle.replace(this.titleContentRemove().get(), "");
      if (this.titlePrefix().isDefined()) mappedTitle = this.titlePrefix().get() + " " + mappedTitle;
      if (this.titleSuffix().isDefined()) mappedTitle = mappedTitle + " " + this.titleSuffix().get();
      return mappedTitle.trim();
    });
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
