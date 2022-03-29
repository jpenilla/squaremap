package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.CommandUtil;
import xyz.jpenilla.squaremap.common.util.Components;

@DefaultQualifier(NonNull.class)
public final class HideShowCommands extends SquaremapCommand {
    private final Function<String, CommandArgument<Commander, ?>> playerArgument;
    private final BiFunction<String, CommandContext<Commander>, ServerPlayer> getPlayer;

    public HideShowCommands(
        final Commands commands,
        final Function<String, CommandArgument<Commander, ?>> playerArgument,
        final BiFunction<String, CommandContext<Commander>, ServerPlayer> getPlayer
    ) {
        super(commands);
        this.playerArgument = playerArgument;
        this.getPlayer = getPlayer;
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("hide")
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.HIDE_COMMAND_DESCRIPTION))
                .permission("squaremap.command.hide")
                .handler(this::executeHide));
        this.commands.registerSubcommand(builder ->
            builder.literal("hide")
                .argument(this.playerArgument.apply("player"), CommandUtil.description(Lang.OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.HIDE_COMMAND_DESCRIPTION))
                .permission("squaremap.command.hide.others")
                .handler(this::executeHide));

        this.commands.registerSubcommand(builder ->
            builder.literal("show")
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.SHOW_COMMAND_DESCRIPTION))
                .permission("squaremap.command.show")
                .handler(this::executeShow));
        this.commands.registerSubcommand(builder ->
            builder.literal("show")
                .argument(this.playerArgument.apply("player"), CommandUtil.description(Lang.OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.SHOW_COMMAND_DESCRIPTION))
                .permission("squaremap.command.show.others")
                .handler(this::executeShow));
    }

    private void executeHide(final CommandContext<Commander> context) {
        final ServerPlayer target = this.getPlayer.apply("player", context);
        final Commander sender = context.getSender();
        if (context.get(Commands.PLAYER_MANAGER).hidden(target.getUUID())) {
            Lang.send(sender, Lang.PLAYER_ALREADY_HIDDEN, Placeholder.unparsed("player", target.getGameProfile().getName()));
            return;
        }

        context.get(Commands.PLAYER_MANAGER).hide(target.getUUID(), true);
        Lang.send(sender, Lang.PLAYER_HIDDEN, Placeholder.unparsed("player", target.getGameProfile().getName()));
    }

    private void executeShow(final CommandContext<Commander> context) {
        final ServerPlayer target = this.getPlayer.apply("player", context);
        final Commander sender = context.getSender();
        if (!context.get(Commands.PLAYER_MANAGER).hidden(target.getUUID())) {
            Lang.send(sender, Lang.PLAYER_NOT_HIDDEN, Placeholder.unparsed("player", target.getGameProfile().getName()));
            return;
        }

        context.get(Commands.PLAYER_MANAGER).show(target.getUUID(), true);
        Lang.send(sender, Lang.PLAYER_SHOWN, Placeholder.unparsed("player", target.getGameProfile().getName()));
    }
}
