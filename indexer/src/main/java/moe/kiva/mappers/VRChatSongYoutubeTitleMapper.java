package moe.kiva.mappers;

import moe.kiva.types.SongMetadata;
import moe.kiva.types.SongMetadataMapper;
import org.jetbrains.annotations.NotNull;

public enum VRChatSongYoutubeTitleMapper implements SongMetadataMapper {
  INSTANCE;

  @Override public boolean shouldWorkOn(@NotNull SongMetadata metadata) {
    return metadata.downloadUrl().contains("youtube.com")
      && metadata.song().title().contains("Song^_^")
      && metadata.song().title().contains("VRChat");
  }

  @Override public @NotNull SongMetadata mapMetadata(@NotNull SongMetadata metadata) {
    var newTitle = "[Song] %s".formatted(metadata.song().title().replace("| Song^_^", "").trim());
    return new SongMetadata(
      metadata.song().withTitle(newTitle),
      metadata.downloadUrl()
    );
  }
}
