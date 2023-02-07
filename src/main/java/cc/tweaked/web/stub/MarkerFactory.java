package cc.tweaked.web.stub;

public class MarkerFactory {
    private static final Marker INSTANCE = new Marker();

    public static Marker getMarker(String name) {
        return INSTANCE;
    }
}
