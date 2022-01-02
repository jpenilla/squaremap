package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.CommandHelpHandler;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import java.util.stream.Collectors;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.CommandUtil;

public final class HelpCommand extends SquaremapCommand {
    private final MinecraftHelp<Commander> minecraftHelp;

    public HelpCommand(final @NonNull Commands commands) {
        super(commands);
        this.minecraftHelp = new MinecraftHelp<>(
            String.format("/%s help", Config.MAIN_COMMAND_LABEL),
            AudienceProvider.nativeAudience(),
            commands.commandManager()
        );
        this.minecraftHelp.setHelpColors(MinecraftHelp.HelpColors.of(
            TextColor.color(0x5B00FF),
            NamedTextColor.WHITE,
            TextColor.color(0xC028FF),
            NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY
        ));
        this.minecraftHelp.setMessage(MinecraftHelp.MESSAGE_HELP_TITLE, "squaremap command help");
    }

    @Override
    public void register() {
        final var commandHelpHandler = this.commands.commandManager().getCommandHelpHandler();
        final var helpQueryArgument = StringArgument.<Commander>newBuilder("query")
            .greedy()
            .asOptional()
            .withSuggestionsProvider((context, input) -> {
                final var indexHelpTopic = (CommandHelpHandler.IndexHelpTopic<Commander>) commandHelpHandler.queryHelp(context.getSender(), "");
                return indexHelpTopic.getEntries()
                    .stream()
                    .map(CommandHelpHandler.VerboseHelpEntry::getSyntaxString)
                    .collect(Collectors.toList());
            })
            .build();

        this.commands.registerSubcommand(builder ->
            builder.literal("help")
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.HELP_COMMAND_DESCRIPTION))
                .argument(helpQueryArgument, CommandUtil.description(Lang.HELP_QUERY_ARGUMENT_DESCRIPTION))
                .permission("squaremap.command.help")
                .handler(this::executeHelp));
    }

    private void executeHelp(final @NonNull CommandContext<Commander> context) {
        this.minecraftHelp.queryCommands(
            context.<String>getOptional("query").orElse(""),
            context.getSender()
        );
    }
}
