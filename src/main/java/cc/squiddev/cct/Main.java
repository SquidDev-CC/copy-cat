package cc.squiddev.cct;

import cc.squiddev.cct.js.Callbacks;
import cc.squiddev.cct.js.ComputerAccess;
import cc.squiddev.cct.js.JsonParse;
import cc.squiddev.cct.mount.ComputerAccessMount;
import cc.squiddev.cct.mount.ResourceMount;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.Palette;

import java.io.InputStream;

public class Main implements IComputerEnvironment {
    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        ComputerCraft.logPeripheralErrors = true;

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
            computer.tick();

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
        return 0;
    }

    @Override
    public double getTimeOfDay() {
        return 0;
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
}
