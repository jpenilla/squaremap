package xyz.jpenilla.squaremap.plugin.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.command.Commands;
import xyz.jpenilla.squaremap.plugin.command.SquaremapCommand;
import xyz.jpenilla.squaremap.plugin.config.Advanced;
import xyz.jpenilla.squaremap.plugin.config.Config;
import xyz.jpenilla.squaremap.plugin.config.Lang;
import xyz.jpenilla.squaremap.plugin.util.FileUtil;

public final class ReloadCommand extends SquaremapCommand {

    public ReloadCommand(final @NonNull SquaremapPlugin plugin, final @NonNull Commands commands) {
        super(plugin, commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
                builder.literal("reload")
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.RELOAD_COMMAND_DESCRIPTION))
                        .permission("squaremap.command.reload")
                        .handler(this::execute));
    }

    public void execute(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        plugin.stop();

        Config.reload();
        Advanced.reload();
        Lang.reload();
        FileUtil.reload();

        plugin.start();

        PluginDescriptionFile desc = plugin.getDescription();
        Lang.send(sender, Lang.PLUGIN_RELOADED,
                Template.template("name", desc.getName()),
                Template.template("version", desc.getVersion())
        );
    }
}
