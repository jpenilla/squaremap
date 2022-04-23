package xyz.jpenilla.squaremap.common.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
@Singleton
public final class ConfigManager {
    private final DirectoryProvider directoryProvider;
    private final WorldConfigContainer<WorldConfig, Config> worldConfigContainer;
    private final WorldConfigContainer<WorldAdvanced, Advanced> worldAdvancedContainer;

    @Inject
    private ConfigManager(
        final DirectoryProvider directoryProvider,
        final ServerAccess serverAccess
    ) {
        this.directoryProvider = directoryProvider;
        this.worldConfigContainer = new WorldConfigContainer<>(WorldConfig::new, Config::config, serverAccess);
        this.worldAdvancedContainer = new WorldConfigContainer<>(WorldAdvanced::new, Advanced::config, serverAccess);
    }

    public void init() {
        this.reload();
    }

    public void reload() {
        Config.reload(this.directoryProvider);
        this.worldConfigContainer.reload();

        Advanced.reload(this.directoryProvider);
        this.worldAdvancedContainer.reload();

        Messages.reload(this.directoryProvider);
    }

    public WorldConfig worldConfig(final ServerLevel level) {
        return this.worldConfigContainer.config(level);
    }

    public WorldAdvanced worldAdvanced(final ServerLevel level) {
        return this.worldAdvancedContainer.config(level);
    }

    private static final class WorldConfigContainer<W extends AbstractWorldConfig<P>, P extends AbstractConfig> {
        private final Map<WorldIdentifier, W> configs = new ConcurrentHashMap<>();
        private final BiFunction<P, ServerLevel, W> worldConfigFactory;
        private final Supplier<P> parentConfigSupplier;
        private final ServerAccess serverAccess;

        WorldConfigContainer(
            final BiFunction<P, ServerLevel, W> worldConfigFactory,
            final Supplier<P> parentConfigSupplier,
            final ServerAccess serverAccess
        ) {
            this.worldConfigFactory = worldConfigFactory;
            this.parentConfigSupplier = parentConfigSupplier;
            this.serverAccess = serverAccess;
        }

        void reload() {
            this.configs.clear();
            for (final ServerLevel level : this.serverAccess.levels()) {
                this.configs.put(Util.worldIdentifier(level), this.create(level));
            }
        }

        W config(final ServerLevel level) {
            return this.configs.computeIfAbsent(Util.worldIdentifier(level), $ -> this.create(level));
        }

        W create(final ServerLevel level) {
            return this.worldConfigFactory.apply(this.parentConfigSupplier.get(), level);
        }
    }
}
