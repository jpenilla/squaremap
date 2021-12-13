package xyz.jpenilla.squaremap.plugin.command;

import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;

public abstract class SquaremapCommand {
    protected final SquaremapPlugin plugin;
    protected final Commands commands;

    protected SquaremapCommand(
            final @NonNull SquaremapPlugin plugin,
            final @NonNull Commands commands
    ) {
        this.plugin = plugin;
        this.commands = commands;
    }

    public abstract void register();
}
