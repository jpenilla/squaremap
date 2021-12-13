package net.pl3x.map.plugin.api;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.Squaremap;

public final class SquaremapApiProvider implements Squaremap {

    private final Pl3xMapPlugin plugin;
    private final IconRegistry iconRegistry;

    public SquaremapApiProvider(final @NonNull Pl3xMapPlugin plugin) {
        this.plugin = plugin;
        this.iconRegistry = new IconRegistry();
    }

    @Override
    public @NonNull Collection<MapWorld> mapWorlds() {
        return Collections.unmodifiableCollection(this.plugin.worldManager().worlds().values());
    }

    @Override
    public @NonNull Optional<MapWorld> getWorldIfEnabled(final @NonNull World world) {
        return this.plugin.worldManager().getWorldIfEnabled(world).map(Function.identity());
    }

    @Override
    public @NonNull Optional<MapWorld> getWorldIfEnabled(final @NonNull UUID uuid) {
        final World world = Bukkit.getWorld(uuid);
        if (world == null) {
            return Optional.empty();
        }
        return this.getWorldIfEnabled(world);
    }

    @Override
    public @NonNull Registry<BufferedImage> iconRegistry() {
        return this.iconRegistry;
    }

    @Override
    public @NonNull PlayerManager playerManager() {
        return this.plugin.playerManager();
    }

    @Override
    public @NonNull Path webDir() {
        return FileUtil.WEB_DIR;
    }

}
