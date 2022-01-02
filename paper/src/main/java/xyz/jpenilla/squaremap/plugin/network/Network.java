package xyz.jpenilla.squaremap.plugin.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.map.CraftMapRenderer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.network.Constants;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.listener.PlayerListener;

public final class Network {
    public static final String CHANNEL = "squaremap:client";

    public static void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(SquaremapPlugin.getInstance(), Network.CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(
            SquaremapPlugin.getInstance(),
            Network.CHANNEL,
            (channel, player, bytes) -> {
                ByteArrayDataInput in = in(bytes);
                int action = in.readInt();
                switch (action) {
                    case Constants.SERVER_DATA -> {
                        PlayerListener.clientUsers.add(player.getUniqueId());
                        Network.sendServerData(player);
                    }
                    case Constants.MAP_DATA -> {
                        int id = in.readInt();
                        Network.sendMapData(player, id);
                    }
                }
            }
        );
    }

    public static void unregister() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(SquaremapPlugin.getInstance(), Network.CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(SquaremapPlugin.getInstance(), Network.CHANNEL);
    }

    public static void sendServerData(Player player) {
        ByteArrayDataOutput out = out();

        out.writeInt(Constants.SERVER_DATA);
        out.writeInt(Constants.PROTOCOL);
        out.writeInt(Constants.RESPONSE_SUCCESS);

        out.writeUTF(Config.WEB_ADDRESS);

        final Map<WorldIdentifier, MapWorldInternal> mapWorlds = SquaremapPlugin.getInstance().worldManager().worlds();
        out.writeInt(mapWorlds.size());

        mapWorlds.forEach(($, mapWorld) -> {
            out.writeUTF(mapWorld.identifier().asString());
            out.writeUTF(mapWorld.name());
            out.writeInt(mapWorld.config().ZOOM_MAX);
            out.writeInt(mapWorld.config().ZOOM_DEFAULT);
            out.writeInt(mapWorld.config().ZOOM_EXTRA);
        });

        out.writeUTF(player.getWorld().key().asString());

        send(player, out);
    }

    private static void sendMapData(Player player, int id) {
        ByteArrayDataOutput out = out();
        out.writeInt(Constants.MAP_DATA);
        out.writeInt(Constants.PROTOCOL);
        out.writeInt(Constants.RESPONSE_SUCCESS);

        MapView map = map(id);
        if (map == null) {
            out.writeInt(Constants.ERROR_NO_SUCH_MAP);
            out.writeInt(id);
            return;
        }

        World world = map.getWorld();
        if (world == null) {
            out.writeInt(Constants.ERROR_NO_SUCH_WORLD);
            out.writeInt(id);
            return;
        }

        for (MapRenderer renderer : map.getRenderers()) {
            if (!renderer.getClass().getName().equals(CraftMapRenderer.class.getName())) {
                out.writeInt(Constants.ERROR_NOT_VANILLA_MAP);
                out.writeInt(id);
                return;
            }
        }

        out.writeInt(id);
        out.writeByte(getScale(map));
        out.writeInt(map.getCenterX());
        out.writeInt(map.getCenterZ());
        out.writeUTF(world.key().asString());

        send(player, out);
    }

    public static void sendUpdateWorld(Player player) {
        ByteArrayDataOutput out = out();
        out.writeInt(Constants.UPDATE_WORLD);
        out.writeInt(Constants.PROTOCOL);
        out.writeInt(Constants.RESPONSE_SUCCESS);

        out.writeUTF(player.getWorld().getKey().asString());

        send(player, out);
    }

    private static void send(Player player, ByteArrayDataOutput out) {
        player.sendPluginMessage(SquaremapPlugin.getInstance(), CHANNEL, out.toByteArray());
    }

    @SuppressWarnings("UnstableApiUsage")
    private static ByteArrayDataOutput out() {
        return ByteStreams.newDataOutput();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static ByteArrayDataInput in(byte[] bytes) {
        return ByteStreams.newDataInput(bytes);
    }

    @SuppressWarnings("deprecation")
    private static MapView map(int id) {
        return Bukkit.getMap(id);
    }

    @SuppressWarnings("deprecation")
    private static byte getScale(MapView map) {
        return map.getScale().getValue();
    }
}
