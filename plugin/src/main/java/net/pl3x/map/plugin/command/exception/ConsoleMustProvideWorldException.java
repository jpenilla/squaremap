package net.pl3x.map.plugin.command.exception;

import cloud.commandframework.context.CommandContext;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ConsoleMustProvideWorldException extends IllegalArgumentException {

    private final CommandContext<CommandSender> context;

    public ConsoleMustProvideWorldException(final @NonNull CommandContext<CommandSender> context) {
        super();
        this.context = context;
    }

    public CommandContext<CommandSender> context() {
        return this.context;
    }

}
