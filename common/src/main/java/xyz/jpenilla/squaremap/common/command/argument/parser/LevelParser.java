package xyz.jpenilla.squaremap.common.command.argument.parser;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.parser.ParserDescriptor;
import cloud.commandframework.arguments.suggestion.BlockingSuggestionProvider;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.context.CommandInput;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.util.Util;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;

@DefaultQualifier(NonNull.class)
public final class LevelParser<C> implements ArgumentParser<C, ServerLevel>, BlockingSuggestionProvider.Strings<C> {

    public static <C> ParserDescriptor<C, ServerLevel> levelParser() {
        return ParserDescriptor.of(new LevelParser<>(), ServerLevel.class);
    }

    @Override
    public ArgumentParseResult<ServerLevel> parse(final CommandContext<C> commandContext, final CommandInput commandInput) {
        final String input = commandInput.readString();

        final @Nullable ResourceLocation key = ResourceLocation.tryParse(input);
        if (key == null || key.getPath().isEmpty()) {
            return failure(new MapWorldParser.MapWorldParseException(input, MapWorldParser.MapWorldParseException.FailureReason.NO_SUCH_WORLD));
        }

        final @Nullable ServerLevel world = commandContext.get(Commands.SERVER_ACCESS).level(Util.worldIdentifier(key));
        if (world == null) {
            return failure(new MapWorldParser.MapWorldParseException(input, MapWorldParser.MapWorldParseException.FailureReason.NO_SUCH_WORLD));
        }

        return success(world);
    }

    @Override
    public List<String> stringSuggestions(final CommandContext<C> commandContext, final CommandInput commandInput) {
        return commandContext.get(Commands.SERVER_ACCESS).levels().stream()
            .flatMap(mapWorld -> {
                final ResourceLocation identifier = mapWorld.dimension().location();
                if (!commandInput.remainingInput().isBlank() && identifier.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                    return Stream.of(identifier.getPath(), identifier.toString());
                }
                return Stream.of(identifier.toString());
            })
            .toList();
    }
}
