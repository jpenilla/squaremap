package xyz.jpenilla.squaremap.forge.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.forge.ForgeAdventure;

// Commanders are used as Map keys by the CommandConfirmationManager to track who has confirmations pending
// So our equals and hashcode only account for the source, not the entire stack
@DefaultQualifier(NonNull.class)
public class ForgeCommander implements Commander, ForwardingAudience.Single {
    private final CommandSourceStack stack;

    private ForgeCommander(final CommandSourceStack stack) {
        this.stack = stack;
    }

    @Override
    public Audience audience() {
        return ForgeAdventure.commandSourceAudience(this.stack);
    }

    @Override
    public boolean hasPermission(final String permission) {
        return this.stack.hasPermission(this.stack.getServer().getOperatorUserPermissionLevel());
    }

    public CommandSourceStack stack() {
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
        final ForgeCommander that = (ForgeCommander) o;
        return this.stack.source.equals(that.stack.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stack.source);
    }

    public static ForgeCommander from(final CommandSourceStack stack) {
        if (stack.source instanceof ServerPlayer) {
            return new Player(stack);
        }
        return new ForgeCommander(stack);
    }

    public static final class Player extends ForgeCommander implements PlayerCommander {
        private static final Cache<String, PermissionNode<Boolean>> PERMISSION_NODE_CACHE = CacheBuilder.newBuilder().maximumSize(100).build();

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
        public boolean equals(final @Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final Player that = (Player) o;
            return this.player().equals(that.player());
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean hasPermission(final String permission) {
            final PermissionNode<Boolean> node;
            try {
                node = PERMISSION_NODE_CACHE.get(permission, () -> (PermissionNode<Boolean>) PermissionAPI.getRegisteredNodes().stream()
                    .filter(n -> n.getNodeName().equals(permission) && n.getType() == PermissionTypes.BOOLEAN)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find registered node for permission " + permission)));
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }
            return PermissionAPI.getPermission(this.player(), node);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.player());
        }
    }
}
