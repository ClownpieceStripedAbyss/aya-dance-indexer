package moe.kiva.fetch;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Result;
import kala.control.Try;
import org.jetbrains.annotations.NotNull;

public interface Fetcher {
  @NotNull ImmutableSeq<Fetcher> FETCHERS = ImmutableSeq.of(
    YoutubeFetcher.INSTANCE
  );

  boolean canFetch(@NotNull String url);
  @NotNull SongMetadata doComputeMetadata(@NotNull SongInput input) throws Exception;

  default @NotNull Result<SongMetadata, Throwable> computeMetadata(@NotNull SongInput input) {
    return Try.of(() -> doComputeMetadata(input)).toResult();
  }
}
