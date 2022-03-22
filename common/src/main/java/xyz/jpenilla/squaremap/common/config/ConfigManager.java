package xyz.jpenilla.squaremap.common.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
@Singleton
public final class ConfigManager {
    private final DirectoryProvider directoryProvider;
    private final ServerAccess serverAccess;
    private final SquaremapPlatform platform;

    @Inject
    private ConfigManager(
        final DirectoryProvider directoryProvider,
        final ServerAccess serverAccess,
        final SquaremapPlatform platform
    ) {
        this.directoryProvider = directoryProvider;
        this.serverAccess = serverAccess;
        this.platform = platform;
    }

    public void init() {
        Config.reload(this.directoryProvider, this.serverAccess, this.platform);

        // this has to load after config.yml in order to know if web dir should be overwritten
        // but also before advanced.yml to ensure foliage.png and grass.png are already on disk
        FileUtil.extract("/web/", this.directoryProvider.webDirectory().toFile(), Config.UPDATE_WEB_DIR);
        FileUtil.extract("/locale/", this.directoryProvider.localeDirectory().toFile(), false);

        Advanced.reload(this.directoryProvider, this.serverAccess, this.platform);
        Lang.reload(this.directoryProvider);
    }

    public void reload() {
        Config.reload(this.directoryProvider, this.serverAccess, this.platform);
        Advanced.reload(this.directoryProvider, this.serverAccess, this.platform);
        Lang.reload(this.directoryProvider);
    }
}
