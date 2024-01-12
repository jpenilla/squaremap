package xyz.jpenilla.squaremap.common.command;

import cloud.commandframework.brigadier.BrigadierManagerHolder;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import io.leangen.geantyref.TypeToken;
import net.minecraft.commands.arguments.DimensionArgument;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.argument.parser.LevelParser;
import xyz.jpenilla.squaremap.common.command.argument.parser.MapWorldParser;

@DefaultQualifier(NonNull.class)
public final class BrigadierSetup {
    private BrigadierSetup() {
    }

    public static void setup(final BrigadierManagerHolder<Commander, ?> manager) {
        final CloudBrigadierManager<Commander, ?> brigManager = manager.brigadierManager();

        brigManager.registerMapping(
            new TypeToken<MapWorldParser<Commander>>() {},
            builder -> builder.toConstant(DimensionArgument.dimension()).cloudSuggestions()
        );
        brigManager.registerMapping(
            new TypeToken<LevelParser<Commander>>() {},
            builder -> builder.toConstant(DimensionArgument.dimension()).cloudSuggestions()
        );
    }
}
