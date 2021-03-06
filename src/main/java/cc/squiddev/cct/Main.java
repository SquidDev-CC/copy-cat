package cc.squiddev.cct;

import cc.squiddev.cct.js.Callbacks;
import cc.squiddev.cct.js.ConfigGroup;
import dan200.computercraft.ComputerCraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static dan200.computercraft.ComputerCraft.computerTermHeight;
import static dan200.computercraft.ComputerCraft.computerTermWidth;

public class Main {
    public static String corsProxy = "https://copy-cat.squiddev.workers.dev/{}";
    private static final List<ComputerWrapper> computers = new ArrayList<>();
    private static long ticks;

    public static void main(String[] args) {
        setupConfig();
        Callbacks.setup(access -> {
            ComputerWrapper wrapper = new ComputerWrapper(access);
            computers.add(wrapper);
            return wrapper;
        });

        Callbacks.setInterval(() -> {
            ticks++;
            Iterator<ComputerWrapper> iterator = computers.iterator();
            while (iterator.hasNext()) {
                ComputerWrapper wrapper = iterator.next();
                if (wrapper.tick()) iterator.remove();
            }
        }, 50);
    }

    public static long getTicks() {
        return ticks;
    }

    private static void setupConfig() {
        ConfigGroup general = Callbacks.config("ComputerCraft", null);

        general.addInt("maximum_open_files", "Maximum open files", ComputerCraft.maximumFilesOpen, 0, Integer.MAX_VALUE,
            "Set how many files a computer can have open at the same time. Set to 0 for unlimited.",
            x -> ComputerCraft.maximumFilesOpen = x
        );

        general.addBoolean("disable_lua51_features", "Disable Lua 5.1 features", ComputerCraft.disableLua51Features,
            "Set this to true to disable Lua 5.1 functions that will be removed in a future " +
                "update. Useful for ensuring forward compatibility of your programs now.",
            x -> ComputerCraft.disableLua51Features = x
        );

        general.addString("default_computer_settings", "Default computer settings", ComputerCraft.defaultComputerSettings,
            "A comma separated list of default system settings to set on new computers. Example: " +
                "\"shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false\" will disable all autocompletion",
            x -> ComputerCraft.defaultComputerSettings = x
        );

        general.addBoolean("debug_enabled", "Debug enabled", ComputerCraft.debugEnable,
            "Enable Lua's debug library. This is sandboxed to each computer, so is generally safe to be used by players.",
            x -> ComputerCraft.debugEnable = x
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

        http.addBoolean("http.enabled", "Enabled", ComputerCraft.httpEnabled,
            "Enable the \"http\" API on Computers (see \"http_whitelist\" and \"http_blacklist\" for " +
                "more fine grained control than this)",
            x -> ComputerCraft.httpEnabled = x
        );

        http.addBoolean("http.websocket_enabled", "Websocket enabled", ComputerCraft.httpWebsocketEnabled,
            "Enable use of http websockets. This requires the \"http_enable\" option to also be true.",
            x -> ComputerCraft.httpWebsocketEnabled = x
        );

        http.addInt("http.max_requests", "Maximum concurrent requests", ComputerCraft.httpMaxRequests, 0, Integer.MAX_VALUE,
            "The number of http requests a computer can make at one time. Additional requests " +
                "will be queued, and sent when the running requests have finished. Set to 0 for unlimited.",
            x -> ComputerCraft.httpMaxRequests = x
        );

        http.addInt("http.max_websockets", "Maximum concurrent websockets", ComputerCraft.httpMaxWebsockets, 1, Integer.MAX_VALUE,
            "The number of websockets a computer can have open at one time. Set to 0 for unlimited.",
            x -> ComputerCraft.httpMaxWebsockets = x
        );

        http.addString("http.proxy", "Proxy", corsProxy,
            "The proxy to use in order to bypass Cross-Origin protection (aka CORS). This allows you to make requests to " +
                "any site, but does involve sending all headers to another site first. Set to empty or \"{}\" to disable.",
            x -> corsProxy = x
        );
    }
}
