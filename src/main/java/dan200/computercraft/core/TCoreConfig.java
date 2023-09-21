package dan200.computercraft.core;

/**
 * Replaces {@link CoreConfig} with a cut-down version.
 */
public final class TCoreConfig {
    private TCoreConfig() {
    }

    public static int maximumFilesOpen = 128;
    public static boolean disableLua51Features = false;
    public static String defaultComputerSettings = "";

    public static boolean httpEnabled = true;
    public static boolean httpWebsocketEnabled = true;
    public static int httpMaxRequests = 16;
    public static int httpMaxWebsockets = 4;
}
