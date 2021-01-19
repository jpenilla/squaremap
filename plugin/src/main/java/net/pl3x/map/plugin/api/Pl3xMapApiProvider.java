package net.pl3x.map.plugin.api;

import net.pl3x.map.api.MapWorld;
import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Registry;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public final class Pl3xMapApiProvider implements Pl3xMap {

    private final Pl3xMapPlugin plugin;
    private final IconRegistry iconRegistry;

    public Pl3xMapApiProvider(final @NonNull Pl3xMapPlugin plugin) {
        this.plugin = plugin;
        this.iconRegistry = new IconRegistry();
    }

    @Override
    public @NonNull Collection<MapWorld> mapWorlds() {
        return Collections.unmodifiableCollection(this.plugin.worldManager().worlds().values());
    }

    @Override
    public @NonNull Optional<MapWorld> getWorldIfEnabled(@NonNull World world) {
        return this.plugin.worldManager().getWorldIfEnabled(world).map(w -> w);
    }

    @Override
    public @NonNull Registry<BufferedImage> iconRegistry() {
        return this.iconRegistry;
    }

}
