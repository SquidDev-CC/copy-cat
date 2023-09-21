package cc.tweaked.builder;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;

public final class CloseScope implements Closeable {
    private final ArrayDeque<Closeable> toClose = new ArrayDeque<>();

    public <T extends Closeable> T add(T value) {
        toClose.addLast(value);
        return value;
    }

    @Override
    public void close() throws IOException {
        Throwable error = null;

        AutoCloseable next;
        while ((next = toClose.pollLast()) != null) {
            try {
                next.close();
            } catch (Throwable e) {
                if (error == null) {
                    error = e;
                } else {
                    error.addSuppressed(e);
                }
            }
        }

        if (error != null) CloseScope.<IOException>throwUnchecked0(error);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked0(Throwable t) throws T {
        throw (T) t;
    }
}
