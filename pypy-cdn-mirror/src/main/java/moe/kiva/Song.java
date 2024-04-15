package moe.kiva;

import org.jetbrains.annotations.NotNull;

public record Song(int id, int category, @NotNull String name) {
}
