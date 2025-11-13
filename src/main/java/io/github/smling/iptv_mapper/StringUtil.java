package io.github.smling.iptv_mapper;

public class StringUtil {
    public static String nullSafe(String s) { return s == null ? "" : s; }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static boolean notNullAndNotEmpty(String value) {
        return !isNullOrEmpty(value);
    }
}
