package moe.kiva;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiHelper {
  public static @NotNull HttpClient httpClient(@Nullable InetSocketAddress proxy) {
    var builder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2);
    if (proxy != null) builder.proxy(ProxySelector.of(proxy));
    return builder.build();
  }

  public static @NotNull String getHtml(@NotNull String url, @Nullable InetSocketAddress proxy) {
    try (var httpClient = httpClient(proxy)) {
      var request = HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .GET()
        .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return response.body();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
