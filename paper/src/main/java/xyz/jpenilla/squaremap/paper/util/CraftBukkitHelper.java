package xyz.jpenilla.squaremap.paper.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class CraftBukkitHelper {
    private CraftBukkitHelper() {
    }

    public static @NonNull ServerLevel serverLevel(final @NonNull World world) {
        return ((CraftWorld) world).getHandle();
    }

    public static @NonNull ServerPlayer serverPlayer(final @NonNull Player player) {
        return ((CraftPlayer) player).getHandle();
    }
}
