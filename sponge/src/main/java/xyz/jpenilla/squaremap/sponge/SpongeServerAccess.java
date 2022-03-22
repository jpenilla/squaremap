package xyz.jpenilla.squaremap.sponge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.ServerAccess;

@DefaultQualifier(NonNull.class)
@Singleton
public final class SpongeServerAccess implements ServerAccess {
    private final Game game;

    @Inject
    private SpongeServerAccess(final Game game) {
        this.game = game;
    }

    @Override
    public Collection<ServerLevel> levels() {
        if (!this.game.isServerAvailable()) {
            return List.of();
        }
        return this.game.server().worldManager().worlds().stream()
            .map(level -> (ServerLevel) level)
            .toList();
    }

    @Override
    public @Nullable ServerLevel level(final WorldIdentifier identifier) {
        return (ServerLevel) this.game.server().worldManager()
            .world(ResourceKey.of(identifier.namespace(), identifier.value()))
            .orElse(null);
    }

    @Override
    public int maxPlayers() {
        return this.game.server().maxPlayers();
    }
}
