package xyz.jpenilla.squaremap.common.command;

import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface PlayerCommander extends Commander {
    @NonNull ServerPlayer player();
}
