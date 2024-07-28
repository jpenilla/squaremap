package xyz.jpenilla.squaremap.forge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.ServerAccess;

@DefaultQualifier(NonNull.class)
@Singleton
public final class ForgePlayerManager extends AbstractPlayerManager {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "squaremap");
    private static final Supplier<AttachmentType<SquaremapPlayerData>> PLAYER_DATA = ATTACHMENT_TYPES.register(
        "player_data",
        () -> AttachmentType.<CompoundTag, SquaremapPlayerData>serializable(SquaremapPlayerDataImpl::new)
            .copyOnDeath()
            .build()
    );

    private final IEventBus modEventBus;

    @Inject
    private ForgePlayerManager(final ServerAccess serverAccess, final IEventBus modEventBus) {
        super(serverAccess);
        this.modEventBus = modEventBus;
    }

    @Override
    public Component displayName(final ServerPlayer player) {
        final MinecraftServerAudiences audiences = MinecraftServerAudiences.of(player.getServer());
        return Optional.ofNullable(player.getDisplayName())
            .map(audiences::asAdventure)
            .orElseGet(() -> audiences.asAdventure(player.getName()));
    }

    @Override
    protected boolean persistentHidden(final ServerPlayer player) {
        return data(player).hidden();
    }

    @Override
    protected void persistentHidden(final ServerPlayer player, final boolean value) {
        data(player).hidden(value);
    }

    public void setupCapabilities() {
        ATTACHMENT_TYPES.register(this.modEventBus);
    }

    public interface SquaremapPlayerData extends INBTSerializable<CompoundTag> {
        void hidden(boolean value);

        boolean hidden();
    }

    private static final class SquaremapPlayerDataImpl implements SquaremapPlayerData {

        private boolean hidden;

        @Override
        public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
            final CompoundTag tag = new CompoundTag();
            tag.putBoolean("hidden", this.hidden());
            return tag;
        }

        @Override
        public void deserializeNBT(final HolderLookup.Provider provider, final CompoundTag arg) {
            this.hidden(arg.getBoolean("hidden"));
        }

        @Override
        public void hidden(final boolean value) {
            this.hidden = value;
        }

        @Override
        public boolean hidden() {
            return this.hidden;
        }
    }

    private static SquaremapPlayerData data(final ServerPlayer player) {
        return player.getData(PLAYER_DATA);
    }
}
