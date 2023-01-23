package xyz.jpenilla.squaremap.paper;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.inject.SquaremapModulesBuilder;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.paper.data.PaperMapWorld;
import xyz.jpenilla.squaremap.paper.inject.module.PaperModule;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
public final class SquaremapPaperBootstrap extends JavaPlugin {
    private static final String TARGET_MINECRAFT_VERSION = Objects.requireNonNull(
        Objects.requireNonNull(
            Util.manifest(SquaremapPaperBootstrap.class),
            "Missing squaremap manifest"
        ).getMainAttributes().getValue("squaremap-target-minecraft-version"),
        "squaremap manifest missing 'squaremap-target-minecraft-version' attribute"
    );

    private @Nullable SquaremapPaper squaremapPaper;

    @Override
    public void onEnable() {
        if (!this.checkCompatibility()) {
            return;
        }
        final Injector injector = Guice.createInjector(
            SquaremapModulesBuilder.forPlatform(SquaremapPaper.class)
                .mapWorldFactory(PaperMapWorld.Factory.class)
                .withModule(new PaperModule(this))
                .build()
        );
        this.squaremapPaper = injector.getInstance(SquaremapPaper.class);
        this.squaremapPaper.init();
    }

    @Override
    public void onDisable() {
        if (this.squaremapPaper != null) {
            this.squaremapPaper.onDisable();
        }
    }

    private boolean checkCompatibility() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
        } catch (final ClassNotFoundException ex) {
            return this.incompatible("squaremap requires a Paper-based server to run. Get Paper from https://papermc.io/downloads");
        }
        if (!Bukkit.getMinecraftVersion().equals(TARGET_MINECRAFT_VERSION)) {
            if (CraftBukkitReflection.mojangMapped()) {
                this.logIncompatibilityMessage(
                    Level.WARNING,
                    "This squaremap jar was built for Minecraft " + TARGET_MINECRAFT_VERSION + ".",
                    "It will attempt to load even though the current Minecraft version is " + Bukkit.getMinecraftVersion(),
                    "as the environment is Mojang-mapped. This may or may not work, prefer running",
                    "the correct squaremap version for your server. Keep in mind only the latest",
                    "build of squaremap running on the intended Minecraft version is officially supported."
                );
                return true;
            }
            return this.incompatible(
                "This squaremap jar was built for Minecraft " + TARGET_MINECRAFT_VERSION + ".",
                "It cannot run in the current environment (Minecraft " + Bukkit.getMinecraftVersion() + ")."
            );
        }
        return true;
    }

    private boolean incompatible(final String... messages) {
        this.logIncompatibilityMessage(Level.SEVERE, messages);
        this.getServer().getPluginManager().disablePlugin(this);
        return false;
    }

    private void logIncompatibilityMessage(final Level level, final String... messages) {
        final StringBuilder builder = new StringBuilder("\n**********************************************\n");
        for (final String msg : messages) {
            builder.append("** ").append(msg).append("\n");
        }
        builder.append("**********************************************\n");
        this.getLogger().log(level, builder.toString());
    }
}
