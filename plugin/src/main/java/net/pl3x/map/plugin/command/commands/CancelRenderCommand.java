package net.pl3x.map.plugin.command.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.command.Pl3xMapCommand;
import net.pl3x.map.plugin.command.argument.MapWorldArgument;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.MapWorld;
import net.pl3x.map.plugin.util.CommandUtil;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class CancelRenderCommand extends Pl3xMapCommand {

    public CancelRenderCommand(final @NonNull Pl3xMapPlugin plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        this.commandManager.registerSubcommand(builder ->
                builder.literal("cancelrender")
                        .argument(MapWorldArgument.optional("world"), ArgumentDescription.of("Defaults to the players current world if not provided"))
                        .meta(CommandMeta.DESCRIPTION, "Cancels a render for the specified world")
                        .permission("pl3xmap.command.cancelrender")
                        .handler(this::executeCancelRender));
    }

    private void executeCancelRender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final MapWorld world = CommandUtil.resolveWorld(context);
        if (!world.isRendering()) {
            Lang.send(sender, Lang.RENDER_NOT_IN_PROGRESS
                    .replace("{world}", world.name()));
            return;
        }

        Lang.send(sender, Lang.CANCELLED_RENDER
                .replace("{world}", world.name()));
        world.stopRender();
    }

}
