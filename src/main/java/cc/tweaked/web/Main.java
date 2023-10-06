package cc.tweaked.web;

import cc.tweaked.copycat.CopyCatComputer;
import cc.tweaked.copycat.js.Callbacks;
import cc.tweaked.copycat.js.ConfigGroup;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.CoreConfig;
import org.teavm.jso.browser.Window;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static String CORS_PROXY = "https://copy-cat-cors.vercel.app/?{}";
    public static int computerTermWidth = 51;
    public static int computerTermHeight = 19;

    private static long ticks;

    public static void main(String[] args) {
        setupConfig();

        var context = ComputerContext.builder(EmulatorEnvironment.INSTANCE).build();
        List<CopyCatComputer> computers = new ArrayList<>();

        Callbacks.setup(access -> {
            var wrapper = new CopyCatComputer(context, access);
            computers.add(wrapper);
            return wrapper;
        });

        Window.setInterval(() -> {
            ticks++;
            var iterator = computers.iterator();
            while (iterator.hasNext()) {
                var wrapper = iterator.next();
                if (wrapper.tick()) iterator.remove();
            }
        }, 50);
    }

    public static long getTicks() {
        return ticks;
    }

    private static void setupConfig() {
        ConfigGroup general = Callbacks.config("ComputerCraft", null);

        general.addInt("maximum_open_files", "Maximum open files", CoreConfig.maximumFilesOpen, 0, Integer.MAX_VALUE,
            "Set how many files a computer can have open at the same time. Set to 0 for unlimited.",
            x -> CoreConfig.maximumFilesOpen = x
        );

        general.addBoolean("disable_lua51_features", "Disable Lua 5.1 features", CoreConfig.disableLua51Features,
            "Set this to true to disable Lua 5.1 functions that will be removed in a future " +
                "update. Useful for ensuring forward compatibility of your programs now.",
            x -> CoreConfig.disableLua51Features = x
        );

        general.addString("default_computer_settings", "Default computer settings", CoreConfig.defaultComputerSettings,
            "A comma separated list of default system settings to set on new computers. Example: " +
                "\"shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false\" will disable all autocompletion",
            x -> CoreConfig.defaultComputerSettings = x
        );

        ConfigGroup terminal = Callbacks.config("Terminal", "Configure the terminal display");

        terminal.addInt("terminal.width", "Width", computerTermWidth, 1, 100,
            "The width of the computer's terminal",
            x -> computerTermWidth = x
        );

        terminal.addInt("terminal.height", "Height", computerTermHeight, 1, 100,
            "The height of the computer's terminal",
            a -> computerTermHeight = a
        );

        ConfigGroup http = Callbacks.config("HTTP API", "Controls the HTTP API");

        http.addBoolean("http.enabled", "Enabled", CoreConfig.httpEnabled,
            "Enable the \"http\" API on Computers (see \"http_whitelist\" and \"http_blacklist\" for " +
                "more fine grained control than this)",
            x -> CoreConfig.httpEnabled = x
        );

        http.addBoolean("http.websocket_enabled", "Websocket enabled", CoreConfig.httpWebsocketEnabled,
            "Enable use of http websockets. This requires the \"http_enable\" option to also be true.",
            x -> CoreConfig.httpWebsocketEnabled = x
        );

        http.addInt("http.max_requests", "Maximum concurrent requests", CoreConfig.httpMaxRequests, 0, Integer.MAX_VALUE,
            "The number of http requests a computer can make at one time. Additional requests " +
                "will be queued, and sent when the running requests have finished. Set to 0 for unlimited.",
            x -> CoreConfig.httpMaxRequests = x
        );

        http.addInt("http.max_websockets", "Maximum concurrent websockets", CoreConfig.httpMaxWebsockets, 1, Integer.MAX_VALUE,
            "The number of websockets a computer can have open at one time. Set to 0 for unlimited.",
            x -> CoreConfig.httpMaxWebsockets = x
        );

        http.addString("http.proxy", "Proxy", CORS_PROXY,
            "The proxy to use in order to bypass Cross-Origin protection (aka CORS). This allows you to make requests to " +
                "any site, but does involve sending all headers to another site first. Set to empty or \"{}\" to disable.",
            x -> CORS_PROXY = x
        );
    }
}
