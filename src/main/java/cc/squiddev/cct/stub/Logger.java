package cc.squiddev.cct.stub;

import java.util.Arrays;

/**
 * I'm not bringing in the entirety of Log4J (and TeaVM will hate it due to reflection), so just
 * stub out the logging methods we need.
 */
public final class Logger {
    public static final Logger INSTANCE = new Logger();

    private Logger() {
    }

    public void warn(String message) {
        System.err.println("[WARN] " + message);
    }

    public void warn(String message, Object... args) {
        warn(message + " " + Arrays.toString(args));
        checkException(args);
    }

    public void warn(String message, Throwable throwable) {
        System.err.println("[WARN] " + message);
        throwable.printStackTrace();
    }

    public void error(String message, Object... args) {
        System.err.println("[ERROR] " + message + " " + Arrays.toString(args));
        checkException(args);
    }

    public void error(String message, Throwable throwable) {
        System.err.println("[ERROR] " + message);
        throwable.printStackTrace();
    }

    public void debug(String message, Object... args) {
        System.err.println("[DEBUG] " + message + " " + Arrays.toString(args));
        checkException(args);
    }

    private static void checkException(Object[] args) {
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            ((Throwable) args[args.length - 1]).printStackTrace();
        }
    }
}
