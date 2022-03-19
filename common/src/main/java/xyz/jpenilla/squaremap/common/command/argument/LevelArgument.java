package xyz.jpenilla.squaremap.common.command.argument;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
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
public class LevelArgument<C> extends CommandArgument<C, ServerLevel> {
    protected LevelArgument(
        final boolean required,
        final String name,
        final String defaultValue,
        final @Nullable BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
        final ArgumentDescription defaultDescription
    ) {
        super(required, name, new Parser<>(), defaultValue, ServerLevel.class, suggestionsProvider, defaultDescription);
    }

    public static <C> Builder<C> builder(final String name) {
        return new Builder<>(name);
    }

    public static <C> LevelArgument<C> of(final String name) {
        return LevelArgument.<C>builder(name).build();
    }

    public static <C> LevelArgument<C> optional(final String name) {
        return LevelArgument.<C>builder(name).asOptional().build();
    }

    public static <C> LevelArgument<C> optional(final String name, final String defaultValue) {
        return LevelArgument.<C>builder(name).asOptionalWithDefault(defaultValue).build();
    }

    public static final class Builder<C> extends CommandArgument.TypedBuilder<C, ServerLevel, Builder<C>> {
        private Builder(final String name) {
            super(ServerLevel.class, name);
        }

        @Override
        public LevelArgument<C> build() {
            return new LevelArgument<>(
                this.isRequired(),
                this.getName(),
                this.getDefaultValue(),
                this.getSuggestionsProvider(),
                this.getDefaultDescription()
            );
        }
    }

    public static final class Parser<C> implements ArgumentParser<C, ServerLevel> {

        @Override
        public ArgumentParseResult<ServerLevel> parse(final CommandContext<C> commandContext, final Queue<String> inputQueue) {
            final @Nullable String input = inputQueue.peek();
            if (input == null) {
                return failure(new NoInputProvidedException(Parser.class, commandContext));
            }

            final @Nullable ResourceLocation key = ResourceLocation.tryParse(input);
            if (key == null) {
                return failure(new MapWorldArgument.MapWorldParseException(input, MapWorldArgument.MapWorldParseException.FailureReason.NO_SUCH_WORLD));
            }

            final @Nullable ServerLevel world = commandContext.get(Commands.PLATFORM).serverAccess().level(Util.worldIdentifier(key));
            if (world == null) {
                return failure(new MapWorldArgument.MapWorldParseException(input, MapWorldArgument.MapWorldParseException.FailureReason.NO_SUCH_WORLD));
            }

            inputQueue.remove();
            return success(world);
        }

        @Override
        public List<String> suggestions(final CommandContext<C> commandContext, final String input) {
            return commandContext.get(Commands.PLATFORM).serverAccess().levels().stream()
                .flatMap(mapWorld -> {
                    final ResourceLocation identifier = mapWorld.dimension().location();
                    if (!input.isBlank() && identifier.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                        return Stream.of(identifier.getPath(), identifier.toString());
                    }
                    return Stream.of(identifier.toString());
                })
                .toList();
        }
    }
}
