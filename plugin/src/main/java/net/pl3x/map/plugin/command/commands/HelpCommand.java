package net.pl3x.map.plugin.command.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.CommandHelpHandler;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.command.Pl3xMapCommand;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.stream.Collectors;

public final class HelpCommand extends Pl3xMapCommand {

    private final MinecraftHelp<CommandSender> minecraftHelp;

    public HelpCommand(final @NonNull Pl3xMapPlugin plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
        this.minecraftHelp = new MinecraftHelp<>(
                "/pl3xmap help",
                plugin.audiences()::sender,
                commandManager
        );
        this.minecraftHelp.setHelpColors(MinecraftHelp.HelpColors.of(
                TextColor.color(0x5B00FF),
                NamedTextColor.WHITE,
                TextColor.color(0xC028FF),
                NamedTextColor.GRAY,
                NamedTextColor.DARK_GRAY
        ));
        this.minecraftHelp.setMessage(MinecraftHelp.MESSAGE_HELP_TITLE, "Pl3xMap Help");
    }

    @Override
    public void register() {
        final var commandHelpHandler = this.commandManager.getCommandHelpHandler();
        final var helpQueryArgument = StringArgument.<CommandSender>newBuilder("query")
                .greedy()
                .asOptional()
                .withSuggestionsProvider((context, input) -> {
                    final var indexHelpTopic = (CommandHelpHandler.IndexHelpTopic<CommandSender>) commandHelpHandler.queryHelp(context.getSender(), "");
                    return indexHelpTopic.getEntries()
                            .stream()
                            .map(CommandHelpHandler.VerboseHelpEntry::getSyntaxString)
                            .collect(Collectors.toList());
                })
                .build();

        this.commandManager.registerSubcommand(builder ->
                builder.literal("help")
                        .argument(helpQueryArgument, ArgumentDescription.of("Help Query"))
                        .handler(context -> this.minecraftHelp.queryCommands(
                                context.getOptional(helpQueryArgument).orElse(""),
                                context.getSender()
                        )));
    }

}
