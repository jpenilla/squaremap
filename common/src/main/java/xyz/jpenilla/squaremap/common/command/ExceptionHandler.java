package xyz.jpenilla.squaremap.common.command;

import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.util.ComponentMessageThrowable;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.exception.handling.ExceptionContext;
import org.incendo.cloud.exception.parsing.ParserException;
import org.incendo.cloud.util.TypeUtils;
import org.spongepowered.configurate.util.NamingSchemes;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.copyToClipboard;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static org.incendo.cloud.exception.handling.ExceptionHandler.unwrappingHandler;
import static xyz.jpenilla.squaremap.common.util.Components.highlightSpecialCharacters;
import static xyz.jpenilla.squaremap.common.util.Components.placeholder;

@DefaultQualifier(NonNull.class)
final class ExceptionHandler {
    @Inject
    private ExceptionHandler() {
    }

    public void registerExceptionHandlers(final CommandManager<Commander> manager) {
        manager.exceptionController()
            .registerHandler(CommandExecutionException.class, this::commandExecution)
            .registerHandler(CommandExecutionException.class, unwrappingHandler(CommandCompleted.class))
            .registerHandler(CommandCompleted.class, this::commandCompleted)
            .registerHandler(NoPermissionException.class, this::noPermission)
            .registerHandler(ArgumentParseException.class, this::argumentParsing)
            .registerHandler(InvalidCommandSenderException.class, this::invalidSender)
            .registerHandler(InvalidSyntaxException.class, this::invalidSyntax);
    }

    private void commandCompleted(final ExceptionContext<Commander, CommandCompleted> ctx) {
        final @Nullable Component message = ctx.exception().componentMessage();
        if (message != null) {
            decorateAndSend(ctx.context().sender(), message);
        }
    }

    private void commandExecution(final ExceptionContext<Commander, CommandExecutionException> ctx) {
        final Throwable cause = ctx.exception().getCause();

        Logging.logger().warn("An unexpected error occurred during command execution", cause);

        final TextComponent.Builder message = text();
        message.append(Messages.COMMAND_EXCEPTION_COMMAND_EXECUTION);
        if (ctx.context().sender().hasPermission("squaremap.command-exception-stacktrace")) {
            decorateWithHoverStacktrace(message, cause);
        }
        decorateAndSend(ctx.context().sender(), message);
    }

    private void noPermission(final ExceptionContext<Commander, NoPermissionException> ctx) {
        decorateAndSend(ctx.context().sender(), Messages.COMMAND_EXCEPTION_NO_PERMISSION);
    }

    private void argumentParsing(final ExceptionContext<Commander, ArgumentParseException> ctx) {
        final Throwable cause = ctx.exception().getCause();
        final Supplier<Component> fallback = () -> Objects.requireNonNull(componentMessage(cause));
        final Component message;
        if (cause instanceof final ParserException parserException) {
            final TagResolver[] placeholders = Arrays.stream(parserException.captionVariables())
                .map(variable -> placeholder(NamingSchemes.SNAKE_CASE.coerce(variable.key()), variable.value()))
                .toArray(TagResolver[]::new);
            final String key = Messages.PARSER_EXCEPTION_MESSAGE_PREFIX + parserException.errorCaption().key().replace("argument.parse.failure.", "");
            @Nullable Component fromConfig;
            try {
                fromConfig = Messages.componentMessage(key).withPlaceholders(placeholders);
            } catch (final Exception ex) {
                Logging.logger().warn("Could not get message with key '{}'", key, ex);
                fromConfig = null;
            }
            message = fromConfig != null ? fromConfig : fallback.get();
        } else {
            message = fallback.get();
        }
        decorateAndSend(
            ctx.context().sender(),
            Messages.COMMAND_EXCEPTION_INVALID_ARGUMENT.withPlaceholders(placeholder("message", message))
        );
    }

    private void invalidSender(final ExceptionContext<Commander, InvalidCommandSenderException> ctx) {
        final Component message = Messages.COMMAND_EXCEPTION_INVALID_SENDER_TYPE.withPlaceholders(
            placeholder("required_sender_type", text(TypeUtils.simpleName(ctx.exception().requiredSender())))
        );
        decorateAndSend(ctx.context().sender(), message);
    }

    private void invalidSyntax(final ExceptionContext<Commander, InvalidSyntaxException> ctx) {
        final Component message = Messages.COMMAND_EXCEPTION_INVALID_SYNTAX.withPlaceholders(
            placeholder("correct_syntax", highlightSpecialCharacters(text("/%s".formatted(ctx.exception().correctSyntax())), WHITE))
        );
        decorateAndSend(ctx.context().sender(), message);
    }

    private static void decorateAndSend(final Audience audience, final ComponentLike componentLike) {
        final Component message = textOfChildren(
            Messages.COMMAND_PREFIX.asComponent()
                .hoverEvent(Messages.CLICK_FOR_HELP.asComponent())
                .clickEvent(runCommand("/%s help".formatted(Config.MAIN_COMMAND_LABEL))),
            componentLike
        );
        audience.sendMessage(message);
    }

    private static void decorateWithHoverStacktrace(final TextComponent.Builder message, final Throwable cause) {
        final StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));
        final String stackTrace = writer.toString().replaceAll("\t", "    ");
        final TextComponent.Builder hoverText = text();
        final @Nullable Component throwableMessage = componentMessage(cause);
        if (throwableMessage != null) {
            hoverText.append(throwableMessage)
                .append(newline())
                .append(newline());
        }
        hoverText.append(text(stackTrace))
            .append(newline())
            .append(text("    "))
            .append(Messages.CLICK_TO_COPY.asComponent().color(GRAY).decorate(ITALIC));

        message.hoverEvent(hoverText.build());
        message.clickEvent(copyToClipboard(stackTrace));
    }

    private static @Nullable Component componentMessage(final Throwable cause) {
        if (cause instanceof ComponentMessageThrowable || !(cause instanceof CommandSyntaxException commandSyntaxException)) {
            return ComponentMessageThrowable.getOrConvertMessage(cause);
        }

        // Fallback for when CommandSyntaxException isn't a ComponentMessageThrowable
        final net.minecraft.network.chat.Component component = ComponentUtils.fromMessage(commandSyntaxException.getRawMessage());
        return GsonComponentSerializer.gson().deserializeFromTree(
            ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow(JsonParseException::new)
        );
    }
}
