package xyz.jpenilla.squaremap.forge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.ServerAccess;

@DefaultQualifier(NonNull.class)
@Singleton
public final class ForgePlayerManager extends AbstractPlayerManager {
    private static final ResourceLocation PLAYER_CAPABILITY_KEY = new ResourceLocation("squaremap:player_capability");
    private static final Capability<SquaremapPlayerCapability> PLAYER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    @Inject
    private ForgePlayerManager(final ServerAccess serverAccess) {
        super(serverAccess);
    }

    @Override
    public Component displayName(final ServerPlayer player) {
        return ForgeAdventure.fromNative(player.getDisplayName());
    }

    @Override
    protected boolean persistentHidden(final ServerPlayer player) {
        return cap(player).hidden();
    }

    @Override
    protected void persistentHidden(final ServerPlayer player, final boolean value) {
        cap(player).hidden(value);
    }

    public void setupCapabilities() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterCapabilitiesEvent event) -> event.register(SquaremapPlayerCapability.class));
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, (AttachCapabilitiesEvent<Entity> event) -> {
            if (!(event.getObject() instanceof ServerPlayer)) {
                return;
            }
            event.addCapability(PLAYER_CAPABILITY_KEY, new SquaremapPlayerCapabilityProvider());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent.Clone event) -> {
            if ((!(event.getOriginal() instanceof ServerPlayer original) || !(event.getEntity() instanceof ServerPlayer entity))) {
                return;
            }
            cap(entity).copyFrom(cap(original));
        });
    }

    public interface SquaremapPlayerCapability {
        void hidden(boolean value);

        boolean hidden();

        default void copyFrom(final SquaremapPlayerCapability capability) {
            this.hidden(capability.hidden());
        }
    }

    private static final class SquaremapPlayerCapabilityProvider implements SquaremapPlayerCapability, ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

        private final LazyOptional<SquaremapPlayerCapability> holder = LazyOptional.of(() -> this);
        private boolean hidden;

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag tag = new CompoundTag();
            tag.putBoolean("hidden", this.hidden());
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundTag arg) {
            this.hidden(arg.getBoolean("hidden"));
        }

        @Override
        public <T> LazyOptional<T> getCapability(final Capability<T> capability, final @Nullable Direction arg) {
            return PLAYER_CAPABILITY.orEmpty(capability, this.holder);
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

    private static SquaremapPlayerCapability cap(final ServerPlayer player) {
        return player.getCapability(PLAYER_CAPABILITY).orElseThrow(() -> new IllegalStateException("No " + SquaremapPlayerCapability.class.getName() + " for player " + player.getGameProfile().getName()));
    }
}
