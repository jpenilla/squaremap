package xyz.jpenilla.squaremap.paper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitHelper;

@DefaultQualifier(NonNull.class)
@Singleton
public final class PaperServerAccess implements ServerAccess {
    private final Server server;

    @Inject
    private PaperServerAccess(final Server server) {
        this.server = server;
    }

    @Override
    public @NonNull Collection<ServerLevel> levels() {
        final List<ServerLevel> levels = new ArrayList<>();
        for (final World world : this.server.getWorlds()) {
            levels.add(CraftBukkitHelper.serverLevel(world));
        }
        return levels;
    }

    @Override
    public @Nullable ServerLevel level(final @NonNull WorldIdentifier identifier) {
        final @Nullable World world = this.server.getWorld(BukkitAdapter.namespacedKey(identifier));
        if (world == null) {
            return null;
        }
        return CraftBukkitHelper.serverLevel(world);
    }

    @Override
    public @Nullable ServerPlayer player(final UUID uuid) {
        final @Nullable Player player = this.server.getPlayer(uuid);
        if (player == null) {
            return null;
        }
        return CraftBukkitHelper.serverPlayer(player);
    }

    @Override
    public int maxPlayers() {
        return this.server.getMaxPlayers();
    }

    @Override
    public void blockSleep() {
        // Not needed on Paper, chunks are unloaded during sleep
    }

    @Override
    public void allowSleep() {
        // Not needed on Paper, chunks are unloaded during sleep
    }
}
