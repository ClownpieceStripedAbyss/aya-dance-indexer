package moe.kiva;

import com.google.gson.GsonBuilder;
import kala.control.Option;
import moe.kiva.types.SongInput;
import moe.kiva.types.SongMetadataFetcher;
import org.jetbrains.annotations.NotNull;

public class Main {
  public static void main(String @NotNull [] args) {
    if (args.length != 9 && args.length != 5) {
      System.err.println("Usage: <url> <id> <catId> <catName> <flip> [<volume> <titleOverride> <startOverride> <endOverride>]");
      System.exit(1);
    }

    var input = parseSongInput(args);
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
  }

  private static @NotNull SongInput parseSongInput(String @NotNull [] args) {
    var url = args[0];
    var id = Integer.parseInt(args[1]);
    var catId = Integer.parseInt(args[2]);
    var catName = args[3];
    var flip = Boolean.parseBoolean(args[4]);

    boolean shorterVersion = args.length == 5;

    var volume = shorterVersion ? 0.35f : Float.parseFloat(args[5]);
    var titleOverride = shorterVersion ? Option.<String>none() : args[6].equals("null") ? Option.<String>none() : Option.some(args[6]);
    var startOverride = shorterVersion ? Option.<Integer>none() : args[7].equals("null") ? Option.<Integer>none() : Option.some(Integer.parseInt(args[7]));
    var endOverride = shorterVersion ? Option.<Integer>none() : args[8].equals("null") ? Option.<Integer>none() : Option.some(Integer.parseInt(args[8]));

    return new SongInput(
      url,
      id,
      catId,
      catName,
      volume,
      flip,
      titleOverride,
      startOverride,
      endOverride
    );
  }
}
