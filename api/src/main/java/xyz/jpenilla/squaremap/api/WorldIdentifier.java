package xyz.jpenilla.squaremap.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Namespaced identifier used to query worlds from squaremap.
 *
 * <p>Mirrors Minecraft's {@code ResourceLocation}. This means the same rules apply regarding allowed characters.</p>
 *
 * @see <a href="https://minecraft.wiki/w/Resource_location#Legal_characters">https://minecraft.wiki/w/Resource_location#Legal_characters</a>
 */
@DefaultQualifier(NonNull.class)
public interface WorldIdentifier {
    /**
     * Gets the namespace string of this {@link WorldIdentifier}.
     *
     * @return namespace string
     */
    String namespace();

    /**
     * Gets the value string of this {@link WorldIdentifier}.
     *
     * @return value string
     */
    String value();

    /**
     * Get the string representation of this {@link WorldIdentifier}.
     *
     * @return string representation
     */
    String asString();

    /**
     * Create a new {@link WorldIdentifier} from the provided namespace and value strings.
     *
     * @param namespace namespace string
     * @param value     value string
     * @return new {@link WorldIdentifier}
     */
    static WorldIdentifier create(final String namespace, final String value) {
        return new WorldIdentifierImpl(namespace, value);
    }

    /**
     * Parse a colon separated identifier string into a new {@link WorldIdentifier}.
     *
     * @param identifierString identifier string
     * @return new {@link WorldIdentifier}
     */
    static WorldIdentifier parse(final String identifierString) {
        final String[] split = identifierString.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid format for WorldIdentifier string '" + identifierString + "', expected 'namespace:value'.");
        }
        return create(split[0], split[1]);
    }
}
