package xyz.jpenilla.squaremap.forge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractWorldManager;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.forge.data.ForgeMapWorld;

@DefaultQualifier(NonNull.class)
@Singleton
public final class ForgeWorldManager extends AbstractWorldManager {
    @Inject
    private ForgeWorldManager(
        final ForgeMapWorld.Factory factory,
        final ServerAccess serverAccess,
        final ConfigManager configManager
    ) {
        super(factory, serverAccess, configManager);
    }
}
