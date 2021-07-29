package net.pl3x.map.plugin.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.WorldConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;

public class Network {
    public static final String CHANNEL = "pl3xmap:pl3xmap";

    public static void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(Pl3xMapPlugin.getInstance(), Network.CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(Pl3xMapPlugin.getInstance(), Network.CHANNEL,
                (channel, player, bytes) -> {
                    ByteArrayDataInput in = in(bytes);
                    int action = in.readInt();
                    switch (action) {
                        case Constants.GET_MAP_URL -> Network.sendMapUrl(player);
                        case Constants.GET_MAP_DATA -> {
                            int id = in.readInt();
                            Network.sendMapData(player, id);
                        }
                    }
                }
        );
    }

    public static void unregister() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(Pl3xMapPlugin.getInstance(), Network.CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(Pl3xMapPlugin.getInstance(), Network.CHANNEL);
    }

    private static void sendMapUrl(Player player) {
        ByteArrayDataOutput out = out();

        out.writeInt(Constants.GET_MAP_URL);
        out.writeInt(Constants.RESPONSE_SUCCESS);
        out.writeUTF(Config.WEB_ADDRESS);

        send(player, out);
    }

    private static void sendMapData(Player player, int id) {
        ByteArrayDataOutput out = out();
        out.writeInt(Constants.GET_MAP_DATA);

        MapView map = map(id);
        if (map == null) {
            out.writeInt(Constants.ERROR_NO_SUCH_MAP);
            return;
        }

        World world = map.getWorld();
        if (world == null) {
            out.writeInt(Constants.ERROR_NO_SUCH_WORLD);
            return;
        }

        out.writeInt(Constants.RESPONSE_SUCCESS);
        out.writeInt(id);
        out.writeByte(getScale(map));
        out.writeInt(map.getCenterX());
        out.writeInt(map.getCenterZ());
        out.writeInt(WorldConfig.get(world).ZOOM_MAX);
        out.writeUTF(world.getName());

        send(player, out);
    }

    private static void send(Player player, ByteArrayDataOutput out) {
        player.sendPluginMessage(Pl3xMapPlugin.getInstance(), CHANNEL, out.toByteArray());
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
