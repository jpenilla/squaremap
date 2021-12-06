package net.pl3x.map.plugin.command;

import net.pl3x.map.plugin.Pl3xMapPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class Pl3xMapCommand {
    protected final Pl3xMapPlugin plugin;
    protected final Commands commands;

    protected Pl3xMapCommand(
            final @NonNull Pl3xMapPlugin plugin,
            final @NonNull Commands commands
    ) {
        this.plugin = plugin;
        this.commands = commands;
    }

    public abstract void register();
}
