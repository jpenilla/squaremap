package xyz.jpenilla.squaremap.paper.util;

import com.google.inject.Inject;
import net.minecraft.world.entity.Entity;
import org.bukkit.Server;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.util.EntityScheduler;
import xyz.jpenilla.squaremap.paper.command.PaperCommander;

@DefaultQualifier(NonNull.class)
public final class PaperEntityScheduler implements EntityScheduler {
    private final Server server;
    private final JavaPlugin plugin;

    @Inject
    private PaperEntityScheduler(final Server server, final JavaPlugin plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void scheduleFor(final Entity entity, final Runnable task) {
        if (Folia.FOLIA) {
            entity.getBukkitEntity().getScheduler().execute(this.plugin, task, null, 0L);
        } else {
            task.run();
        }
    }

    @Override
    public void scheduleFor(final Commander commander, final Runnable task) {
        if (!Folia.FOLIA) {
            task.run();
            return;
        }
        final CommandSender sender = ((PaperCommander) commander).sender();
        if (sender instanceof org.bukkit.entity.Entity entity) {
            entity.getScheduler().execute(this.plugin, task, null, 0L);
        } else if (sender instanceof BlockCommandSender block) {
            this.server.getRegionScheduler().execute(this.plugin, block.getBlock().getLocation(), task);
        } else {
            this.server.getGlobalRegionScheduler().execute(this.plugin, task);
        }
    }
}
