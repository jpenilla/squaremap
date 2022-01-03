package xyz.jpenilla.squaremap.fabric.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.util.Util;

public class FabricCommander implements Commander, ForwardingAudience.Single {
    private final CommandSourceStack stack;

    public FabricCommander(final @NonNull CommandSourceStack stack) {
        this.stack = stack;
    }

    @Override
    public @NonNull Audience audience() {
        return FabricServerAudiences.of(this.stack.getServer()).audience(this.stack);
    }

    public @NonNull CommandSourceStack stack() {
        return this.stack;
    }

    public static final class Player extends FabricCommander implements PlayerCommander {
        public Player(final @NonNull CommandSourceStack stack) {
            super(stack);
        }

        @Override
        public @NonNull ServerPlayer player() {
            try {
                return this.stack().getPlayerOrException();
            } catch (final CommandSyntaxException e) {
                throw Util.rethrow(e);
            }
        }
    }
}
