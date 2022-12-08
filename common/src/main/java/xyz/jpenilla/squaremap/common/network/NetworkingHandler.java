package xyz.jpenilla.squaremap.common.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Util;

/**
 * Networking handler for squaremap-client compatibility
 */
@DefaultQualifier(NonNull.class)
public final class NetworkingHandler {
    public static final ResourceLocation CHANNEL = new ResourceLocation("squaremap:client");

    private final WorldManager worldManager;
    private final ServerAccess serverAccess;
    private final Set<UUID> clientUsers;

    @Inject
    public NetworkingHandler(
        final WorldManager worldManager,
        final ServerAccess serverAccess
    ) {
        this.worldManager = worldManager;
        this.serverAccess = serverAccess;
        this.clientUsers = ConcurrentHashMap.newKeySet();
    }

    public void onDisconnect(final UUID playerUuid) {
        this.clientUsers.remove(playerUuid);
    }

    public void handleIncoming(
        final ServerPlayer player,
        final byte[] data,
        final Predicate<MapItemSavedData> isVanillaMap
    ) {
        final ByteArrayDataInput in = in(data);
        final int action = in.readInt();
        switch (action) {
            case Constants.SERVER_DATA -> {
                this.clientUsers.add(player.getUUID());
                this.sendServerData(player);
            }
            case Constants.MAP_DATA -> {
                final int id = in.readInt();
                this.sendMapData(player, id, isVanillaMap);
            }
        }
    }

    private void sendServerData(final ServerPlayer player) {
        final ByteArrayDataOutput out = out();

        out.writeInt(Constants.SERVER_DATA);
        out.writeInt(Constants.PROTOCOL);
        out.writeInt(Constants.RESPONSE_SUCCESS);

        out.writeUTF(Config.WEB_ADDRESS);

        final Collection<MapWorldInternal> mapWorlds = this.worldManager.worlds();
        out.writeInt(mapWorlds.size());

        for (final MapWorldInternal mapWorld : mapWorlds) {
            out.writeUTF(mapWorld.identifier().asString());
            out.writeUTF(Util.levelWebName(mapWorld.serverLevel()));
            out.writeInt(mapWorld.config().ZOOM_MAX);
            out.writeInt(mapWorld.config().ZOOM_DEFAULT);
            out.writeInt(mapWorld.config().ZOOM_EXTRA);
        }

        out.writeUTF(player.getLevel().dimension().location().toString());

        send(player, out);
    }

    private void sendMapData(
        final ServerPlayer player,
        final int id,
        final Predicate<MapItemSavedData> isVanillaMap
    ) {
        final ByteArrayDataOutput data = this.mapData(player, id, isVanillaMap);
        send(player, data);
    }

    private ByteArrayDataOutput mapData(
        final ServerPlayer player,
        final int id,
        final Predicate<MapItemSavedData> isVanillaMap
    ) {
        final ByteArrayDataOutput out = out();
        out.writeInt(Constants.MAP_DATA);
        out.writeInt(Constants.PROTOCOL);

        final @Nullable MapItemSavedData mapData = player.level.getMapData(MapItem.makeKey(id));
        if (mapData == null) {
            out.writeInt(Constants.ERROR_NO_SUCH_MAP);
            out.writeInt(id);
            return out;
        }

        final @Nullable ServerLevel world = this.serverAccess.level(Util.worldIdentifier(mapData.dimension.location()));
        if (world == null) {
            out.writeInt(Constants.ERROR_NO_SUCH_WORLD);
            out.writeInt(id);
            return out;
        }

        if (!isVanillaMap.test(mapData)) {
            out.writeInt(Constants.ERROR_NOT_VANILLA_MAP);
            out.writeInt(id);
            return out;
        }

        out.writeInt(Constants.RESPONSE_SUCCESS);
        out.writeInt(id);
        out.writeByte(mapData.scale);
        out.writeInt(mapData.centerX);
        out.writeInt(mapData.centerZ);
        out.writeUTF(world.dimension().location().toString());

        return out;
    }

    public void worldChanged(final ServerPlayer player) {
        if (!this.clientUsers.contains(player.getUUID())) {
            return;
        }

        final ByteArrayDataOutput out = out();
        out.writeInt(Constants.UPDATE_WORLD);
        out.writeInt(Constants.PROTOCOL);
        out.writeInt(Constants.RESPONSE_SUCCESS);

        out.writeUTF(player.getLevel().dimension().location().toString());

        send(player, out);
    }

    private static void send(final ServerPlayer player, final ByteArrayDataOutput out) {
        final ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(
            CHANNEL,
            new FriendlyByteBuf(Unpooled.wrappedBuffer(out.toByteArray()))
        );
        player.connection.send(packet);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static ByteArrayDataOutput out() {
        return ByteStreams.newDataOutput();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static ByteArrayDataInput in(final byte[] bytes) {
        return ByteStreams.newDataInput(bytes);
    }
}
