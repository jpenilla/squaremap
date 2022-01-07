package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.Components;

public final class ReloadCommand extends SquaremapCommand {
    public ReloadCommand(final @NonNull Commands commands) {
        super(commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("reload")
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.RELOAD_COMMAND_DESCRIPTION))
                .permission("squaremap.command.reload")
                .handler(this::execute));
    }

    public void execute(final @NonNull CommandContext<Commander> context) {
        context.get(Commands.COMMON).reload(context.getSender());
    }
}
