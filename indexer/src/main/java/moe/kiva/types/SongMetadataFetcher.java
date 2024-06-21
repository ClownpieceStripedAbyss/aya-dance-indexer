package moe.kiva.types;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Result;
import kala.control.Try;
import moe.kiva.fetchers.BiliBiliFetcher;
import moe.kiva.fetchers.YoutubeFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SongMetadataFetcher {
  @NotNull ImmutableSeq<SongMetadataFetcher> FETCHERS = ImmutableSeq.of(
    YoutubeFetcher.INSTANCE,
    BiliBiliFetcher.INSTANCE
  );

  static @Nullable SongMetadata fetchMetadata(@NotNull SongInput input) {
    var metadata = doFetchMetadata(input);
    if (metadata == null) return null;
    for (SongMetadataMapper mapper : SongMetadataMapper.MAPPERS) {
      if (mapper.shouldWorkOn(metadata)) {
        metadata = mapper.mapMetadata(metadata);
      }
    }
    return metadata;
  }

  private static @Nullable SongMetadata doFetchMetadata(@NotNull SongInput input) {
    for (SongMetadataFetcher fetcher : FETCHERS) {
      if (fetcher.canFetch(input.originalUrl())) {
        var x = fetcher.computeMetadata(input);
        if (x.isErr()) {
          System.out.println("Error fetching metadata: ");
          x.getErr().printStackTrace(System.err);
        }
        return x.getOrNull();
      }
    }
    return null;
  }

  boolean canFetch(@NotNull String url);
  @NotNull SongMetadata doComputeMetadata(@NotNull SongInput input) throws Exception;

  default @NotNull Result<SongMetadata, Throwable> computeMetadata(@NotNull SongInput input) {
    return Try.of(() -> doComputeMetadata(input)).toResult();
  }
}
