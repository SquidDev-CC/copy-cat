package cc.squiddev.cct.stub;

import dan200.computercraft.api.lua.LuaException;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static java.util.Calendar.*;

public final class LuaDateTime {
    private LuaDateTime() {
    }

    public static long fromTable(Map<?, ?> table) throws LuaException {
        int year = getField(table, "year", -1);
        int month = getField(table, "month", -1);
        int day = getField(table, "day", -1);
        int hour = getField(table, "hour", 12);
        int minute = getField(table, "min", 12);
        int second = getField(table, "sec", 12);
        Calendar time = Calendar.getInstance();
        time.set(year, month, day, hour, minute, second);

        getBoolField(table, "isdst"); // TODO: Work it out
        return time.getTimeInMillis() / 1000;
    }

    public static Map<String, ?> toTable(Calendar calendar) {
        HashMap<String, Object> table = new HashMap<>(9);
        table.put("year", calendar.get(YEAR));
        table.put("month", calendar.get(MONTH));
        table.put("day", calendar.get(DAY_OF_MONTH));
        table.put("hour", calendar.get(HOUR));
        table.put("min", calendar.get(MINUTE));
        table.put("sec", calendar.get(SECOND));
        table.put("wday", calendar.get(DAY_OF_WEEK));
        table.put("yday", calendar.get(DAY_OF_YEAR));
        table.put("isdst", calendar.getTimeZone().useDaylightTime());
        return table;
    }

    private static int getField(Map<?, ?> table, String field, int def) throws LuaException {
        Object value = table.get(field);
        if (value instanceof Number) return ((Number) value).intValue();
        if (def < 0) throw new LuaException("field \"" + field + "\" missing in date table");
        return def;
    }

    private static Boolean getBoolField(Map<?, ?> table, String field) throws LuaException {
        Object value = table.get(field);
        if (value instanceof Boolean || value == null) return (Boolean) value;
        throw new LuaException("field \"" + field + "\" missing in date table");
    }
}
