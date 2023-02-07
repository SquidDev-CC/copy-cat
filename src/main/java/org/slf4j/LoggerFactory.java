package org.slf4j;

public final class LoggerFactory {
    private static final Logger INSTANCE = new Logger();

    public static Logger getLogger(Class<?> klass) {
        return INSTANCE;
    }
}
