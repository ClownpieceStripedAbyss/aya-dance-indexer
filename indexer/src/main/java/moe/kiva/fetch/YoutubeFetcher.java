package moe.kiva.fetch;

import moe.kiva.ApiHelper;
import moe.kiva.Song;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.util.List;

public enum YoutubeFetcher implements Fetcher {
  INSTANCE;

  @Override public boolean canFetch(@NotNull String url) {
    return url.startsWith("https://youtube.com/watch?v=")
      || url.startsWith("https://www.youtube.com/watch?v=");
  }

  @Override public @NotNull SongMetadata doComputeMetadata(@NotNull SongInput input) {
    var html = ApiHelper.getHtml(input.originalUrl());
    var x = Jsoup.parse(html);

    // <meta name="title" content="Para Para Sakura | VRChat Fitness Dance | Song^_^">
    // <meta itemprop="duration" content="PT3M51S">
    var title = input.titleOverride().getOrElse(
      () -> x.select("meta[name=title]").attr("content"));
    var spell = Song.spell(title);

    var duration = x.select("meta[itemprop=duration]").attr("content");
    var durationInSeconds = 0;
    if (!duration.isEmpty()) {
      var parts = duration.split("PT");
      if (parts.length == 2) {
        var time = parts[1];
        var timeParts = time.split("M");
        if (timeParts.length == 2) {
          var minutes = Integer.parseInt(timeParts[0]);
          var seconds = timeParts[1].isEmpty() ? 0 : Integer.parseInt(timeParts[1].substring(0, timeParts[1].length() - 1));
          durationInSeconds = minutes * 60 + seconds;
        }
      }
    }

    return new SongMetadata(
      new Song(
        input.id(),
        input.categoryId(),
        title,
        input.categoryName(),
        null,
        null,
        spell,
        0,
        input.volume(),
        input.startOverride().getOrDefault(0),
        input.endOverride().getOrDefault(durationInSeconds),
        input.flip(),
        false,
        List.of(input.originalUrl()),
        "checksum"
      ),
      input.originalUrl()
    );
  }
}
