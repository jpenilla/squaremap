package xyz.jpenilla.squaremap.common.inject.module;

import com.google.inject.AbstractModule;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.WorldManagerImpl;
import xyz.jpenilla.squaremap.common.util.EntityScheduler;
import xyz.jpenilla.squaremap.common.util.SquaremapJarAccess;

@DefaultQualifier(NonNull.class)
public final class PlatformModule extends AbstractModule {
    private final @Nullable SquaremapPlatform platform;
    private final @Nullable Class<? extends SquaremapPlatform> platformClass;
    private final Class<? extends SquaremapJarAccess> jarAccess;
    private final Class<? extends EntityScheduler> entitySchedulerClass;
    private final Class<? extends WorldManagerImpl> worldManagerClass;

    public PlatformModule(
        final @Nullable SquaremapPlatform platform,
        final @Nullable Class<? extends SquaremapPlatform> platformClass,
        final Class<? extends SquaremapJarAccess> jarAccess,
        final Class<? extends EntityScheduler> entitySchedulerClass,
        final Class<? extends WorldManagerImpl> worldManagerClass
    ) {
        this.platform = platform;
        this.platformClass = platformClass;
        this.jarAccess = jarAccess;
        this.entitySchedulerClass = entitySchedulerClass;
        this.worldManagerClass = worldManagerClass;
    }

    @Override
    protected void configure() {
        if (this.platformClass != null) {
            this.bind(SquaremapPlatform.class).to(this.platformClass);
        } else if (this.platform != null) {
            this.bind(SquaremapPlatform.class).toInstance(this.platform);
        } else {
            throw new IllegalArgumentException();
        }

        this.bind(SquaremapJarAccess.class)
            .to(this.jarAccess);

        this.bind(EntityScheduler.class)
            .to(this.entitySchedulerClass);

        this.bind(WorldManager.class)
            .to(this.worldManagerClass);

        if (!this.worldManagerClass.equals(WorldManagerImpl.class)) {
            this.bind(WorldManagerImpl.class)
                .to(this.worldManagerClass);
        }
    }
}
