package io.github.smling.iptv_mapper;

import java.util.List;
import java.util.Objects;

public final class ListUtil {
    public static <T> boolean isNullOrEmpty(List<T> value) {
        if (Objects.isNull(value)) {
            return true;
        }
        return value.isEmpty();
    }

    public static <T> boolean notNullAndNotEmpty(List<T> value) {
        return !isNullOrEmpty(value);
    }
}
