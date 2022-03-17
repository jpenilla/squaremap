package xyz.jpenilla.squaremap.common.data.storage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class AdditionalParameters {

    private final Map<String, Object> parameters = new ConcurrentHashMap<>();

    public AdditionalParameters put(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(parameters.get(key));
    }
}
