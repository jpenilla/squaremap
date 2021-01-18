package net.pl3x.map.plugin.api;

import net.pl3x.map.api.LayerProvider;
import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Registry;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class Pl3xMapApiProvider implements Pl3xMap {

    private final Pl3xMapPlugin plugin;
    private final LayerRegistry layerRegistry = new LayerRegistry();

    public Pl3xMapApiProvider(final @NonNull Pl3xMapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NonNull Registry<LayerProvider> layerRegistry() {
        return this.layerRegistry;
    }
}
