package xyz.jpenilla.squaremap.common.command.commands;

import com.google.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.component.TypedCommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.help.result.CommandEntry;
import org.incendo.cloud.minecraft.extras.ImmutableMinecraftHelp;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Components;

import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;

@DefaultQualifier(NonNull.class)
public final class HelpCommand extends SquaremapCommand {
    private final MinecraftHelp<Commander> minecraftHelp;
    private final TypedCommandComponent<Commander, String> helpQueryArgument;

    @Inject
    private HelpCommand(final Commands commands) {
        super(commands);
        this.minecraftHelp = createMinecraftHelp(commands.commandManager());
        this.helpQueryArgument = createHelpQueryArgument(commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("help")
                .commandDescription(richDescription(Messages.HELP_COMMAND_DESCRIPTION))
                .argument(this.helpQueryArgument)
                .permission("squaremap.command.help")
                .handler(this::executeHelp));
    }

    private void executeHelp(final CommandContext<Commander> context) {
        this.minecraftHelp.queryCommands(
            context.optional(this.helpQueryArgument).orElse(""),
            context.sender()
        );
    }

    private static TypedCommandComponent<Commander, String> createHelpQueryArgument(final Commands commands) {
        final var commandHelpHandler = commands.commandManager().createHelpHandler();
        final BlockingSuggestionProvider.Strings<Commander> suggestions = (context, input) ->
            commandHelpHandler.queryRootIndex(context.sender()).entries().stream()
                .map(CommandEntry::syntax)
                .toList();
        return CommandComponent.<Commander, String>ofType(String.class, "query")
            .parser(StringParser.greedyStringParser())
            .suggestionProvider(suggestions)
            .optional()
            .description(richDescription(Messages.HELP_QUERY_ARGUMENT_DESCRIPTION))
            .build();
    }

    private static MinecraftHelp<Commander> createMinecraftHelp(final CommandManager<Commander> manager) {
        final MinecraftHelp<Commander> minecraftHelp = MinecraftHelp.createNative(
            String.format("/%s help", Config.MAIN_COMMAND_LABEL),
            manager
        );
        return ImmutableMinecraftHelp.copyOf(minecraftHelp).withColors(
            MinecraftHelp.helpColors(
                TextColor.color(0x5B00FF),
                NamedTextColor.WHITE,
                TextColor.color(0xC028FF),
                NamedTextColor.GRAY,
                NamedTextColor.DARK_GRAY
            )
        ).withMessageProvider(HelpCommand::helpMessage);
    }

    private static Component helpMessage(final Commander sender, final String key, final Map<String, String> args) {
        return Messages.componentMessage(Messages.COMMAND_HELP_MESSAGE_PREFIX + key)
            .withPlaceholders(args.entrySet().stream().map(e -> Components.placeholder(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }
}
