package xyz.jpenilla.squaremap.forge.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.forge.ForgeAdventure;

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
    public Object commanderId() {
        return this.stack.source;
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
            return (ServerPlayer) this.stack().source;
        }

        @Override
        public Object commanderId() {
            return this.player().getGameProfile().getId();
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
    }
}
