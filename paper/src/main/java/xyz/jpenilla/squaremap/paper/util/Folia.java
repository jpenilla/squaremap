package xyz.jpenilla.squaremap.paper.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;

public final class Folia {
    public static final boolean FOLIA;

    static {
        final @Nullable Class<?> regionizedServerCls = ReflectionUtil.findClass("io.papermc.paper.threadedregions.RegionizedServer");
        FOLIA = regionizedServerCls != null;
    }

    private Folia() {
    }
}
