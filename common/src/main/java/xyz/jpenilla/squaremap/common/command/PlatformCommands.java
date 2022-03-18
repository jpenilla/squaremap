package xyz.jpenilla.squaremap.common.command;

import cloud.commandframework.CommandManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface PlatformCommands {
    CommandManager<Commander> createCommandManager();

    void registerCommands(Commands commands);
}
