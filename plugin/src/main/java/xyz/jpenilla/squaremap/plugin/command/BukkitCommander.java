package xyz.jpenilla.squaremap.plugin.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;

public class BukkitCommander implements Commander, ForwardingAudience.Single {
    private final CommandSender sender;

    public BukkitCommander(final @NonNull CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public @NonNull Audience audience() {
        return this.sender;
    }

    public @NonNull CommandSender sender() {
        return this.sender;
    }

    public static final class Player extends BukkitCommander implements PlayerCommander {
        public Player(final org.bukkit.entity.@NonNull Player sender) {
            super(sender);
        }

        public org.bukkit.entity.@NonNull Player bukkit() {
            return (org.bukkit.entity.Player) this.sender();
        }

        @Override
        public @NonNull ServerPlayer player() {
            return ((CraftPlayer) this.bukkit()).getHandle();
        }
    }
}
