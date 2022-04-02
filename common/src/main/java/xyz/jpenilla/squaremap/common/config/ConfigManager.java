package xyz.jpenilla.squaremap.common.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;

@DefaultQualifier(NonNull.class)
@Singleton
public final class ConfigManager {
    private final DirectoryProvider directoryProvider;
    private final ServerAccess serverAccess;

    @Inject
    private ConfigManager(
        final DirectoryProvider directoryProvider,
        final ServerAccess serverAccess
    ) {
        this.directoryProvider = directoryProvider;
        this.serverAccess = serverAccess;
    }

    public void init() {
        this.reload();
    }

    public void reload() {
        Config.reload(this.directoryProvider, this.serverAccess);
        Advanced.reload(this.directoryProvider, this.serverAccess);
        Lang.reload(this.directoryProvider);
    }
}
