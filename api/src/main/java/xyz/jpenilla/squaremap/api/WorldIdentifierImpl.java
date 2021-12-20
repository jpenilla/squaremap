package xyz.jpenilla.squaremap.api;

import java.util.Objects;
import java.util.function.IntPredicate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
final class WorldIdentifierImpl implements WorldIdentifier {
    private static final IntPredicate NAMESPACE_PREDICATE = value -> value == '_' || value == '-' || (value >= 'a' && value <= 'z') || (value >= '0' && value <= '9') || value == '.';
    private static final IntPredicate VALUE_PREDICATE = value -> value == '_' || value == '-' || (value >= 'a' && value <= 'z') || (value >= '0' && value <= '9') || value == '/' || value == '.';

    private final String namespace;
    private final String value;

    WorldIdentifierImpl(
        final String namespace,
        final String value
    ) {
        if (fails(NAMESPACE_PREDICATE, namespace)) {
            throw new IllegalArgumentException(String.format("Non [a-z0-9_.-] character in namespace of WorldIdentifier[%s]", asString(namespace, value)));
        }
        if (fails(VALUE_PREDICATE, value)) {
            throw new IllegalArgumentException(String.format("Non [a-z0-9/._-] character in value of WorldIdentifier[%s]", asString(namespace, value)));
        }
        this.namespace = namespace;
        this.value = value;
    }

    @Override
    public String namespace() {
        return this.namespace;
    }

    @Override
    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final WorldIdentifierImpl that = (WorldIdentifierImpl) o;
        return this.namespace.equals(that.namespace) && this.value.equals(that.value);
    }

    @Override
    public String toString() {
        return "WorldIdentifierImpl[" + this.asString() + ']';
    }

    @Override
    public String asString() {
        return asString(this.namespace, this.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.namespace, this.value);
    }

    private static String asString(final String namespace, final String value) {
        return namespace + ':' + value;
    }

    private static boolean fails(final IntPredicate test, final @NonNull String key) {
        for (int i = 0, length = key.length(); i < length; i++) {
            if (!test.test(key.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
