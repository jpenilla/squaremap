package net.pl3x.map.plugin.command.argument;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.data.MapWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * cloud argument type that parses {@link MapWorld}
 *
 * @param <C> Command sender type
 */
public class MapWorldArgument<C> extends CommandArgument<C, MapWorld> {

    protected MapWorldArgument(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription
    ) {
        super(required, name, new MapWorldParser<>(), defaultValue, MapWorld.class, suggestionsProvider, defaultDescription);
    }

    /**
     * Create a new builder
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     */
    public static <C> CommandArgument.@NonNull Builder<C, MapWorld> newBuilder(final @NonNull String name) {
        return new MapWorldArgument.Builder<>(name);
    }

    /**
     * Create a new required argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, MapWorld> of(final @NonNull String name) {
        return MapWorldArgument.<C>newBuilder(name).asRequired().build();
    }

    /**
     * Create a new optional argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, MapWorld> optional(final @NonNull String name) {
        return MapWorldArgument.<C>newBuilder(name).asOptional().build();
    }

    /**
     * Create a new optional argument with a default value
     *
     * @param name         Argument name
     * @param defaultValue Default value
     * @param <C>          Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, MapWorld> optional(
            final @NonNull String name,
            final @NonNull String defaultValue
    ) {
        return MapWorldArgument.<C>newBuilder(name).asOptionalWithDefault(defaultValue).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, MapWorld> {

        private Builder(final @NonNull String name) {
            super(MapWorld.class, name);
        }

        @Override
        public @NonNull CommandArgument<C, MapWorld> build() {
            return new MapWorldArgument<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription()
            );
        }

    }


    public static final class MapWorldParser<C> implements ArgumentParser<C, MapWorld> {

        @Override
        public @NonNull ArgumentParseResult<MapWorld> parse(
                final @NonNull CommandContext<C> commandContext,
                final @NonNull Queue<String> inputQueue
        ) {
            final String input = inputQueue.peek();
            if (input == null) {
                return ArgumentParseResult.failure(new NoInputProvidedException(
                        MapWorldParser.class,
                        commandContext
                ));
            }

            final World world = Bukkit.getWorld(input);
            if (world == null) {
                return ArgumentParseResult.failure(new MapWorldParseException(input, MapWorldParseException.FailureReason.NO_SUCH_WORLD));
            }

            final WorldConfig worldConfig = WorldConfig.get(world);
            if (!worldConfig.MAP_ENABLED) {
                return ArgumentParseResult.failure(new MapWorldParseException(input, MapWorldParseException.FailureReason.MAP_NOT_ENABLED));
            }

            inputQueue.remove();
            return ArgumentParseResult.success(Pl3xMapPlugin.getInstance().worldManager().getWorld(world));
        }

        @Override
        public @NonNull List<String> suggestions(final @NonNull CommandContext<C> commandContext, final @NonNull String input) {
            return Pl3xMapPlugin.getInstance().worldManager().worlds().values().stream().map(MapWorld::name).collect(Collectors.toList());
        }

    }


    public static final class MapWorldParseException extends IllegalArgumentException {

        private final String input;
        private final FailureReason reason;

        /**
         * Construct a new MapWorldParseException
         *
         * @param input  Input
         * @param reason Failure reason
         */
        public MapWorldParseException(
                final @NonNull String input,
                final @NonNull FailureReason reason
        ) {
            this.input = input;
            this.reason = reason;
        }

        @Override
        public @NonNull String getMessage() {
            switch (this.reason) {
                case NO_SUCH_WORLD:
                    return Lang.NO_SUCH_WORLD.replace("{world}", this.input);
                case MAP_NOT_ENABLED:
                    return Lang.MAP_NOT_ENABLED_FOR_WORLD.replace("{world}", this.input);
                default:
                    throw new IllegalArgumentException("Unknown MapWorld argument parse failure reason");
            }
        }

        public enum FailureReason {
            NO_SUCH_WORLD, MAP_NOT_ENABLED
        }

    }

}
