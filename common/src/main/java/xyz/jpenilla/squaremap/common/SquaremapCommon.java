package xyz.jpenilla.squaremap.common;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class SquaremapCommon {
    private static SquaremapCommon INSTANCE;

    private final SquaremapPlatform platform;

    public SquaremapCommon(final SquaremapPlatform platform) {
        INSTANCE = this;
        this.platform = platform;
    }

    public SquaremapPlatform platform() {
        return this.platform;
    }

    public static SquaremapCommon instance() {
        return INSTANCE;
    }
}
