package xyz.jpenilla.squaremap.common.network;

public final class Constants {
    private Constants() {
    }

    public static final int SERVER_DATA = 0;
    public static final int MAP_DATA = 1;
    public static final int UPDATE_WORLD = 2;

    public static final int PROTOCOL = 3;

    public static final int RESPONSE_SUCCESS = 200;

    public static final int ERROR_NO_SUCH_MAP = -1;
    public static final int ERROR_NO_SUCH_WORLD = -2;
    public static final int ERROR_NOT_VANILLA_MAP = -3;
}
