package moe.kiva;

import kala.control.Option;
import moe.kiva.fetch.Fetcher;
import moe.kiva.fetch.SongInput;

public class Main {
  public static void main(String[] args) {
    var x = Fetcher.fetchMetadata(new SongInput(
      "https://www.youtube.com/watch?v=5DEkEaQ0NFY",
      10010,
      13,
      "Others",
      0.35f,
      false,
      Option.none(),
      Option.none(),
      Option.none()
    ));
    System.out.println("Hello, World!");
  }
}
