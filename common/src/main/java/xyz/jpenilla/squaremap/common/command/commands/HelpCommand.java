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

    @Inject
    private HelpCommand(final Commands commands) {
        super(commands);
        this.minecraftHelp = createMinecraftHelp(commands.commandManager());
    }

    @Override
    public void register() {
        final var commandHelpHandler = this.commands.commandManager().createCommandHelpHandler();
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

        return Messages.componentMessage("command.message.help." + key, placeholders);
    }
}
