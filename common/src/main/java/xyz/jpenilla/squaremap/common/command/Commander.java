package xyz.jpenilla.squaremap.common.command;

import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface Commander extends Audience {
    boolean hasPermission(String permission);
}
