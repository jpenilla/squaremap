package xyz.jpenilla.squaremap.sponge;

import com.google.inject.Inject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractWorldManager;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.sponge.data.SpongeMapWorld;

@DefaultQualifier(NonNull.class)
public final class SpongeWorldManager extends AbstractWorldManager<SpongeMapWorld> {
    @Inject
    private SpongeWorldManager(
        final SpongeMapWorld.Factory factory,
        final ServerAccess serverAccess
    ) {
        super(factory, serverAccess);
    }
}
