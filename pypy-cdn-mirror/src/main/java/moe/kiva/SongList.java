package moe.kiva;

import org.jetbrains.annotations.NotNull;

import java.util.List;

record SongList(
  @NotNull String title,
  @NotNull List<Song> entries
) {
}
