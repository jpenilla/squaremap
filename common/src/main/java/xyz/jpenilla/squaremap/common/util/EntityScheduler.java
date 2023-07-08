package xyz.jpenilla.squaremap.common.util;

import com.google.inject.Inject;
import net.minecraft.world.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;

@DefaultQualifier(NonNull.class)
public interface EntityScheduler {
    void scheduleFor(Entity entity, Runnable task);

    void scheduleFor(Commander commander, Runnable task);

    final class NoneEntityScheduler implements EntityScheduler {
        @Inject
        private NoneEntityScheduler() {
        }

        @Override
        public void scheduleFor(final Entity entity, final Runnable task) {
            task.run();
        }

        @Override
        public void scheduleFor(final Commander commander, final Runnable task) {
            task.run();
        }
    }
}
