package moe.kiva;

import com.google.gson.GsonBuilder;
import kala.control.Option;
import moe.kiva.types.SongInput;
import moe.kiva.types.SongMetadataFetcher;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.util.Objects;
import java.util.concurrent.Callable;

public class Main extends MainArgs implements Callable<Integer> {
  public static void main(String @NotNull [] args) {
    System.exit(new CommandLine(new Main()).execute(args));
  }

  private static @NotNull SongInput parseSongInput(@NotNull MainArgs args) {
    return new SongInput(
      Objects.requireNonNull(args.url),
      args.id,
      args.catId,
      Objects.requireNonNull(args.catName),
      args.volume,
      args.flip,
      Option.ofNullable(args.titleOverride),
      Option.ofNullable(args.startOverride),
      Option.ofNullable(args.endOverride)
    );
  }

  @Override public Integer call() {
    var input = parseSongInput(this);
    var x = SongMetadataFetcher.fetchMetadata(input);
    if (x == null) {
      System.err.println("Failed to fetch metadata");
      System.exit(1);
    }

    var json = new GsonBuilder()
      .setPrettyPrinting()
      .create()
      .toJson(x.song());
    System.out.println(json);
    return 0;
  }
}
