package xyz.jpenilla.squaremap.common.command.argument.parser;

import java.util.List;
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
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Components;
import xyz.jpenilla.squaremap.common.util.Util;

import static org.incendo.cloud.parser.ArgumentParseResult.failure;
import static org.incendo.cloud.parser.ArgumentParseResult.success;

@DefaultQualifier(NonNull.class)
public final class MapWorldParser<C> implements ArgumentParser<C, MapWorldInternal>, BlockingSuggestionProvider.Strings<C> {

    public static <C> ParserDescriptor<C, MapWorldInternal> mapWorldParser() {
        return ParserDescriptor.of(new MapWorldParser<>(), MapWorldInternal.class);
    }

    @Override
    public ArgumentParseResult<MapWorldInternal> parse(final CommandContext<C> commandContext, final CommandInput commandInput) {
        final String input = commandInput.readString();

        final @Nullable ResourceLocation key = ResourceLocation.tryParse(input);
        if (key == null || key.getPath().isEmpty()) {
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

        return success(commandContext.get(Commands.WORLD_MANAGER).getWorldIfEnabled(world).orElseThrow());
    }

    @Override
    public List<String> stringSuggestions(final CommandContext<C> commandContext, final CommandInput commandInput) {
        return commandContext.get(Commands.WORLD_MANAGER).worlds().stream()
            .flatMap(mapWorld -> {
                final WorldIdentifier identifier = mapWorld.identifier();
                if (!commandInput.remainingInput().isBlank() && identifier.namespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                    return Stream.of(identifier.value(), identifier.asString());
                }
                return Stream.of(identifier.asString());
            })
            .toList();
    }

    @SuppressWarnings("serial")
    public static final class MapWorldParseException extends IllegalArgumentException implements ComponentMessageThrowable {

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
