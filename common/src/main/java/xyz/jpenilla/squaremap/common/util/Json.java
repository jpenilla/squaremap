package xyz.jpenilla.squaremap.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class Json {
    private static final Gson GSON = new GsonBuilder().create();

    private Json() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static void writeAsync(final Path file, final Object object) {
        FileUtil.atomicWriteAsync(file, tmp -> {
            try (final BufferedWriter writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(object, writer);
            }
        });
    }
}
