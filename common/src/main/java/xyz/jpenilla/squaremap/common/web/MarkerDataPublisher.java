package xyz.jpenilla.squaremap.common.web;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.util.Json;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

public final class MarkerDataPublisher implements Runnable {
    public interface Factory {
        MarkerDataPublisher create(@NonNull MapWorldInternal mapWorld);
    }

    private final MapWorldInternal mapWorld;
    private final String jsonPathString;
    private final WebJsonStore jsonStore;
    private final Object2LongMap<Key> lastUpdatedTime = new Object2LongOpenHashMap<>();
    private final Map<Key, Map<String, Object>> layerCache = new HashMap<>();
    private final Map<Key, Map<String, Object>> serializedLayerCache = new HashMap<>();
    private long lastResetTime = Long.MIN_VALUE; // min value to ensure initial write even with no layers

    @AssistedInject
    private MarkerDataPublisher(
        @Assisted final @NonNull MapWorldInternal mapWorld,
        final @NonNull SquaremapDirectories directories,
        final @NonNull WebJsonStore jsonStore
    ) {
        this.mapWorld = mapWorld;
        final Path jsonPath = this.mapWorld.tilesPath().resolve("markers.json");
        this.jsonPathString = "/" + directories.webDirectory().relativize(jsonPath).toString().replace("\\", "/");
        this.jsonStore = jsonStore;
    }

    @Override
    public void run() {
        final Registry<LayerProvider> layerRegistry = this.mapWorld.layerRegistry();

        final List<Map<String, Object>> layers = new ArrayList<>();
        final Set<Key> layerKeys = new HashSet<>();
        boolean[] changed = {false};
        layerRegistry.entries().forEach(registeredLayer -> {
            final LayerProvider provider = registeredLayer.right();
            final Key key = registeredLayer.left();
            layerKeys.add(key);
            final List<Marker> markers = List.copyOf(provider.getMarkers());

            final Map<String, Object> current = this.createMap(key, provider);
            current.put("markers", markers.hashCode());

            final Map<String, Object> previous = this.layerCache.get(key);

            if (previous == null || !previous.equals(current)) {
                changed[0] = true; // new or changed layer
                this.layerCache.put(key, current);

                final Map<String, Object> serializedLayer = this.serializeLayer(key, provider, markers);
                this.serializedLayerCache.put(key, serializedLayer);

                final long time = System.currentTimeMillis();
                this.lastUpdatedTime.put(key, time);

                final Map<String, Object> timeStampedLayer = new HashMap<>(serializedLayer);
                timeStampedLayer.put("timestamp", time);
                layers.add(timeStampedLayer);
            } else {
                final Map<String, Object> serializedLayer = this.serializedLayerCache.get(key);
                final long lastUpdate = this.lastUpdatedTime.getLong(key);

                final Map<String, Object> timeStampedLayer = new HashMap<>(serializedLayer);
                timeStampedLayer.put("timestamp", lastUpdate);
                layers.add(timeStampedLayer);
            }
        });

        // set flag to ensure update on removed layers
        changed[0] |= clearUnused(layerKeys, this.layerCache);
        changed[0] |= clearUnused(layerKeys, this.serializedLayerCache);
        changed[0] |= clearUnused(layerKeys, this.lastUpdatedTime);

        if (changed[0] || this.mapWorld.lastReset() != this.lastResetTime) {
            this.lastResetTime = this.mapWorld.lastReset();
            ForkJoinPool.commonPool().execute(() -> this.jsonStore.put(this.jsonPathString, Json.gson().toJson(layers)));
        }
    }

    @SuppressWarnings("rawtypes")
    private static <K> boolean clearUnused(final Set<K> keepKeys, final Map<K, ?> map) {
        boolean madeChange = false;
        for (final K key : Set.copyOf(map.keySet())) {
            if (!keepKeys.contains(key)) {
                madeChange = true;
                if (map instanceof Object2LongMap longMap) {
                    longMap.removeLong(key);
                } else {
                    map.remove(key);
                }
            }
        }
        return madeChange;
    }

    private @NonNull Map<String, Object> serializeLayer(final @NonNull Key key, final @NonNull LayerProvider provider, final @NonNull List<Marker> markers) {
        final Map<String, Object> map = this.createMap(key, provider);
        map.put("markers", MarkerJsonSerializer.serialize(markers));
        return map;
    }

    private Map<String, Object> createMap(Key key, LayerProvider provider) {
        final Map<String, Object> map = new HashMap<>();
        map.put("id", key.getKey());
        map.put("name", provider.getLabel());
        map.put("control", provider.showControls());
        map.put("hide", provider.defaultHidden());
        map.put("order", provider.layerPriority());
        map.put("z_index", provider.zIndex());
        return map;
    }

}
