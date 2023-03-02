package xyz.jpenilla.squaremap.common.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.exceptions.parsing.ParserException;
import com.google.inject.Inject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import net.minecraft.network.chat.ComponentUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
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
import static xyz.jpenilla.squaremap.common.util.Components.highlightSpecialCharacters;
import static xyz.jpenilla.squaremap.common.util.Components.placeholder;

@DefaultQualifier(NonNull.class)
final class ExceptionHandler {
    @Inject
    private ExceptionHandler() {
    }

    public void registerExceptionHandlers(final CommandManager<Commander> manager) {
        manager.registerExceptionHandler(CommandExecutionException.class, this::commandExecution);
        manager.registerExceptionHandler(NoPermissionException.class, this::noPermission);
        manager.registerExceptionHandler(ArgumentParseException.class, this::argumentParsing);
        manager.registerExceptionHandler(InvalidCommandSenderException.class, this::invalidSender);
        manager.registerExceptionHandler(InvalidSyntaxException.class, this::invalidSyntax);
    }

    private void commandExecution(final Commander commander, final CommandExecutionException exception) {
        final Throwable cause = exception.getCause();

        if (cause instanceof CommandCompleted completed) {
            final @Nullable Component message = completed.componentMessage();
            if (message != null) {
                commander.sendMessage(message);
            }
            return;
        }

        Logging.logger().warn("An unexpected error occurred during command execution", cause);

        final TextComponent.Builder message = text();
        message.append(Messages.COMMAND_EXCEPTION_COMMAND_EXECUTION);
        if (commander.hasPermission("squaremap.command-exception-stacktrace")) {
            decorateWithHoverStacktrace(message, cause);
        }
        decorateAndSend(commander, message);
    }

    private void noPermission(final Commander commander, final NoPermissionException exception) {
        decorateAndSend(commander, Messages.COMMAND_EXCEPTION_NO_PERMISSION);
    }

    private void argumentParsing(final Commander commander, final ArgumentParseException exception) {
        final Throwable cause = exception.getCause();
        final Supplier<Component> fallback = () -> Objects.requireNonNull(componentMessage(cause));
        final Component message;
        if (cause instanceof final ParserException parserException) {
            final TagResolver[] placeholders = Arrays.stream(parserException.captionVariables())
                .map(variable -> placeholder(NamingSchemes.SNAKE_CASE.coerce(variable.getKey()), variable.getValue()))
                .toArray(TagResolver[]::new);
            final String key = Messages.PARSER_EXCEPTION_MESSAGE_PREFIX + parserException.errorCaption().getKey().replace("argument.parse.failure.", "");
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
            commander,
            Messages.COMMAND_EXCEPTION_INVALID_ARGUMENT.withPlaceholders(placeholder("message", message))
        );
    }

    private void invalidSender(final Commander commander, final InvalidCommandSenderException exception) {
        final Component message = Messages.COMMAND_EXCEPTION_INVALID_SENDER_TYPE.withPlaceholders(
            placeholder("required_sender_type", text(exception.getRequiredSender().getSimpleName()))
        );
        decorateAndSend(commander, message);
    }

    private void invalidSyntax(final Commander commander, final InvalidSyntaxException exception) {
        final Component message = Messages.COMMAND_EXCEPTION_INVALID_SYNTAX.withPlaceholders(
            placeholder("correct_syntax", highlightSpecialCharacters(text("/%s".formatted(exception.getCorrectSyntax())), WHITE))
        );
        decorateAndSend(commander, message);
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
        return GsonComponentSerializer.gson().deserializeFromTree(net.minecraft.network.chat.Component.Serializer.toJsonTree(component));
    }
}
