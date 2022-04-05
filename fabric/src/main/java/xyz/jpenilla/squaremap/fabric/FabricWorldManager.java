package xyz.jpenilla.squaremap.fabric;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractWorldManager;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.fabric.data.FabricMapWorld;

@DefaultQualifier(NonNull.class)
@Singleton
public final class FabricWorldManager extends AbstractWorldManager {
    @Inject
    private FabricWorldManager(
        final FabricMapWorld.Factory factory,
        final ServerAccess serverAccess,
        final ConfigManager configManager
    ) {
        super(factory, serverAccess, configManager);
    }
}
