package cc.tweaked.copycat.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Callbacks {
    @JSFunctor
    @FunctionalInterface
    public interface Setup extends JSObject {
        ExtendedComputerHandle addComputer(ExtendedComputerDisplay computer);
    }

    /**
     * Get the current callback instance
     *
     * @param setup The setup function.
     */
    @JSBody(params = {"setup"}, script = "return $javaCallbacks.setup(setup);")
    public static native void setup(Setup setup);

    /**
     * Get or create a config group
     *
     * @param name        The display name of this group
     * @param description A short description of this group
     * @return The constructed config group
     */
    @JSBody(params = {"name", "description"}, script = "return $javaCallbacks.config(name, description);")
    public static native ConfigGroup config(@Nonnull String name, @Nullable String description);
}
