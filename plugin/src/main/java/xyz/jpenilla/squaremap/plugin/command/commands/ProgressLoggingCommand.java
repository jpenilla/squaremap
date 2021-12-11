package xyz.jpenilla.squaremap.plugin.command.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.Commands;
import net.pl3x.map.plugin.command.Pl3xMapCommand;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.MapWorld;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

@DefaultQualifier(NonNull.class)
public final class ProgressLoggingCommand extends Pl3xMapCommand {
    public ProgressLoggingCommand(final Pl3xMapPlugin plugin, final Commands commands) {
        super(plugin, commands);
    }

    @Override
    public void register() {
        final Command.Builder<CommandSender> progressLogging = this.commands.rootBuilder()
            .literal("progresslogging")
            .permission("squaremap.command.progresslogging");

        this.commands.register(progressLogging
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().deserialize(Lang.PROGRESSLOGGING_COMMAND_DESCRIPTION))
            .handler(this::executePrint));
        this.commands.register(progressLogging.literal("toggle")
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().deserialize(Lang.PROGRESSLOGGING_TOGGLE_COMMAND_DESCRIPTION))
            .handler(this::executeToggle));
        this.commands.register(progressLogging.literal("rate")
            .argument(IntegerArgument.<CommandSender>newBuilder("seconds").withMin(1))
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().deserialize(Lang.PROGRESSLOGGING_RATE_COMMAND_DESCRIPTION))
            .handler(this::executeRate));
    }

    private void executePrint(final CommandContext<CommandSender> context) {
        Lang.send(
            context.getSender(),
            Lang.PROGRESSLOGGING_STATUS_MESSAGE,
            Template.template("seconds", Integer.toString(Config.PROGRESS_LOGGING_INTERVAL)),
            Template.template("enabled", clickAndHover(Config.PROGRESS_LOGGING ? text("✔", GREEN) : text("✖", RED)))
        );
    }

    private void executeToggle(final CommandContext<CommandSender> context) {
        Config.toggleProgressLogging();

        context.get(Commands.PLUGIN_INSTANCE_KEY).worldManager().worlds().values()
            .forEach(MapWorld::restartRenderProgressLogging);

        final Component message;
        if (Config.PROGRESS_LOGGING) {
            message = Lang.parse(Lang.PROGRESSLOGGING_ENABLED_MESSAGE);
        } else {
            message = Lang.parse(Lang.PROGRESSLOGGING_DISABLED_MESSAGE);
        }
        context.getSender().sendMessage(clickAndHover(message));
    }

    private static Component clickAndHover(final ComponentLike componentLike) {
        return componentLike.asComponent().hoverEvent(Lang.parse(Lang.CLICK_TO_TOGGLE))
            .clickEvent(runCommand("/" + Config.MAIN_COMMAND_LABEL + " progresslogging toggle"));
    }

    private void executeRate(final CommandContext<CommandSender> context) {
        final int seconds = context.get("seconds");
        Config.setLoggingInterval(seconds);

        context.get(Commands.PLUGIN_INSTANCE_KEY).worldManager().worlds().values()
            .forEach(MapWorld::restartRenderProgressLogging);

        context.getSender().sendMessage(Lang.parse(Lang.PROGRESSLOGGING_SET_RATE_MESSAGE, Template.template("seconds", Integer.toString(seconds))));
    }
}
