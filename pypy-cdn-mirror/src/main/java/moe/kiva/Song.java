package moe.kiva;

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum;
import com.github.houbb.pinyin.util.PinyinHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public record Song(
  int id,
  int category,
  @NotNull String title,
  @NotNull String categoryName,
  @NotNull String url,
  @NotNull String urlForQuest,
  @NotNull String titleSpell,
  int playerIndex,
  float volume,
  int start,
  int end,
  boolean flip,
  boolean skipRandom,
  @Nullable List<String> originalUrl,
  @Nullable String checksum
) {
  public static @NotNull String spell(@NotNull String name) {
    try {
      var s = PinyinHelper.toPinyin(name, PinyinStyleEnum.FIRST_LETTER);
      var bytes = s.getBytes(StandardCharsets.UTF_8);
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (Throwable t) {
      return name;
    }
  }

  public @NotNull Song withChecksumFromFile(@NotNull Path file) {
    // compute MD5 checksum with file content
    try {
      var messageDigest = MessageDigest.getInstance("MD5");
      messageDigest.update(Files.readAllBytes(file));
      var checksum = HexFormat.of().formatHex(messageDigest.digest());
      return withChecksum(checksum);
    } catch (NoSuchAlgorithmException ignored) {
      // should not happen
      return this;
    } catch (IOException e) {
      System.out.println("WARN: withChecksumFromFile IOE: " + file + ": " + e.getMessage());
      return this;
    }
  }

  public @NotNull Song withChecksum(@NotNull String checksum) {
    return new Song(
      id,
      category,
      title,
      categoryName,
      url,
      urlForQuest,
      titleSpell,
      playerIndex,
      volume,
      start,
      end,
      flip,
      skipRandom,
      originalUrl,
      checksum
    );
  }
}
