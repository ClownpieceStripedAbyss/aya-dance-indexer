package moe.kiva.types;

import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

public interface SongMetadataMapper {
  @NotNull
  ImmutableSeq<SongMetadataMapper> MAPPERS = ImmutableSeq.of(
  );

  boolean shouldWorkOn(@NotNull SongMetadata metadata);

  @NotNull SongMetadata mapMetadata(@NotNull SongMetadata metadata);
}
