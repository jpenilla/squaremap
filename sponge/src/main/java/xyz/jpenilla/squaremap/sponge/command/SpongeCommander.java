package xyz.jpenilla.squaremap.sponge.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.command.CommandCause;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;

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

    @Override
    public boolean hasPermission(final String permission) {
        return this.cause.hasPermission(permission);
    }

    public CommandCause cause() {
        return this.cause;
    }

    @Override
    public Object commanderId() {
        return this.cause.root();
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
        public Object commanderId() {
            return this.player().getGameProfile().id();
        }
    }
}
