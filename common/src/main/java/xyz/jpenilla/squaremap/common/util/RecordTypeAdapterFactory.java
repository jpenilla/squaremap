package xyz.jpenilla.squaremap.common.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gson support for Java 16+ record types.
 * Taken from https://github.com/google/gson/issues/1794 and adjusted for performance and proper handling of
 * {@link SerializedName} annotations
 */
public class RecordTypeAdapterFactory implements TypeAdapterFactory {

    private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = new HashMap<>();

    static {
        PRIMITIVE_DEFAULTS.put(byte.class, (byte) 0);
        PRIMITIVE_DEFAULTS.put(int.class, 0);
        PRIMITIVE_DEFAULTS.put(long.class, 0L);
        PRIMITIVE_DEFAULTS.put(short.class, (short) 0);
        PRIMITIVE_DEFAULTS.put(double.class, 0D);
        PRIMITIVE_DEFAULTS.put(float.class, 0F);
        PRIMITIVE_DEFAULTS.put(char.class, '\0');
        PRIMITIVE_DEFAULTS.put(boolean.class, false);
    }

    private final Map<RecordComponent, List<String>> recordComponentNameCache = new ConcurrentHashMap<>();

    /**
     * Get all names of a record component
     * If annotated with {@link SerializedName} the list returned will be the primary name first, then any alternative names
     * Otherwise, the component name will be returned.
     */
    private List<String> getRecordComponentNames(final RecordComponent recordComponent) {
        List<String> inCache = this.recordComponentNameCache.get(recordComponent);
        if (inCache != null) {
            return inCache;
        }
        List<String> names = new ArrayList<>();
        // The @SerializedName is compiled to be part of the componentName() method
        // The use of a loop is also deliberate, getAnnotation seemed to return null if Gson's package was relocated
        SerializedName annotation = null;
        for (Annotation a : recordComponent.getAccessor().getAnnotations()) {
            if (a.annotationType() == SerializedName.class) {
                annotation = (SerializedName) a;
                break;
            }
        }

        if (annotation != null) {
            names.add(annotation.value());
            names.addAll(Arrays.asList(annotation.alternate()));
        } else {
            names.add(recordComponent.getName());
        }
        var namesList = List.copyOf(names);
        this.recordComponentNameCache.put(recordComponent, namesList);
        return namesList;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) type.getRawType();
        if (!clazz.isRecord()) {
            return null;
        }
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader reader) throws IOException {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return null;
                }
                var recordComponents = clazz.getRecordComponents();
                var typeMap = new HashMap<String, TypeToken<?>>();
                for (RecordComponent recordComponent : recordComponents) {
                    for (String name : getRecordComponentNames(recordComponent)) {
                        typeMap.put(name, TypeToken.get(recordComponent.getGenericType()));
                    }
                }
                var argsMap = new HashMap<String, Object>();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    var type = typeMap.get(name);
                    if (type != null) {
                        argsMap.put(name, gson.getAdapter(type).read(reader));
                    } else {
                        gson.getAdapter(Object.class).read(reader);
                    }

                }
                reader.endObject();

                var argTypes = new Class<?>[recordComponents.length];
                var args = new Object[recordComponents.length];
                for (int i = 0; i < recordComponents.length; i++) {
                    argTypes[i] = recordComponents[i].getType();
                    List<String> names = getRecordComponentNames(recordComponents[i]);
                    Object value = null;
                    TypeToken<?> type = null;
                    // Find the first matching type and value
                    for (String name : names) {
                        value = argsMap.get(name);
                        type = typeMap.get(name);
                        if (value != null && type != null) {
                            break;
                        }
                    }

                    if (value == null && (type != null && type.getRawType().isPrimitive())) {
                        value = PRIMITIVE_DEFAULTS.get(type.getRawType());
                    }
                    args[i] = value;
                }
                Constructor<T> constructor;
                try {
                    constructor = clazz.getDeclaredConstructor(argTypes);
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                } catch (NoSuchMethodException | InstantiationException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
