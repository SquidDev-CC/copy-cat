package cc.squiddev.cct;

import cc.squiddev.cct.js.Callbacks;
import cc.squiddev.cct.js.ComputerAccess;
import cc.squiddev.cct.js.ConfigGroup;
import cc.squiddev.cct.js.JsonParse;
import cc.squiddev.cct.mount.ComputerAccessMount;
import cc.squiddev.cct.mount.ResourceMount;
import cc.squiddev.cct.stub.Logger;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.apis.http.websocket.Websocket;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.Palette;

import java.io.InputStream;

public class Main implements IComputerEnvironment {
    public static String corsProxy = "https://cors-anywhere.herokuapp.com/{}";

    private long ticks;

    public static void main(String[] args) {
        ComputerCraft.log = Logger.INSTANCE;
        setupConfig();
        new Main().run();
    }

    public void run() {
        TerminalMonitor terminalMonitor = new TerminalMonitor();

        Terminal terminal = new Terminal(ComputerCraft.terminalWidth_computer, ComputerCraft.terminalHeight_computer, terminalMonitor);
        Computer computer = new Computer(this, terminal, 0);

        ComputerAccess computerAccess = Callbacks.computer();
        computer.setLabel(computerAccess.getLabel());
        computerAccess.onEvent((e, args) -> computer.queueEvent(e, JsonParse.parseValues(args)));
        computerAccess.onTurnOn(computer::turnOn);
        computerAccess.onReboot(computer::reboot);
        computerAccess.onShutdown(computer::shutdown);

        Callbacks.setInterval(() -> {
            ticks++;

            try {
                computer.tick();
            } catch (RuntimeException e) {
                Logger.INSTANCE.error("Error when ticking computer", e);
            }

            if (computer.pollAndResetChanged()) {
                computerAccess.setState(computer.getLabel(), computer.isOn());
            }

            if (terminalMonitor.pollChanged()) {
                computerAccess.updateTerminal(
                    terminal.getWidth(), terminal.getHeight(),
                    terminal.getCursorX(), terminal.getCursorY(),
                    terminal.getCursorBlink(), terminal.getTextColour()
                );

                for (int i = 0; i < terminal.getHeight(); i++) {
                    computerAccess.setTerminalLine(i,
                        terminal.getLine(i).toString(),
                        terminal.getTextColourLine(i).toString(),
                        terminal.getBackgroundColourLine(i).toString()
                    );
                }

                Palette palette = terminal.getPalette();
                for (int i = 0; i < 15; i++) {
                    double[] colours = palette.getColour(i);
                    computerAccess.setPaletteColour(15 - i, colours[0], colours[1], colours[2]);
                }

                computerAccess.flushTerminal();
            }
        }, 50);

        computer.turnOn();
    }

    @Override
    public int getDay() {
        return (int) ((ticks + 6000) / 24000) + 1;
    }

    @Override
    public double getTimeOfDay() {
        return ((ticks + 6000) % 24000) / 1000.0;
    }

    @Override
    public boolean isColour() {
        return true;
    }

    @Override
    public long getComputerSpaceLimit() {
        return 0;
    }

    @Override
    public String getHostString() {
        return "ComputerCraft " + ComputerCraft.getVersion() + " (Online)";
    }

    @Override
    public int assignNewID() {
        return 0;
    }

    @Override
    public IWritableMount createSaveDirMount(String subPath, long capacity) {
        return new ComputerAccessMount(Callbacks.computer());
    }

    @Override
    public IMount createResourceMount(String domain, String subPath) {
        return new ResourceMount("assets/" + domain + "/" + subPath);
    }

    @Override
    public InputStream createResourceFile(String domain, String subPath) {
        return ComputerCraft.class.getClassLoader().getResourceAsStream("assets/" + domain + "/" + subPath);
    }

