package xyz.jpenilla.squaremap.paper;

import com.google.inject.Inject;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
public final class PaperPlayerManager extends AbstractPlayerManager {
    public static final NamespacedKey HIDDEN_KEY = new NamespacedKey(SquaremapPaper.instance(), "hidden");

    private static PersistentDataContainer pdc(final ServerPlayer player) {
        return CraftBukkitReflection.player(player).getPersistentDataContainer();
    }

    @Inject
    private PaperPlayerManager() {
    }

    @Override
    protected boolean persistentHidden(final ServerPlayer player) {
        return pdc(player).getOrDefault(HIDDEN_KEY, PersistentDataType.BYTE, (byte) 0) != (byte) 0;
    }

    @Override
    protected void persistentHidden(final ServerPlayer player, final boolean value) {
        pdc(player).set(HIDDEN_KEY, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
    }

    @Override
    public boolean otherwiseHidden(final @NonNull ServerPlayer player) {
        return CraftBukkitReflection.player(player).hasMetadata("NPC");
    }

    @Override
    public @NonNull Component displayName(final @NonNull ServerPlayer player) {
        return CraftBukkitReflection.player(player).displayName();
    }

    @Override
    public @Nullable ServerPlayer player(final @NonNull UUID uuid) {
        final @Nullable Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return null;
        }
        return CraftBukkitReflection.serverPlayer(player);
    }
}
