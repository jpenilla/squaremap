package xyz.jpenilla.squaremap.common.command;

import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class SquaremapCommand {
    protected final Commands commands;

    protected SquaremapCommand(final @NonNull Commands commands) {
        this.commands = commands;
    }

    public abstract void register();
}
