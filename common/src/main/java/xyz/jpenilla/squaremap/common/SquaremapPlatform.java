package xyz.jpenilla.squaremap.common;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface SquaremapPlatform {
    void startCallback();

    void stopCallback();

    WorldManager worldManager();

    AbstractPlayerManager playerManager();

    ServerAccess serverAccess();

    String version();
}
