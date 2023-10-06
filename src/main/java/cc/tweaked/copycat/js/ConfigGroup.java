package cc.tweaked.copycat.js;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import javax.annotation.Nonnull;

public interface ConfigGroup extends JSObject {
    /**
     * Add a string property to this config group
     *
     * @param id          Unique ID for this property
     * @param name        The display name of this group
     * @param description A short description of this group
     * @param def         The default value
     * @param changed     Called when it is changed
     */
    void addString(@Nonnull String id, @Nonnull String name, @Nonnull String def, @Nonnull String description, @Nonnull StringPropertyConsumer changed);

    /**
     * Add a boolean property to this config group
     *
     * @param id          Unique ID for this property
     * @param name        The display name of this group
     * @param description A short description of this group
     * @param def         The default value
     * @param changed     Called when it is changed
     */
    void addBoolean(@Nonnull String id, @Nonnull String name, boolean def, @Nonnull String description, @Nonnull BooleanPropertyConsumer changed);

    /**
     * Add an integer property to this config group
     *
     * @param id          Unique ID for this property
     * @param name        The display name of this group
     * @param description A short description of this group
     * @param min         The minimum value
     * @param max         The maximum value
     * @param def         The default value
     * @param changed     Called when it is changed
     */
    void addInt(@Nonnull String id, @Nonnull String name, int def, int min, int max, @Nonnull String description, @Nonnull IntPropertyConsumer changed);

    @JSFunctor
    @FunctionalInterface
    interface StringPropertyConsumer extends JSObject {
        void onChanged(@Nonnull String value);
    }

    @JSFunctor
    @FunctionalInterface
    interface BooleanPropertyConsumer extends JSObject {
        void onChanged(boolean value);
    }

    @JSFunctor
    @FunctionalInterface
    interface IntPropertyConsumer extends JSObject {
        void onChanged(int value);
    }
}
