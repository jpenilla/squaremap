package org.incendo.cloud.paper;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.configuration.PluginMeta;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CloudCapability;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.SenderMapperHolder;
import org.incendo.cloud.brigadier.BrigadierManagerHolder;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.BukkitCommandContextKeys;
import org.incendo.cloud.bukkit.BukkitDefaultCaptionsProvider;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.bukkit.internal.BukkitHelper;
import org.incendo.cloud.bukkit.parser.location.Location2DParser;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;

import static org.incendo.cloud.paper.parser.KeyedWorldParser.keyedWorldParser;

@SuppressWarnings("UnstableApiUsage")
public final class SquaremapPaperCommandManager<C> extends CommandManager<C> implements SenderMapperHolder<CommandSourceStack, C>,
    PluginMetaHolder, BrigadierManagerHolder<C, CommandSourceStack> {
    private final Plugin plugin;
    private final SenderMapper<CommandSourceStack, C> senderMapper;

    public SquaremapPaperCommandManager(
        final Plugin plugin,
        final ExecutionCoordinator<C> executionCoordinator,
        final SenderMapper<CommandSourceStack, C> senderMapper
    ) {
        super(executionCoordinator, CommandRegistrationHandler.nullCommandRegistrationHandler());
        this.plugin = plugin;
        this.senderMapper = senderMapper;

        this.commandRegistrationHandler(new ModernPaperBrigadier<>(
            CommandSourceStack.class,
            this,
            senderMapper,
            this::lockRegistration
        ));

        CloudBukkitCapabilities.CAPABLE.forEach(this::registerCapability);
        this.registerCapability(CloudCapability.StandardCapabilities.ROOT_COMMAND_DELETION);

        this.registerDefaultExceptionHandlers();
        this.captionRegistry().registerProvider(new BukkitDefaultCaptionsProvider<>());

        this.registerCommandPreProcessor(ctx -> ctx.commandContext().store(
            BukkitCommandContextKeys.BUKKIT_COMMAND_SENDER,
            this.senderMapper.reverse(ctx.commandContext().sender()).getSender()
        ));
        this.registerCommandPreProcessor(new PaperCommandPreprocessor<>(
            this,
            this.senderMapper,
            commandSourceStack -> {
                final Entity executor = commandSourceStack.getExecutor();
                if (executor != null) {
                    return executor;
                }
                return commandSourceStack.getSender();
            }
        ));

        this.parserRegistry()
            .registerParser(keyedWorldParser())
            .registerParser(Location2DParser.location2DParser())
            .registerParser(SinglePlayerSelectorParser.<C>singlePlayerSelectorParser());

        ((ModernPaperBrigadier<CommandSourceStack, C>) this.commandRegistrationHandler()).registerPlugin(plugin);
        BukkitHelper.ensurePluginEnabledOrEnabling(plugin);
    }

    @Override
    public boolean hasPermission(final @NonNull C sender, final @NonNull String permission) {
        return this.senderMapper.reverse(sender).getSender().hasPermission(permission);
    }

    @Override
    public @NonNull SenderMapper<CommandSourceStack, C> senderMapper() {
        return this.senderMapper;
    }

    private void registerDefaultExceptionHandlers() {
        this.registerDefaultExceptionHandlers(
            triplet -> this.senderMapper.reverse(triplet.first().sender()).getSender().sendMessage(Component.text(
                triplet.first().formatCaption(triplet.second(), triplet.third()),
                NamedTextColor.RED
            )),
            pair -> this.plugin.getLogger().log(Level.SEVERE, pair.first(), pair.second())
        );
    }

    @Override
    public Plugin owningPlugin() {
        return this.plugin;
    }

    @Override
    public PluginMeta owningPluginMeta() {
        return this.plugin.getPluginMeta();
    }

    @Override
    public boolean hasBrigadierManager() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull CloudBrigadierManager<C, ? extends CommandSourceStack> brigadierManager() {
        return ((BrigadierManagerHolder<C, CommandSourceStack>) this.commandRegistrationHandler()).brigadierManager();
    }
}
