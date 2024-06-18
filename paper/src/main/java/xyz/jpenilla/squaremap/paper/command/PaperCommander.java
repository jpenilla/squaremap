package xyz.jpenilla.squaremap.paper.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitHelper;

@DefaultQualifier(NonNull.class)
@SuppressWarnings("UnstableApiUsage")
public class PaperCommander implements Commander, ForwardingAudience.Single {
    private final CommandSourceStack stack;

    private PaperCommander(final io.papermc.paper.command.brigadier.CommandSourceStack stack) {
        this.stack = stack;
    }

    @Override
    public Audience audience() {
        final @Nullable Entity executor = this.stack.getExecutor();
        return executor == null ? this.stack.getSender() : executor;
    }

    @Override
    public boolean hasPermission(final String permission) {
        return this.stack.getSender().hasPermission(permission);
    }

    public CommandSourceStack stack() {
        return this.stack;
    }

    @Override
    public Object commanderId() {
        return this.stack.getSender();
    }

    public static PaperCommander from(final CommandSourceStack stack) {
        if (stack.getSender() instanceof org.bukkit.entity.Player) {
            return new Player(stack);
        }
        return new PaperCommander(stack);
    }

    public static final class Player extends PaperCommander implements PlayerCommander {
        private Player(final CommandSourceStack commandSourceStack) {
            super(commandSourceStack);
        }

        public org.bukkit.entity.Player bukkit() {
            return (org.bukkit.entity.Player) this.stack().getSender();
        }

        @Override
        public ServerPlayer player() {
            return CraftBukkitHelper.serverPlayer(this.bukkit());
        }

        @Override
        public Object commanderId() {
            return this.bukkit().getUniqueId();
        }
    }
}
