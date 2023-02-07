package cc.tweaked.web.stub;

public final class LoggerFactory {
    private static final Logger INSTANCE = new Logger();

    public static Logger getLogger(Class<?> klass) {
        return INSTANCE;
    }
}
