package net.pl3x.map.plugin.command;

import net.pl3x.map.plugin.Pl3xMap;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class Pl3xMapCommand {
    protected final Pl3xMap plugin;
    protected final CommandManager commandManager;

    protected Pl3xMapCommand(
            final @NonNull Pl3xMap plugin,
            final @NonNull CommandManager commandManager
    ) {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public abstract void register();
}
