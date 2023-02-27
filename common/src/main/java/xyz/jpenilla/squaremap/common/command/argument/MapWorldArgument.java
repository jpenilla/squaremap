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
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.ComponentMessageThrowable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Components;
import xyz.jpenilla.squaremap.common.util.Util;

import static cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static cloud.commandframework.arguments.parser.ArgumentParseResult.success;

/**
 * cloud argument type that parses {@link MapWorldInternal}
 *
 * @param <C> Command sender type
 */
@DefaultQualifier(NonNull.class)
public final class MapWorldArgument<C> extends CommandArgument<C, MapWorldInternal> {
    private MapWorldArgument(
        final boolean required,
        final String name,
        final String defaultValue,
        final @Nullable BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
        final ArgumentDescription defaultDescription
    ) {
        super(required, name, new Parser<>(), defaultValue, MapWorldInternal.class, suggestionsProvider, defaultDescription);
    }

    /**
     * Create a new builder
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     */
    public static <C> Builder<C> builder(final String name) {
        return new MapWorldArgument.Builder<>(name);
    }

    /**
     * Create a new required argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> MapWorldArgument<C> of(final String name) {
        return MapWorldArgument.<C>builder(name).build();
    }

    /**
     * Create a new optional argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> MapWorldArgument<C> optional(final String name) {
        return MapWorldArgument.<C>builder(name).asOptional().build();
    }

    /**
     * Create a new optional argument with a default value
     *
     * @param name         Argument name
     * @param defaultValue Default value
     * @param <C>          Command sender type
     * @return Created argument
     */
    public static <C> MapWorldArgument<C> optional(final String name, final String defaultValue) {
        return MapWorldArgument.<C>builder(name).asOptionalWithDefault(defaultValue).build();
    }

    public static final class Builder<C> extends CommandArgument.TypedBuilder<C, MapWorldInternal, Builder<C>> {
        private Builder(final String name) {
            super(MapWorldInternal.class, name);
        }

        @Override
        public MapWorldArgument<C> build() {
            return new MapWorldArgument<>(
                this.isRequired(),
                this.getName(),
                this.getDefaultValue(),
                this.getSuggestionsProvider(),
                this.getDefaultDescription()
            );
        }
    }

    public static final class Parser<C> implements ArgumentParser<C, MapWorldInternal> {
        @Override
        public ArgumentParseResult<MapWorldInternal> parse(final CommandContext<C> commandContext, final Queue<String> inputQueue) {
            final @Nullable String input = inputQueue.peek();
            if (input == null) {
                return failure(new NoInputProvidedException(Parser.class, commandContext));
            }

            final @Nullable ResourceLocation key = ResourceLocation.tryParse(input);
            if (key == null) {
                return failure(new MapWorldParseException(input, MapWorldParseException.FailureReason.NO_SUCH_WORLD));
            }

            final @Nullable ServerLevel world = commandContext.get(Commands.SERVER_ACCESS).level(Util.worldIdentifier(key));
            if (world == null) {
                return failure(new MapWorldParseException(input, MapWorldParseException.FailureReason.NO_SUCH_WORLD));
            }

            final WorldConfig worldConfig = commandContext.get(Commands.CONFIG_MANAGER).worldConfig(world);
            if (!worldConfig.MAP_ENABLED) {
                return failure(new MapWorldParseException(input, MapWorldParseException.FailureReason.MAP_NOT_ENABLED));
            }

            inputQueue.remove();
            return success(commandContext.get(Commands.WORLD_MANAGER).getWorldIfEnabled(world).orElseThrow());
        }

        @Override
        public List<String> suggestions(final CommandContext<C> commandContext, final String input) {
            return commandContext.get(Commands.WORLD_MANAGER).worlds().stream()
                .flatMap(mapWorld -> {
                    final WorldIdentifier identifier = mapWorld.identifier();
                    if (!input.isBlank() && identifier.namespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                        return Stream.of(identifier.value(), identifier.asString());
                    }
                    return Stream.of(identifier.asString());
                })
                .toList();
        }
    }

    public static final class MapWorldParseException extends IllegalArgumentException implements ComponentMessageThrowable {
        private static final long serialVersionUID = 3072715326923004782L;

        private final String input;
        private final FailureReason reason;

        public MapWorldParseException(
            final String input,
            final FailureReason reason
        ) {
            this.input = input;
            this.reason = reason;
        }

        @Override
        public Component componentMessage() {
            return this.reason.failureMessage.get().withPlaceholders(Components.placeholder("world", this.input));
        }

        @Override
        public String getMessage() {
            return PlainTextComponentSerializer.plainText().serialize(this.componentMessage());
        }

        public enum FailureReason {
            NO_SUCH_WORLD(() -> Messages.NO_SUCH_WORLD),
            MAP_NOT_ENABLED(() -> Messages.MAP_NOT_ENABLED_FOR_WORLD);

            private final Supplier<Messages.ComponentMessage> failureMessage;

            FailureReason(final Supplier<Messages.ComponentMessage> failureMessage) {
                this.failureMessage = failureMessage;
            }
        }
    }
}
