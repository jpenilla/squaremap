package xyz.jpenilla.squaremap.sponge.command;

import java.util.Objects;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.command.CommandCause;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;

// Commanders are used as Map keys by the CommandConfirmationManager to track who has confirmations pending
// So our equals and hashcode only account for the source, not the entire stack
@DefaultQualifier(NonNull.class)
public class SpongeCommander implements Commander, ForwardingAudience.Single {
    private final CommandCause cause;

    private SpongeCommander(final CommandCause cause) {
        this.cause = cause;
    }

    @Override
    public Audience audience() {
        return this.cause.audience();
    }

    public CommandCause cause() {
        return this.cause;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final SpongeCommander that = (SpongeCommander) o;
        return this.cause.root().equals(that.cause.root());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.cause.root());
    }

    public static SpongeCommander from(final CommandCause cause) {
        if (cause.root() instanceof ServerPlayer) {
            return new Player(cause);
        }
        return new SpongeCommander(cause);
    }

    public static final class Player extends SpongeCommander implements PlayerCommander {
        private Player(final CommandCause stack) {
            super(stack);
        }

        @Override
        public ServerPlayer player() {
            return (ServerPlayer) this.cause().root();
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final SpongeCommander.Player that = (SpongeCommander.Player) o;
            return this.player().equals(that.player());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.player());
        }
    }
}
