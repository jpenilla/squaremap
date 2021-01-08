package net.pl3x.map.command.argument;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import net.pl3x.map.configuration.Lang;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class WorldArgument<C> extends CommandArgument<C, World> {

    protected WorldArgument(
            final boolean required,
            final String name,
            final String defaultValue,
            final BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider
    ) {
        super(required, name, new WorldParser<>(), defaultValue, World.class, suggestionsProvider);
    }

    public static <C> WorldArgument.Builder<C> newBuilder(final String name) {
        return new WorldArgument.Builder<>(name);
    }

    public static <C> CommandArgument<C, World> of(final String name) {
        return WorldArgument.<C>newBuilder(name).asRequired().build();
    }

    public static <C> CommandArgument<C, World> optional(final String name) {
        return WorldArgument.<C>newBuilder(name).asOptional().build();
    }

    public static <C> CommandArgument<C, World> optional(
            final String name,
            final World world
    ) {
        return WorldArgument.<C>newBuilder(name).asOptionalWithDefault(world.getName()).build();
    }

    public static final class Builder<C> extends CommandArgument.Builder<C, World> {

        protected Builder(final String name) {
            super(World.class, name);
        }

        @Override
        public CommandArgument<C, World> build() {
            return new WorldArgument<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider()
            );
        }

    }

    public static final class WorldParser<C> implements ArgumentParser<C, World> {

        @Override
        public ArgumentParseResult<World> parse(
                final CommandContext<C> commandContext,
                final Queue<String> inputQueue
        ) {
            String input = inputQueue.peek();
            World world;
            if (input == null) {
                if (commandContext.getSender() instanceof Player) {
                    world = ((Player) commandContext.getSender()).getWorld();
                } else {
                    return ArgumentParseResult.failure(new NullPointerException(Lang.WORLD_NOT_SPECIFIED));
                }
            } else {
                world = Bukkit.getWorld(input);
            }
            if (world != null) {
                inputQueue.remove();
                return ArgumentParseResult.success(world);
            }
            return ArgumentParseResult.failure(new IllegalArgumentException(
                    Lang.WORLD_NOT_FOUND
            ));
        }

        @Override
        public List<String> suggestions(
                final CommandContext<C> commandContext,
                final String input
        ) {
            return Bukkit.getWorlds()
                    .stream()
                    .map(World::getName)
                    .collect(Collectors.toList());
        }

    }

}
