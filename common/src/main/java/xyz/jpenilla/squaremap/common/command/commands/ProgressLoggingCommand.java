package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Components;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

@DefaultQualifier(NonNull.class)
public final class ProgressLoggingCommand extends SquaremapCommand {
    private final WorldManager worldManager;

    @Inject
    private ProgressLoggingCommand(
        final Commands commands,
        final WorldManager worldManager
    ) {
        super(commands);
        this.worldManager = worldManager;
    }

    @Override
    public void register() {
        final Command.Builder<Commander> progressLogging = this.commands.rootBuilder()
            .literal("progresslogging")
            .permission("squaremap.command.progresslogging");

        this.commands.register(progressLogging
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.PROGRESSLOGGING_COMMAND_DESCRIPTION.asComponent())
            .handler(this::executePrint));
        this.commands.register(progressLogging.literal("toggle")
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.PROGRESSLOGGING_TOGGLE_COMMAND_DESCRIPTION.asComponent())
            .handler(this::executeToggle));
        this.commands.register(progressLogging.literal("rate")
            .argument(IntegerArgument.<Commander>newBuilder("seconds").withMin(1))
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.PROGRESSLOGGING_RATE_COMMAND_DESCRIPTION.asComponent())
            .handler(this::executeRate));
    }

    private void executePrint(final CommandContext<Commander> context) {
        context.getSender().sendMessage(
            Messages.PROGRESSLOGGING_STATUS_MESSAGE.withPlaceholders(
                Components.placeholder("seconds", Config.PROGRESS_LOGGING_INTERVAL),
                Components.placeholder("enabled", clickAndHover(Config.PROGRESS_LOGGING ? text("✔", GREEN) : text("✖", RED)))
            )
        );
    }

    private void executeToggle(final CommandContext<Commander> context) {
        Config.toggleProgressLogging();

        this.worldManager.worlds()
            .forEach(mapWorld -> mapWorld.renderManager().restartRenderProgressLogging());

        final ComponentLike message;
        if (Config.PROGRESS_LOGGING) {
            message = Messages.PROGRESSLOGGING_ENABLED_MESSAGE;
        } else {
            message = Messages.PROGRESSLOGGING_DISABLED_MESSAGE;
        }
        context.getSender().sendMessage(clickAndHover(message));
    }

    private static Component clickAndHover(final ComponentLike componentLike) {
        return componentLike.asComponent().hoverEvent(Messages.CLICK_TO_TOGGLE.asComponent())
            .clickEvent(runCommand("/" + Config.MAIN_COMMAND_LABEL + " progresslogging toggle"));
    }

    private void executeRate(final CommandContext<Commander> context) {
        final int seconds = context.get("seconds");
        Config.setLoggingInterval(seconds);

        this.worldManager.worlds()
            .forEach(mapWorld -> mapWorld.renderManager().restartRenderProgressLogging());

        context.getSender().sendMessage(Messages.PROGRESSLOGGING_SET_RATE_MESSAGE.withPlaceholders(Components.placeholder("seconds", seconds)));
    }
}
