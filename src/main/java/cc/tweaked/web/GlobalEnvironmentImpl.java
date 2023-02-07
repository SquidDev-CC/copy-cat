package cc.tweaked.web;

import cc.tweaked.web.mount.ResourceMount;
import cc.tweaked.web.mount.Resources;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.computer.GlobalEnvironment;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.InputStream;

public final class GlobalEnvironmentImpl implements GlobalEnvironment {
    public static final GlobalEnvironmentImpl INSTANCE = new GlobalEnvironmentImpl();

    private GlobalEnvironmentImpl() {
    }

    @Nonnull
    @Override
    public String getHostString() {
        return "ComputerCraft " + Resources.VERSION + " (copy-cat)";
    }

    @Override
    public String getUserAgent() {
        return "computercraft/" + Resources.VERSION;
    }

    @Nullable
    @Override
    public Mount createResourceMount(String domain, String subPath) {
        return new ResourceMount("data/" + domain + "/" + subPath);
    }

    @Nullable
    @Override
    public InputStream createResourceFile(String domain, String subPath) {
        return ResourceMount.class.getClassLoader().getResourceAsStream("data/" + domain + "/" + subPath);
    }
}
