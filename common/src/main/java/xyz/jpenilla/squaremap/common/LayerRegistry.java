package xyz.jpenilla.squaremap.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.Pair;
import xyz.jpenilla.squaremap.api.Registry;

@DefaultQualifier(NonNull.class)
public final class LayerRegistry implements Registry<LayerProvider> {
    private final Map<Key, LayerProvider> layerProviders = new ConcurrentHashMap<>();

    @Override
    public void register(final Key key, final LayerProvider value) {
        if (this.hasEntry(key)) {
            throw layerAlreadyRegistered(key);
        }
        this.layerProviders.put(key, value);
    }

    @Override
    public void unregister(final Key key) {
        final LayerProvider removed = this.layerProviders.remove(key);
        if (removed == null) {
            throw noLayerRegistered(key);
        }
    }

    @Override
    public boolean hasEntry(final Key key) {
        return this.layerProviders.containsKey(key);
    }

    @Override
    public LayerProvider get(final Key key) {
        final LayerProvider provider = this.layerProviders.get(key);
        if (provider == null) {
            throw noLayerRegistered(key);
        }
        return provider;
    }

    @Override
    public Iterable<Pair<Key, LayerProvider>> entries() {
        return this.layerProviders.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    private static IllegalArgumentException noLayerRegistered(final Key key) {
        return new IllegalArgumentException(String.format("No LayerProvider registered for key '%s'", key.getKey()));
    }

    private static IllegalArgumentException layerAlreadyRegistered(final Key key) {
        throw new IllegalArgumentException(String.format("LayerProvider already registered for key '%s'", key.getKey()));
    }
}
