package xyz.jpenilla.squaremap.plugin.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.task.render.FullRender;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.command.Commands;
import xyz.jpenilla.squaremap.plugin.command.SquaremapCommand;
import xyz.jpenilla.squaremap.plugin.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.plugin.data.PaperMapWorld;
import xyz.jpenilla.squaremap.plugin.util.CommandUtil;

public final class FullRenderCommand extends SquaremapCommand {

    public FullRenderCommand(final @NonNull SquaremapPlugin plugin, final @NonNull Commands commands) {
        super(plugin, commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
                builder.literal("fullrender")
                        .argument(MapWorldArgument.optional("world"), CommandUtil.description(Lang.OPTIONAL_WORLD_ARGUMENT_DESCRIPTION))
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.FULLRENDER_COMMAND_DESCRIPTION))
                        .permission("squaremap.command.fullrender")
                        .handler(this::executeFullRender));
    }

    private void executeFullRender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final PaperMapWorld world = CommandUtil.resolveWorld(context);
        if (world.isRendering()) {
            Lang.send(sender, Lang.RENDER_IN_PROGRESS, Template.template("world", world.identifier().asString()));
            return;
        }

        if (sender instanceof Player) {
            Lang.send(sender, Lang.LOG_STARTED_FULLRENDER, Template.template("world", world.identifier().asString()));
        }
        world.startRender(new FullRender(world));
    }

}
