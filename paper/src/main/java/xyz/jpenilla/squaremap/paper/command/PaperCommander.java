package xyz.jpenilla.squaremap.paper.command;

import java.util.Objects;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
public class PaperCommander implements Commander, ForwardingAudience.Single {
    private final CommandSender sender;

    private PaperCommander(final CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public Audience audience() {
        return this.sender;
    }

    public CommandSender sender() {
        return this.sender;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final PaperCommander that = (PaperCommander) o;
        return this.sender.equals(that.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.sender);
    }

    public static PaperCommander from(final CommandSender sender) {
        if (sender instanceof org.bukkit.entity.Player player) {
            return new Player(player);
        }
        return new PaperCommander(sender);
    }

    public static final class Player extends PaperCommander implements PlayerCommander {
        private Player(final org.bukkit.entity.Player sender) {
            super(sender);
        }

        public org.bukkit.entity.Player bukkit() {
            return (org.bukkit.entity.Player) this.sender();
        }

        @Override
        public ServerPlayer player() {
            return CraftBukkitReflection.serverPlayer(this.bukkit());
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final PaperCommander.Player that = (PaperCommander.Player) o;
            return this.sender().equals(that.sender());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.sender());
        }
    }
}
