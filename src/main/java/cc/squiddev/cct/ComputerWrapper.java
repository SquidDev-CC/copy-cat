package cc.squiddev.cct;

import cc.squiddev.cct.js.ComputerAccess;
import cc.squiddev.cct.js.ComputerCallbacks;
import cc.squiddev.cct.js.JsonParse;
import cc.squiddev.cct.mount.ComputerAccessMount;
import cc.squiddev.cct.mount.ResourceMount;
import cc.squiddev.cct.peripheral.SpeakerPeripheral;
import cc.squiddev.cct.peripheral.TickablePeripheral;
import cc.squiddev.cct.stub.Logger;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.Palette;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

import static dan200.computercraft.ComputerCraft.computerTermHeight;
import static dan200.computercraft.ComputerCraft.computerTermWidth;

public class ComputerWrapper implements IComputerEnvironment, ComputerCallbacks {
    private static final ComputerSide[] SIDES = ComputerSide.values();
    private final TerminalMonitor terminalMonitor = new TerminalMonitor();
    private final Terminal terminal = new Terminal(computerTermWidth, computerTermHeight, terminalMonitor);
    private final Computer computer;
    private final ComputerAccess computerAccess;
    private boolean disposed = false;
    private boolean customSize;

    public ComputerWrapper(ComputerAccess computerAccess) {
        this.computerAccess = computerAccess;
        this.computer = new Computer(this, terminal, 0);

        if (!disposed) computer.turnOn();
    }

    /**
     * Tick this computer.
     *
     * @return If this computer has been disposed of.
     */
    public boolean tick() {
        if (disposed && computer.isOn()) computer.unload();

        try {
            computer.tick();
        } catch (RuntimeException e) {
            Logger.INSTANCE.error("Error when ticking computer", e);
        }

        if (computer.pollAndResetChanged()) {
            computerAccess.setState(computer.getLabel(), computer.isOn());
        }

        if (!customSize && (terminal.getWidth() != computerTermWidth || terminal.getHeight() != computerTermHeight)) {
            terminal.resize(computerTermWidth, computerTermHeight);
            computer.queueEvent("term_resize", null);
        }

        for (ComputerSide side : SIDES) {
            IPeripheral peripheral = computer.getEnvironment().getPeripheral(side);
            if (peripheral instanceof TickablePeripheral) ((TickablePeripheral) peripheral).tick();
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
            for (int i = 0; i < 16; i++) {
                double[] colours = palette.getColour(i);
                computerAccess.setPaletteColour(15 - i, colours[0], colours[1], colours[2]);
            }

            computerAccess.flushTerminal();
        }

        return disposed && !computer.isOn();
    }

    @Override
    public int getDay() {
        return (int) ((Main.getTicks() + 6000) / 24000) + 1;
    }

    @Override
    public double getTimeOfDay() {
        return ((Main.getTicks() + 6000) % 24000) / 1000.0;
    }

    @Override
    public boolean isColour() {
        return true;
    }

    @Override
    public long getComputerSpaceLimit() {
        return 0;
    }

    @Nonnull
    @Override
    public String getHostString() {
        return "ComputerCraft " + ComputerCraft.getVersion() + " (copy-cat)";
    }

    @Nonnull
    @Override
    public String getUserAgent() {
        return ComputerCraft.MOD_ID + "/" + ComputerCraft.getVersion();
    }

    @Override
    public int assignNewID() {
        return 0;
    }

    @Override
    public IWritableMount createSaveDirMount(String subPath, long capacity) {
        return new ComputerAccessMount(computerAccess);
    }

    @Override
    public IMount createResourceMount(String domain, String subPath) {
        return new ResourceMount("data/" + domain + "/" + subPath);
    }

    @Override
    public InputStream createResourceFile(String domain, String subPath) {
        return ComputerCraft.class.getClassLoader().getResourceAsStream("data/" + domain + "/" + subPath);
    }

    @Override
    public void setLabel(@Nullable String label) {
        computer.setLabel(label);
    }

    @Override
    public void event(@Nonnull String event, String[] args) {
        computer.queueEvent(event, JsonParse.parseValues(args));
    }

    @Override
    public void shutdown() {
        computer.shutdown();
    }

    @Override
    public void turnOn() {
        computer.turnOn();
    }

    @Override
    public void reboot() {
        computer.reboot();
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public void resize(int width, int height) {
        customSize = true;
        if (terminal.getWidth() != width || terminal.getHeight() != height) {
            terminal.resize(width, height);
            computer.queueEvent("term_resize", null);
        }
    }

    @Override
    public void setPeripheral(@Nonnull String sideName, @Nullable String kind) {
        ComputerSide side = ComputerSide.valueOfInsensitive(sideName);
        IPeripheral peripheral;
        if (kind == null) {
            peripheral = null;
        } else if (kind.equals("peripheral")) {
            peripheral = new SpeakerPeripheral();
        } else {
            throw new IllegalStateException("Unknown peripheral kind");
        }

        computer.getEnvironment().setPeripheral(side, peripheral);
    }
}
