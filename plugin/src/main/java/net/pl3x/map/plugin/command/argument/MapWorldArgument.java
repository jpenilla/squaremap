package net.pl3x.map.plugin.command.argument;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.pl3x.map.plugin.command.Commands;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.data.MapWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * cloud argument type that parses {@link MapWorld}
 *
 * @param <C> Command sender type
 */
@DefaultQualifier(NonNull.class)
public class MapWorldArgument<C> extends CommandArgument<C, MapWorld> {

    protected MapWorldArgument(
        final boolean required,
        final String name,
        final String defaultValue,
        final @Nullable BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
        final ArgumentDescription defaultDescription
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
    public static <C> Builder<C> newBuilder(final String name) {
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
        return MapWorldArgument.<C>newBuilder(name).build();
    }

    /**
     * Create a new optional argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> MapWorldArgument<C> optional(final String name) {
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
    public static <C> MapWorldArgument<C> optional(
        final String name,
        final String defaultValue
    ) {
        return MapWorldArgument.<C>newBuilder(name).asOptionalWithDefault(defaultValue).build();
    }


    public static final class Builder<C> extends CommandArgument.TypedBuilder<C, MapWorld, Builder<C>> {

        private Builder(final String name) {
            super(MapWorld.class, name);
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


    public static final class MapWorldParser<C> implements ArgumentParser<C, MapWorld> {

        @Override
        public ArgumentParseResult<MapWorld> parse(
            final CommandContext<C> commandContext,
            final Queue<String> inputQueue
        ) {
            final @Nullable String input = inputQueue.peek();
            if (input == null) {
                return ArgumentParseResult.failure(new NoInputProvidedException(
                    MapWorldParser.class,
                    commandContext
                ));
            }

            final @Nullable World world = Bukkit.getWorld(input);
            if (world == null) {
                return ArgumentParseResult.failure(new MapWorldParseException(input, MapWorldParseException.FailureReason.NO_SUCH_WORLD));
            }

            final WorldConfig worldConfig = WorldConfig.get(world);
            if (!worldConfig.MAP_ENABLED) {
                return ArgumentParseResult.failure(new MapWorldParseException(input, MapWorldParseException.FailureReason.MAP_NOT_ENABLED));
            }

            inputQueue.remove();
            return ArgumentParseResult.success(commandContext.get(Commands.PLUGIN_INSTANCE_KEY).worldManager().getWorld(world));
        }

        @Override
        public List<String> suggestions(final CommandContext<C> commandContext, final String input) {
            return commandContext.get(Commands.PLUGIN_INSTANCE_KEY).worldManager().worlds().values().stream()
                .map(MapWorld::name)
                .toList();
        }

    }


    public static final class MapWorldParseException extends IllegalArgumentException {

        private static final long serialVersionUID = 3072715326923004782L;

        private final String input;
        private final FailureReason reason;

        /**
         * Construct a new MapWorldParseException
         *
         * @param input  Input
         * @param reason Failure reason
         */
        public MapWorldParseException(
            final String input,
            final FailureReason reason
        ) {
            this.input = input;
            this.reason = reason;
        }

        @Override
        public String getMessage() {
            return switch (this.reason) {
                case NO_SUCH_WORLD -> MiniMessage.miniMessage().stripTokens(Lang.NO_SUCH_WORLD.replace("<world>", this.input));
                case MAP_NOT_ENABLED -> MiniMessage.miniMessage().stripTokens(Lang.MAP_NOT_ENABLED_FOR_WORLD.replace("<world>", this.input));
            };
        }

        public enum FailureReason {
            NO_SUCH_WORLD, MAP_NOT_ENABLED
        }

    }

}
