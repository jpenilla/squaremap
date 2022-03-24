package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Components;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

@DefaultQualifier(NonNull.class)
public final class ProgressLoggingCommand extends SquaremapCommand {
    private final SquaremapPlatform platform;

    @Inject
    private ProgressLoggingCommand(
        final Commands commands,
        final SquaremapPlatform platform
    ) {
        super(commands);
        this.platform = platform;
    }

    @Override
    public void register() {
        final Command.Builder<Commander> progressLogging = this.commands.rootBuilder()
            .literal("progresslogging")
            .permission("squaremap.command.progresslogging");

        this.commands.register(progressLogging
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.PROGRESSLOGGING_COMMAND_DESCRIPTION))
            .handler(this::executePrint));
        this.commands.register(progressLogging.literal("toggle")
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.PROGRESSLOGGING_TOGGLE_COMMAND_DESCRIPTION))
            .handler(this::executeToggle));
        this.commands.register(progressLogging.literal("rate")
            .argument(IntegerArgument.<Commander>newBuilder("seconds").withMin(1))
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.PROGRESSLOGGING_RATE_COMMAND_DESCRIPTION))
            .handler(this::executeRate));
    }

    private void executePrint(final CommandContext<Commander> context) {
        Lang.send(
            context.getSender(),
            Lang.PROGRESSLOGGING_STATUS_MESSAGE,
            Placeholder.unparsed("seconds", Integer.toString(Config.PROGRESS_LOGGING_INTERVAL)),
            Placeholder.component("enabled", clickAndHover(Config.PROGRESS_LOGGING ? text("✔", GREEN) : text("✖", RED)))
        );
    }

    private void executeToggle(final CommandContext<Commander> context) {
        Config.toggleProgressLogging();

        this.platform.worldManager().worlds().values()
            .forEach(MapWorldInternal::restartRenderProgressLogging);

        final Component message;
        if (Config.PROGRESS_LOGGING) {
            message = Components.miniMessage(Lang.PROGRESSLOGGING_ENABLED_MESSAGE);
        } else {
            message = Components.miniMessage(Lang.PROGRESSLOGGING_DISABLED_MESSAGE);
        }
        context.getSender().sendMessage(clickAndHover(message));
    }

    private static Component clickAndHover(final ComponentLike componentLike) {
        return componentLike.asComponent().hoverEvent(Components.miniMessage(Lang.CLICK_TO_TOGGLE))
            .clickEvent(runCommand("/" + Config.MAIN_COMMAND_LABEL + " progresslogging toggle"));
    }

    private void executeRate(final CommandContext<Commander> context) {
        final int seconds = context.get("seconds");
        Config.setLoggingInterval(seconds);

        this.platform.worldManager().worlds().values()
            .forEach(MapWorldInternal::restartRenderProgressLogging);

        context.getSender().sendMessage(Components.miniMessage(Lang.PROGRESSLOGGING_SET_RATE_MESSAGE, Placeholder.unparsed("seconds", Integer.toString(seconds))));
    }
}
