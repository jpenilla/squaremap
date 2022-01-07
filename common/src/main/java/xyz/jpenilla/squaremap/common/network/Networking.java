package xyz.jpenilla.squaremap.common.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Util;

public final class Networking {
    public static final ResourceLocation CHANNEL = new ResourceLocation("squaremap:client");
    public static final Set<UUID> CLIENT_USERS = ConcurrentHashMap.newKeySet();

    public static void handleIncoming(
        final byte[] bytes,
        final ServerPlayer serverPlayer,
        final Predicate<MapItemSavedData> vanillaMap
    ) {
        final ByteArrayDataInput in = in(bytes);
        final int action = in.readInt();
        switch (action) {
            case Constants.SERVER_DATA -> {
                CLIENT_USERS.add(serverPlayer.getUUID());
                Networking.sendServerData(serverPlayer);
            }
            case Constants.MAP_DATA -> {
                int id = in.readInt();
                Networking.sendMapData(serverPlayer, id, vanillaMap);
            }
        }
    }

    public static void sendServerData(final ServerPlayer player) {
        ByteArrayDataOutput out = out();

        out.writeInt(Constants.SERVER_DATA);
        out.writeInt(Constants.PROTOCOL);
        out.writeInt(Constants.RESPONSE_SUCCESS);

        out.writeUTF(Config.WEB_ADDRESS);

        final Map<WorldIdentifier, MapWorldInternal> mapWorlds = SquaremapCommon.instance().platform().worldManager().worlds();
        out.writeInt(mapWorlds.size());

        mapWorlds.forEach(($, mapWorld) -> {
            out.writeUTF(mapWorld.identifier().asString());
            out.writeUTF(SquaremapCommon.instance().platform().webNameForWorld(mapWorld.serverLevel()));
            out.writeInt(mapWorld.config().ZOOM_MAX);
            out.writeInt(mapWorld.config().ZOOM_DEFAULT);
            out.writeInt(mapWorld.config().ZOOM_EXTRA);
        });

        out.writeUTF(player.getLevel().dimension().location().toString());

        send(player, out);
    }

    private static void sendMapData(
        final ServerPlayer player,
        final int id,
        final Predicate<MapItemSavedData> vanillaMap
    ) {
        ByteArrayDataOutput out = out();
        out.writeInt(Constants.MAP_DATA);
        out.writeInt(Constants.PROTOCOL);
        out.writeInt(Constants.RESPONSE_SUCCESS);

        final MapItemSavedData mapData = player.level.getMapData("map_" + id);
        if (mapData == null) {
            out.writeInt(Constants.ERROR_NO_SUCH_MAP);
            out.writeInt(id);
            return;
        }

        final @Nullable ServerLevel world = SquaremapCommon.instance().platform()
            .level(Util.worldIdentifier(mapData.dimension.location()));
        if (world == null) {
            out.writeInt(Constants.ERROR_NO_SUCH_WORLD);
            out.writeInt(id);
            return;
        }

        if (!vanillaMap.test(mapData)) {
            out.writeInt(Constants.ERROR_NOT_VANILLA_MAP);
            out.writeInt(id);
            return;
        }

        out.writeInt(id);
        out.writeByte(mapData.scale);
        out.writeInt(mapData.x);
        out.writeInt(mapData.z);
        out.writeUTF(world.dimension().location().toString());

        send(player, out);
    }

    public static void worldChanged(final ServerPlayer player) {
        if (!CLIENT_USERS.contains(player.getUUID())) {
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
    private static ByteArrayDataInput in(byte[] bytes) {
        return ByteStreams.newDataInput(bytes);
    }
}
