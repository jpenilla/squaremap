package xyz.jpenilla.squaremap.common.command.commands;

import com.google.inject.Inject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.context.CommandContext;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Messages;

import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;

@DefaultQualifier(NonNull.class)
public final class ReloadCommand extends SquaremapCommand {
    private final SquaremapCommon common;

    @Inject
    private ReloadCommand(
        final Commands commands,
        final SquaremapCommon common
    ) {
        super(commands);
        this.common = common;
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("reload")
                .commandDescription(richDescription(Messages.RELOAD_COMMAND_DESCRIPTION))
                .permission("squaremap.command.reload")
                .handler(this::execute));
    }

    public void execute(final CommandContext<Commander> context) {
        this.common.reload(context.sender());
    }
}
