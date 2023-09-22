package cc.tweaked.web;

import cc.tweaked.web.js.Callbacks;
import cc.tweaked.web.mount.ResourceMount;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.computer.GlobalEnvironment;

import javax.annotation.Nonnull;
import java.io.InputStream;

public final class GlobalEnvironmentImpl implements GlobalEnvironment {
    public static final GlobalEnvironmentImpl INSTANCE = new GlobalEnvironmentImpl();

    private static final String VERSION = Callbacks.getModVersion();

    private GlobalEnvironmentImpl() {
    }

    @Nonnull
    @Override
    public String getHostString() {
        return "ComputerCraft " + VERSION + " (copy-cat)";
    }

    @Override
    public String getUserAgent() {
        return "computercraft/" + VERSION;
    }

    @Override
    public Mount createResourceMount(String domain, String subPath) {
        if (domain.equals("computercraft") && subPath.equals("lua/rom")) {
            return ResourceMount.rom();
        } else {
            throw new IllegalArgumentException("Unknown domain or subpath");
        }
    }

    @Override
    public InputStream createResourceFile(String domain, String subPath) {
        if (domain.equals("computercraft") && subPath.equals("lua/bios.lua")) {
            return ResourceMount.bios();
        } else {
            throw new IllegalArgumentException("Unknown domain or subpath");
        }
    }
}
