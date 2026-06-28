package xyz.jpenilla.squaremap.fabric;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;

@DefaultQualifier(NonNull.class)
@Singleton
public final class FabricPlayerManager extends AbstractPlayerManager {
    private static final Codec<SquaremapPlayerData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(SquaremapPlayerData::hidden)
        ).apply(instance, SquaremapPlayerDataImpl::new)
    );
    private static final AttachmentType<SquaremapPlayerData> PLAYER_DATA = AttachmentRegistry.create(
        Identifier.parse("squaremap:player_data"),
        builder -> builder
            .initializer(SquaremapPlayerDataImpl::new)
            .persistent(CODEC)
            .copyOnDeath()
    );

    private static final String LEGACY_ROOT_KEY = "cardinal_components";
    private static final String LEGACY_COMPONENT_KEY = "squaremap:player_component";
    private static final String LEGACY_HIDDEN_KEY = "hidden";

    @Inject
    private FabricPlayerManager(final FabricServerAccess serverAccess) {
        super(serverAccess);
    }

    public void setupAttachments() {
        // Force static initialization of this class so Fabric's attachment type is registered before player data is loaded.
    }

    public static void migrateLegacyHidden(final ServerPlayer player, final ValueInput input) {
        if (player.hasAttached(PLAYER_DATA)) {
            return;
        }
        input.read(LEGACY_ROOT_KEY, CompoundTag.CODEC).flatMap(root -> root.getCompound(LEGACY_COMPONENT_KEY)).ifPresent(component -> {
            if (component.contains(LEGACY_HIDDEN_KEY)) {
                data(player).hidden(component.getBooleanOr(LEGACY_HIDDEN_KEY, false));
            }
        });
    }

    @Override
    public Component displayName(final ServerPlayer player) {
        return MinecraftServerAudiences.of(player.level().getServer()).asAdventure(player.getDisplayName());
    }

    @Override
    protected boolean persistentHidden(final ServerPlayer player) {
        return data(player).hidden();
    }

    @Override
    protected void persistentHidden(final ServerPlayer player, final boolean value) {
        data(player).hidden(value);
    }

    public interface SquaremapPlayerData {
        void hidden(boolean value);

        boolean hidden();
    }

    private static final class SquaremapPlayerDataImpl implements SquaremapPlayerData {
        private boolean hidden;

        SquaremapPlayerDataImpl() {
            this(false);
        }

        SquaremapPlayerDataImpl(final boolean hidden) {
            this.hidden = hidden;
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
        return player.getAttachedOrCreate(PLAYER_DATA);
    }
}
