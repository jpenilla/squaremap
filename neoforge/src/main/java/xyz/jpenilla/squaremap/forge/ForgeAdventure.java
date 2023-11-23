package xyz.jpenilla.squaremap.forge;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@DefaultQualifier(NonNull.class)
public final class ForgeAdventure {
    public static net.minecraft.network.chat.Component toNative(final Component component) {
        final JsonElement tree = GsonComponentSerializer.gson().serializeToTree(component);
        return Objects.requireNonNull(net.minecraft.network.chat.Component.Serializer.fromJson(tree));
    }

    public static Component fromNative(final net.minecraft.network.chat.Component component) {
        return GsonComponentSerializer.gson().deserializeFromTree(
            net.minecraft.network.chat.Component.Serializer.toJsonTree(component)
        );
    }

    public static Audience commandSourceAudience(final CommandSourceStack stack) {
        return new Audience() {
            @Override
            public void sendMessage(final Identity identity, final Component message, final MessageType type) {
                stack.sendSystemMessage(toNative(message));
            }
        };
    }

    private static final Pattern LOCALIZATION_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?s");

    // From PaperAdventure
    public static final Supplier<ComponentFlattener> FLATTENER = Suppliers.memoize(() -> ComponentFlattener.basic().toBuilder()
        .complexMapper(TranslatableComponent.class, (translatable, consumer) -> {
            if (!Language.getInstance().has(translatable.key())) {
                for (final Translator source : GlobalTranslator.translator().sources()) {
                    if (source instanceof TranslationRegistry registry && registry.contains(translatable.key())) {
                        consumer.accept(GlobalTranslator.render(translatable, Locale.US));
                        return;
                    }
                }
            }
            final @NotNull String translated = Language.getInstance().getOrDefault(translatable.key());

            final Matcher matcher = LOCALIZATION_PATTERN.matcher(translated);
            final List<Component> args = translatable.args();
            int argPosition = 0;
            int lastIdx = 0;
            while (matcher.find()) {
                // append prior
                if (lastIdx < matcher.start()) {
                    consumer.accept(Component.text(translated.substring(lastIdx, matcher.start())));
                }
                lastIdx = matcher.end();

                final @Nullable String argIdx = matcher.group(1);
                // calculate argument position
                if (argIdx != null) {
                    try {
                        final int idx = Integer.parseInt(argIdx) - 1;
                        if (idx < args.size()) {
                            consumer.accept(args.get(idx));
                        }
                    } catch (final NumberFormatException ex) {
                        // ignore, drop the format placeholder
                    }
                } else {
                    final int idx = argPosition++;
                    if (idx < args.size()) {
                        consumer.accept(args.get(idx));
                    }
                }
            }

            // append tail
            if (lastIdx < translated.length()) {
                consumer.accept(Component.text(translated.substring(lastIdx)));
            }
        })
        .build());
}
