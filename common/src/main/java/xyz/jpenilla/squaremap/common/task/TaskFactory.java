package xyz.jpenilla.squaremap.common.task;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;

@DefaultQualifier(NonNull.class)
public interface TaskFactory {
    UpdateMarkers createUpdateMarkers(MapWorldInternal mapWorld);
}