    private static void setupConfig() {
        ComputerCraft.logPeripheralErrors = true;

        ConfigGroup general = Callbacks.config("ComputerCraft", null);

        general.addInt("maximum_open_files", "Maximum open files", ComputerCraft.maximumFilesOpen, 0, Integer.MAX_VALUE,
            "Set how many files a computer can have open at the same time. Set to 0 for unlimited.",
            x -> ComputerCraft.maximumFilesOpen = x
        );

        general.addBoolean("disable_lua51_features", "Disable Lua 5.1 features", ComputerCraft.disable_lua51_features,
            "Set this to true to disable Lua 5.1 functions that will be removed in a future " +
                "update. Useful for ensuring forward compatibility of your programs now.",
            x -> ComputerCraft.disable_lua51_features = x
        );

        general.addString("default_computer_settings", "Default computer settings", ComputerCraft.default_computer_settings,
            "A comma separated list of default system settings to set on new computers. Example: " +
                "\"shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false\" will disable all autocompletion",
            x -> ComputerCraft.default_computer_settings = x
        );

        general.addBoolean("debug_enabled", "Debug enabled", ComputerCraft.debug_enable,
            "Enable Lua's debug library. This is sandboxed to each computer, so is generally safe to be used by players.",
            x -> ComputerCraft.debug_enable = x
        );

        ConfigGroup http = Callbacks.config("HTTP API", "Controls the HTTP API");

        http.addBoolean("http.enabled", "Enabled", ComputerCraft.http_enable,
            "Enable the \"http\" API on Computers (see \"http_whitelist\" and \"http_blacklist\" for " +
                "more fine grained control than this)",
            x -> ComputerCraft.http_enable = x
        );

        http.addBoolean("http.websocket_enabled", "Websocket enabled", ComputerCraft.http_websocket_enable,
            "Enable use of http websockets. This requires the \"http_enable\" option to also be true.",
            x -> ComputerCraft.http_websocket_enable = x
        );

        http.addInt("http.max_requests", "Maximum concurrent requests", ComputerCraft.httpMaxRequests, 0, Integer.MAX_VALUE,
            "The number of http requests a computer can make at one time. Additional requests " +
                "will be queued, and sent when the running requests have finished. Set to 0 for unlimited.",
            x -> ComputerCraft.httpMaxRequests = x
        );

        http.addInt("http.max_download", "Maximum response size", (int) ComputerCraft.httpMaxDownload, 0, Integer.MAX_VALUE,
            "The maximum size (in bytes) that a computer can download in a single request. " +
                "Note that responses may receive more data than allowed, but this data will not be returned to the client.",
            x -> ComputerCraft.httpMaxDownload = x
        );

        http.addInt("http.max_upload", "Maximum request size", (int) ComputerCraft.httpMaxUpload, 0, Integer.MAX_VALUE,
            "The maximum size (in bytes) that a computer can upload in a single request. This " +
                "includes headers and POST text.",
            x -> ComputerCraft.httpMaxUpload = x
        );

        http.addInt("http.max_websockets", "Maximum concurrent websockets", ComputerCraft.httpMaxWebsockets, 1, Integer.MAX_VALUE,
            "The number of websockets a computer can have open at one time. Set to 0 for unlimited.",
            x -> ComputerCraft.httpMaxWebsockets = x
        );

        http.addInt("http.max_websocket_message", "Maximum websocket message size", ComputerCraft.httpMaxWebsocketMessage, 0, Websocket.MAX_MESSAGE_SIZE,
            "The maximum size (in bytes) that a computer can send or receive in one websocket packet.",
            x -> ComputerCraft.httpMaxWebsocketMessage = x
        );

        http.addString("http.proxy", "Proxy", corsProxy,
            "The proxy to use in order to bypass Cross-Origin protection (aka CORS). This allows you to make requests to " +
                "any site, but does involve sending all headers to another site first. Set to empty or \"{}\" to disable.",
            x -> corsProxy = x
        );
    }
}
