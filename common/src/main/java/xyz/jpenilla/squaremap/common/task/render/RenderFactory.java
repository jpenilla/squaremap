package xyz.jpenilla.squaremap.common.task.render;

import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;

@DefaultQualifier(NonNull.class)
public interface RenderFactory {
    FullRender createFullRender(MapWorldInternal mapWorld, int wait);

    FullRender createFullRender(MapWorldInternal mapWorld);

    BackgroundRender createBackgroundRender(MapWorldInternal mapWorld);

    RadiusRender createRadiusRender(MapWorldInternal mapWorld, BlockPos center, int radius);
}
