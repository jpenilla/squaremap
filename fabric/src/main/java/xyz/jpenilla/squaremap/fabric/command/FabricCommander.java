package xyz.jpenilla.squaremap.fabric.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.fabric.mixin.CommandSourceStackAccess;

@DefaultQualifier(NonNull.class)
public class FabricCommander implements Commander, ForwardingAudience.Single {
    private final CommandSourceStack stack;

    private FabricCommander(final CommandSourceStack stack) {
        this.stack = stack;
    }

    @Override
    public Audience audience() {
        return this.stack;
    }

    @Override
    public boolean hasPermission(final String permission) {
        return Permissions.check(this.stack, permission, this.stack.getServer().getOperatorUserPermissionLevel());
    }

    public CommandSourceStack stack() {
        return this.stack;
    }

    @Override
    public Object commanderId() {
        return ((CommandSourceStackAccess) this.stack).source();
    }

    public static FabricCommander from(final CommandSourceStack stack) {
        if (((CommandSourceStackAccess) stack).source() instanceof ServerPlayer) {
            return new Player(stack);
        }
        return new FabricCommander(stack);
    }

    public static final class Player extends FabricCommander implements PlayerCommander {
        private Player(final CommandSourceStack stack) {
            super(stack);
        }

        @Override
        public ServerPlayer player() {
            try {
                return this.stack().getPlayerOrException();
            } catch (final CommandSyntaxException e) {
                throw Util.rethrow(e);
            }
        }

        @Override
        public Object commanderId() {
            return this.player().getGameProfile().getId();
        }
    }
}
