package xyz.jpenilla.squaremap.sponge;

import com.google.inject.Inject;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;

@DefaultQualifier(NonNull.class)
public final class SpongePlayerManager extends AbstractPlayerManager {
    public static final Key<Value<Boolean>> HIDDEN_KEY = Key.from(ResourceKey.of("squaremap", "hidden"), Boolean.class);

    @Inject
    private SpongePlayerManager() {
    }

    @Override
    protected boolean persistentHidden(final ServerPlayer player) {
        return ((org.spongepowered.api.entity.living.player.server.ServerPlayer) player).get(HIDDEN_KEY).orElse(false);
    }

    @Override
    protected void persistentHidden(final ServerPlayer player, final boolean value) {
        ((org.spongepowered.api.entity.living.player.server.ServerPlayer) player).offer(HIDDEN_KEY, value);
    }

    @Override
    public @NonNull Component displayName(final @NonNull ServerPlayer player) {
        return ((org.spongepowered.api.entity.living.player.server.ServerPlayer) player).displayName().get();
    }

    @Override
    public @Nullable ServerPlayer player(final @NonNull UUID uuid) {
        return (ServerPlayer) Sponge.server().player(uuid).orElse(null);
    }
}
