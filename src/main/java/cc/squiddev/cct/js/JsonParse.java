package cc.squiddev.cct.js;

/**
 * A basic JSON parser for primitives
 *
 * We pass JSON in through {@link ComputerAccess.QueueEventHandler}, as TeaVM doesn't allow an easy
 * way to handle arbitrary JSON objects. This acts as a primitive parser for these objects, only handling
 * types we expect to see.
 */
public class JsonParse {
    public static Object parseValue(String value) {
        if (value.isEmpty() || value.equals("null")) return null;
        if (value.equals("true")) return true;
        if (value.equals("false")) return false;

        char first = value.charAt(0);
        if (first >= '0' && first <= '9' || first == '-') {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        } else if (first == '"') {
            StringBuilder builder = new StringBuilder(value.length() - 2);
            for (int i = 1; i < value.length() - 1; i++) {
                char c = value.charAt(i);
                if (c == '\\') {
                    builder.append(decodeEscape(value.charAt(++i)));
                } else {
                    builder.append(c);
                }
            }

            return builder.toString();
        }

        return null;
    }

    public static Object[] parseValues(String[] value) {
        if (value == null) return null;
        Object[] result = new Object[value.length];
        for (int i = 0; i < value.length; i++) result[i] = parseValue(value[i]);
        return result;
    }

    private static char decodeEscape(char escape) {
        switch (escape) {
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case '0':
                return '\0';
            default:
                return escape;
        }
    }
}
