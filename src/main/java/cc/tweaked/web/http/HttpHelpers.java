package cc.tweaked.web.http;

public class HttpHelpers {
    public static byte[] encode(String string) {
        byte[] chars = new byte[string.length()];
        for (int i = 0; i < chars.length; i++) {
            char c = string.charAt(i);
            chars[i] = c < 256 ? (byte) c : 63;
        }
        return chars;
    }
}
