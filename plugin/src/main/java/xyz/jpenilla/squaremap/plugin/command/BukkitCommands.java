package xyz.jpenilla.squaremap.plugin.command;

import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.location.Location2D;
import cloud.commandframework.bukkit.parsers.location.Location2DArgument;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.minecraft.core.BlockPos;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.commands.HideShowCommands;
import xyz.jpenilla.squaremap.common.command.commands.RadiusRenderCommand;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Lang;

public final class BukkitCommands {
    private BukkitCommands() {
    }

    public static void register(final @NonNull SquaremapCommon common) {
        List.of(
            new RadiusRenderCommand(
                common.commands(),
                Location2DArgument::optional,
                (name, context) -> {
                    final @Nullable Location2D loc = context.getOrDefault(name, null);
                    if (loc == null) {
                        return null;
                    }
                    return new BlockPos(loc.getBlockX(), 0, loc.getBlockZ());
                }
            ),
            new HideShowCommands(
                common.commands(),
                SinglePlayerSelectorArgument::of,
                (name, context) -> ((CraftPlayer) BukkitCommands.resolvePlayer(name, context)).getHandle()
            )
        ).forEach(SquaremapCommand::register);
    }

    private static @NonNull Player resolvePlayer(final @NonNull String argName, final @NonNull CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final SinglePlayerSelector selector = context.getOrDefault(argName, null);

        if (selector == null) {
            if (sender instanceof PlayerCommander player) {
                return player.player().getBukkitEntity();
            }
            throw CommandCompleted.withMessage(MiniMessage.miniMessage().deserialize(Lang.CONSOLE_MUST_SPECIFY_PLAYER));
        }

        final Player targetPlayer = selector.getPlayer();
        if (targetPlayer == null) {
            Lang.send(sender, Lang.PLAYER_NOT_FOUND_FOR_INPUT, Template.template("input", selector.getSelector()));
            throw CommandCompleted.withoutMessage();
        }

        return targetPlayer;
    }
}
