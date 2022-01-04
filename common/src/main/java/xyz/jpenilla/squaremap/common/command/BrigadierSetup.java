package xyz.jpenilla.squaremap.common.command;

import cloud.commandframework.brigadier.BrigadierManagerHolder;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import io.leangen.geantyref.TypeToken;
import net.minecraft.commands.arguments.DimensionArgument;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.argument.LevelArgument;
import xyz.jpenilla.squaremap.common.command.argument.MapWorldArgument;

import static java.util.Objects.requireNonNull;

@DefaultQualifier(NonNull.class)
public final class BrigadierSetup {
    private BrigadierSetup() {
    }

    public static void setup(final BrigadierManagerHolder<Commander> manager) {
        final @Nullable CloudBrigadierManager<Commander, ?> brigManager = manager.brigadierManager();
        requireNonNull(brigManager);

        brigManager.setNativeNumberSuggestions(false);
        brigManager.registerMapping(
            new TypeToken<MapWorldArgument.Parser<Commander>>() {},
            builder -> builder.toConstant(DimensionArgument.dimension()).cloudSuggestions()
        );
        brigManager.registerMapping(
            new TypeToken<LevelArgument.Parser<Commander>>() {},
            builder -> builder.toConstant(DimensionArgument.dimension()).cloudSuggestions()
        );
    }
}
