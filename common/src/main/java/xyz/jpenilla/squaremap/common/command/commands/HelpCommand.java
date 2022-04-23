package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.CommandHelpHandler;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.minecraft.extras.RichDescription;
import com.google.inject.Inject;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;

@DefaultQualifier(NonNull.class)
public final class HelpCommand extends SquaremapCommand {
    private final MinecraftHelp<Commander> minecraftHelp;

    @Inject
    private HelpCommand(final Commands commands) {
        super(commands);
        this.minecraftHelp = createMinecraftHelp(commands.commandManager());
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
                    .toList();
            })
            .build();

        this.commands.registerSubcommand(builder ->
            builder.literal("help")
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.HELP_COMMAND_DESCRIPTION.asComponent())
                .argument(helpQueryArgument, RichDescription.of(Messages.HELP_QUERY_ARGUMENT_DESCRIPTION))
                .permission("squaremap.command.help")
                .handler(this::executeHelp));
    }

    private void executeHelp(final CommandContext<Commander> context) {
        this.minecraftHelp.queryCommands(
            context.<String>getOptional("query").orElse(""),
            context.getSender()
        );
    }

    private static MinecraftHelp<Commander> createMinecraftHelp(final CommandManager<Commander> manager) {
        final MinecraftHelp<Commander> minecraftHelp = new MinecraftHelp<>(
            String.format("/%s help", Config.MAIN_COMMAND_LABEL),
            AudienceProvider.nativeAudience(),
            manager
        );
        minecraftHelp.setHelpColors(MinecraftHelp.HelpColors.of(
            TextColor.color(0x5B00FF),
            NamedTextColor.WHITE,
            TextColor.color(0xC028FF),
            NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY
        ));
        minecraftHelp.setMessage(MinecraftHelp.MESSAGE_HELP_TITLE, "squaremap command help");
        return minecraftHelp;
    }
}
