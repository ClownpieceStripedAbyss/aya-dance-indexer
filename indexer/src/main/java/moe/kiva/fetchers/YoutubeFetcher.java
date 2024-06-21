package moe.kiva.fetchers;

import moe.kiva.ApiHelper;
import moe.kiva.types.SongInput;
import moe.kiva.types.SongMetadata;
import moe.kiva.types.SongMetadataFetcher;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

public enum YoutubeFetcher implements SongMetadataFetcher {
  INSTANCE;

  @Override public boolean canFetch(@NotNull String url) {
    return url.startsWith("https://youtube.com/watch?v=")
      || url.startsWith("https://www.youtube.com/watch?v=");
  }

  @Override public @NotNull SongMetadata doComputeMetadata(@NotNull SongInput input) {
    var html = ApiHelper.getHtml(input.originalUrl(), false, null);
    var dom = Jsoup.parse(html);

    // <meta name="title" content="Para Para Sakura | VRChat Fitness Dance | Song^_^">
    // <meta itemprop="duration" content="PT3M51S">

    return input.toMetadata(
      () -> dom.select("meta[name=title]").attr("content"),
      () -> {
        var duration = dom.select("meta[itemprop=duration]").attr("content");
        if (duration.isBlank())
          throw new RuntimeException("Cannot find duration for youtube video: " + input.originalUrl());
        var durationInSeconds = 0;
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
        return durationInSeconds;
      }
    );
  }
}
