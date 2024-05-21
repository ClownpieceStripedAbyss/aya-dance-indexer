package moe.kiva;

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum;
import com.github.houbb.pinyin.util.PinyinHelper;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

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
  boolean skipRandom
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
}
