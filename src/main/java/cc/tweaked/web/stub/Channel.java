package cc.tweaked.web.stub;

import com.google.errorprone.annotations.DoNotCall;

import java.io.Closeable;

/**
 * @see java.nio.channels.Channel
 */
public interface Channel extends Closeable {
    @DoNotCall
    default boolean isOpen() {
        return true;
    }
}
