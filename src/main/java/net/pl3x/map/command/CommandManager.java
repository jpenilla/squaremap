package net.pl3x.map.command;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.bukkit.BukkitCommandMetaBuilder;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import io.leangen.geantyref.TypeToken;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.command.argument.WorldArgument;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.function.Function;

public class CommandManager extends PaperCommandManager<CommandSender> {

    public CommandManager(Pl3xMap plugin) throws Exception {

        super(
                plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity()
        );

        final Function<ParserParameters, CommandMeta> commandMetaFunction = p ->
                BukkitCommandMetaBuilder.builder()
                        .withDescription(p.get(StandardParameters.DESCRIPTION, "No description"))
                        .build();

        final AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(
                this,
                CommandSender.class,
                commandMetaFunction
        );

        if (this.queryCapability(CloudBukkitCapabilities.BRIGADIER)) {
            this.registerBrigadier();
        }

        if (this.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            this.registerAsynchronousCompletions();
        }

        // Register World type parser for annotated methods
        this.getParserRegistry().registerParserSupplier(
                TypeToken.get(World.class),
                parserParameters ->
                        new WorldArgument.WorldParser<>()
        );

        ImmutableList.of(
                new CmdPl3xMap(plugin)
        ).forEach(annotationParser::parse);

    }

}
