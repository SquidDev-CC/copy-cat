package org.slf4j;

import cc.tweaked.web.js.Console;

import java.util.Arrays;

public class Logger {
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    public void trace(String message, Object... args) {
        Console.log(message + " " + Arrays.toString(args));
        printException(args);
    }

    public void debug(String message, Object... args) {
        Console.log(message + " " + Arrays.toString(args));
        printException(args);
    }

    public void warn(Marker marker, String message, Object... args) {
        warn(message, args);
    }

    public void warn(String message, Object... args) {
        Console.warn(message + " " + Arrays.toString(args));
        printException(args);
    }

    public void error(Marker marker, String message, Object... args) {
        error(message, args);
    }

    public void error(String message, Object... args) {
        Console.error(message + " " + Arrays.toString(args));
        printException(args);
    }

    private static void printException(Object[] args) {
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            ((Throwable) args[args.length - 1]).printStackTrace();
        }
    }
}
