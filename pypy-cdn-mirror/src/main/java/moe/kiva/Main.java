package moe.kiva;

import org.jetbrains.annotations.NotNull;

public class Main {
  public static final @NotNull String DEFAULT_SONG_LIST_URL = "https://docs.google.com/spreadsheets/u/1/d/e/2PACX-1vQAvsUeoYncuBCN3iJs6RpNFONmUvWumoK4SqKWsJ3svLAY_t0cPvneaGrDQwxzGj4k1RaJ-EhkrRFY/pubhtml";

  public static void main(String[] args) {
    var songListUrl = args.length == 1
      ? args[0]
      : DEFAULT_SONG_LIST_URL;
    var songList = PyPySongListParser.parseSongList(songListUrl);
    var downloader = PyPySongDownloader.create(songList, "../template/pypydance-song");
    downloader.downloadAllSingle();
  }
}
