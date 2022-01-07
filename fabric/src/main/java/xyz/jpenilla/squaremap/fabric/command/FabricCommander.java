package xyz.jpenilla.squaremap.fabric.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.fabric.mixin.CommandSourceStackAccess;

// Commanders are used as Map keys by the CommandConfirmationManager to track who has confirmations pending
// So our equals and hashcode only account for the source, not the entire stack
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

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final FabricCommander that = (FabricCommander) o;
        return ((CommandSourceStackAccess) this.stack).source().equals(((CommandSourceStackAccess) that.stack).source());
    }

    @Override
    public int hashCode() {
        return Objects.hash(((CommandSourceStackAccess) this.stack).source());
    }

    public static FabricCommander from(final CommandSourceStack stack) {
        if (((CommandSourceStackAccess) stack).source() instanceof ServerPlayer) {
            return new Player(stack);
        }
        return new FabricCommander(stack);
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

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final FabricCommander.Player that = (FabricCommander.Player) o;
            return this.player().equals(that.player());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.player());
        }
    }
}
