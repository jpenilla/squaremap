package net.pl3x.map.command;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.common.collect.ImmutableSet;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.configuration.Lang;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

public class CommandManager extends PaperCommandManager<CommandSender> {

    public CommandManager(final @NonNull Pl3xMap plugin) throws Exception {

        super(
                plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                UnaryOperator.identity(),
                UnaryOperator.identity()
        );

        final Function<ParserParameters, CommandMeta> commandMetaFunction = parameters ->
                SimpleCommandMeta.builder()
                        .with(CommandMeta.DESCRIPTION, parameters.get(StandardParameters.DESCRIPTION, "No description"))
                        .build();

        final AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(
                this,
                CommandSender.class,
                commandMetaFunction
        );

        if (this.queryCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            this.registerBrigadier();
            final CloudBrigadierManager<?, ?> brigManager = this.brigadierManager();
            if (brigManager != null) {
                brigManager.setNativeNumberSuggestions(false);
            }
        }

        if (this.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            this.registerAsynchronousCompletions();
        }

        this.registerExceptionHandler(CommandExecutionException.class, (sender, ex) -> {
            final Throwable cause = ex.getCause();
            if (cause instanceof ConsoleMustProvidePlayerException) {
                Lang.send(sender, Lang.PROVIDE_A_WORLD);
            } else {
                plugin.getLogger().log(Level.WARNING, "Unexpected error occurred during command execution", cause);
                Lang.send(sender, Lang.INTERNAL_ERROR);
            }
        });

        ImmutableSet.of(
                new CmdPl3xMap(plugin)
        ).forEach(annotationParser::parse);

    }

}
