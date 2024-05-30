package moe.kiva.fetch;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Result;
import kala.control.Try;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Fetcher {
  @NotNull ImmutableSeq<Fetcher> FETCHERS = ImmutableSeq.of(
    YoutubeFetcher.INSTANCE
  );

  static @Nullable SongMetadata fetchMetadata(@NotNull SongInput input) {
    for (Fetcher fetcher : FETCHERS) {
      if (fetcher.canFetch(input.originalUrl())) {
        return fetcher.computeMetadata(input).getOrNull();
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
