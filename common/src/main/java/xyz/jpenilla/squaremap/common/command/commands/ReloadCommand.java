package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Advanced;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.FileUtil;

public final class ReloadCommand extends SquaremapCommand {
    public ReloadCommand(final @NonNull Commands commands) {
        super(commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("reload")
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.RELOAD_COMMAND_DESCRIPTION))
                .permission("squaremap.command.reload")
                .handler(this::execute));
    }

    public void execute(final @NonNull CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final SquaremapCommon common = context.get(Commands.COMMON);

        common.stop();

        Config.reload();
        Advanced.reload();
        Lang.reload();
        FileUtil.reload();

        common.start();

        /* TODO
        final PluginDescriptionFile desc = this.plugin.getDescription();
        Lang.send(sender, Lang.PLUGIN_RELOADED,
            Template.template("name", desc.getName()),
            Template.template("version", desc.getVersion())
        );
         */
    }
}
