package moe.kiva.fetchers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kala.control.Option;
import moe.kiva.ApiHelper;
import moe.kiva.types.SongInput;
import moe.kiva.types.SongMetadata;
import moe.kiva.types.SongMetadataFetcher;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public enum BiliBiliFetcher implements SongMetadataFetcher {
  INSTANCE;


  @Override
  public boolean canFetch(@NotNull String url) {
    return url.startsWith("https://www.bilibili.com/video/");
  }

  @Override public @NotNull SongMetadata doComputeMetadata(@NotNull SongInput input) throws Exception {
    var html = ApiHelper.getGzippedHtml(input.originalUrl(), true, null);
    var dom = Jsoup.parse(html);

    var initialStateJs = dom.select("script")
      .stream()
      .filter(n -> n.html().contains("window.__INITIAL_STATE__") && n.html().contains("title") && n.html().contains("duration"))
      .findFirst()
      .map(Element::html)
      .orElseThrow(() -> new RuntimeException("Cannot find duration for bilibili video: " + input.originalUrl()));
    var initialStateJson = initialStateJs.substring(initialStateJs.indexOf("{"), initialStateJs.lastIndexOf("};") + 1);
    var initialState = new Gson().fromJson(initialStateJson, JsonObject.class);

    return input.toMetadata(
      () -> Option.ofNullable(initialState)
        .mapNotNull(x -> x.getAsJsonObject("videoData"))
        .mapNotNull(x -> x.get("title"))
        .mapNotNull(JsonElement::getAsString)
        .getOrThrow(() -> new RuntimeException("Cannot find title from window.__INITIAL_STATE__ for bilibili video: " + input.originalUrl())),
      () -> Option.ofNullable(initialState)
        .mapNotNull(x -> x.getAsJsonObject("videoData"))
        .mapNotNull(x -> x.get("duration"))
        .mapNotNull(JsonElement::getAsInt)
        .getOrThrow(() -> new RuntimeException("Cannot find duration from window.__INITIAL_STATE__ for bilibili video: " + input.originalUrl()))
    );
  }
}
