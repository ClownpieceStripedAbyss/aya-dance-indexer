package moe.kiva;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiHelper {
  public static @NotNull String getHtml(@NotNull String url) {
    try (var httpClient = HttpClient.newHttpClient()) {
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
