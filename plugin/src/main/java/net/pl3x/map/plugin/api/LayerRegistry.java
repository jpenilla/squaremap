package net.pl3x.map.plugin.api;

import net.pl3x.map.api.Key;
import net.pl3x.map.api.LayerProvider;
import net.pl3x.map.api.Pair;
import net.pl3x.map.api.Registry;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class LayerRegistry implements Registry<LayerProvider> {

    private final Map<Key, LayerProvider> layerProviders = new ConcurrentHashMap<>();

    @Override
    public void register(@NonNull Key key, @NonNull LayerProvider value) {
        if (this.hasEntry(key)) {
            throw layerAlreadyRegistered(key);
        }
        this.layerProviders.put(key, value);
    }

    @Override
    public void unregister(@NonNull Key key) {
        final LayerProvider removed = this.layerProviders.remove(key);
        if (removed == null) {
            throw noLayerRegistered(key);
        }
    }

    @Override
    public boolean hasEntry(@NonNull Key key) {
        return this.layerProviders.containsKey(key);
    }

    @Override
    public @NonNull LayerProvider get(@NonNull Key key) {
        final LayerProvider provider = this.layerProviders.get(key);
        if (provider == null) {
            throw noLayerRegistered(key);
        }
        return provider;
    }

    @Override
    public @NonNull Iterable<Pair<Key, LayerProvider>> entries() {
        return this.layerProviders.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toUnmodifiableList());
    }

    private static @NonNull IllegalArgumentException noLayerRegistered(final @NonNull Key key) {
        return new IllegalArgumentException(String.format("No LayerProvider registered for key '%s'", key.getKey()));
    }

    private static @NonNull IllegalArgumentException layerAlreadyRegistered(final @NonNull Key key) {
        throw new IllegalArgumentException(String.format("LayerProvider already registered for key '%s'", key.getKey()));
    }

}
