package cc.tweaked.web.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Callbacks {
    @JSFunctor
    @FunctionalInterface
    public interface Setup extends JSObject {
        ComputerCallbacks addComputer(ComputerAccess computer);
    }

    /**
     * Get the current callback instance
     *
     * @param setup The setup function.
     */
    @JSBody(params = {"setup"}, script = "return copycatCallbacks.setup(setup);")
    public static native void setup(Setup setup);

    /**
     * Get or create a config group
     *
     * @param name        The display name of this group
     * @param description A short description of this group
     * @return The constructed config group
     */
    @JSBody(params = {"name", "description"}, script = "return copycatCallbacks.config(name, description);")
    public static native ConfigGroup config(@Nonnull String name, @Nullable String description);

    /**
     * Get the version of CC: Tweaked
     *
     * @return The mod's version.
     */
    @JSBody(script = "return copycatCallbacks.modVersion;")
    public static native String getModVersion();

    /**
     * List all resources available in the ROM.
     *
     * @return All available resources.
     */
    @JSBody(script = "return copycatCallbacks.listResources();")
    public static native String[] listResources();

    /**
     * Load a resource from the ROM.
     *
     * @param resource The path to the resource to load.
     * @return The loaded resource.
     */
    @JSByRef
    @JSBody(params = "name", script = "return copycatCallbacks.getResource(name);")
    public static native byte[] getResource(String resource);
}
