package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.CommandHelpHandler;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.minecraft.extras.RichDescription;
import com.google.inject.Inject;
import java.util.List;
import java.util.function.BiFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Components;

@DefaultQualifier(NonNull.class)
public final class HelpCommand extends SquaremapCommand {
    private final MinecraftHelp<Commander> minecraftHelp;
    private final CommandArgument<Commander, String> helpQueryArgument;

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
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.HELP_COMMAND_DESCRIPTION.asComponent())
                .argument(this.helpQueryArgument, RichDescription.of(Messages.HELP_QUERY_ARGUMENT_DESCRIPTION))
                .permission("squaremap.command.help")
                .handler(this::executeHelp));
    }

    private void executeHelp(final CommandContext<Commander> context) {
        this.minecraftHelp.queryCommands(
            context.getOptional(this.helpQueryArgument).orElse(""),
            context.getSender()
        );
    }

    private static CommandArgument<Commander, String> createHelpQueryArgument(final Commands commands) {
        final var commandHelpHandler = commands.commandManager().createCommandHelpHandler();
        final BiFunction<CommandContext<Commander>, String, List<String>> suggestions = (context, input) ->
            commandHelpHandler.queryRootIndex(context.getSender()).getEntries().stream()
                .map(CommandHelpHandler.VerboseHelpEntry::getSyntaxString)
                .toList();
        return StringArgument.<Commander>builder("query")
            .greedy()
            .withSuggestionsProvider(suggestions)
            .asOptional()
            .build();
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
        minecraftHelp.messageProvider(HelpCommand::helpMessage);
        return minecraftHelp;
    }

    private static Component helpMessage(final Commander sender, final String key, final String... args) {
        // Hack but works
        final TagResolver[] placeholders;
        if (args.length == 0) {
            placeholders = new TagResolver[]{};
        } else {
            placeholders = new TagResolver[]{
                Components.placeholder("page", args[0]),
                Components.placeholder("max_pages", args[1])
            };
        }

        return Messages.componentMessage(Messages.COMMAND_HELP_MESSAGE_PREFIX + key).withPlaceholders(placeholders);
    }
}
