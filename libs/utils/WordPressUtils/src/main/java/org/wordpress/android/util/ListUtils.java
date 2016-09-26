package org.wordpress.android.util;

import android.support.annotation.Nullable;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUtils {
    @Nullable
    public static ArrayList<Long> toLongList(long[] array) {
        if (array == null) {
            return null;
        }
        Long[] longObjects = ArrayUtils.toObject(array);
        return new ArrayList<>(Arrays.asList(longObjects));
    }

    @Nullable
    public static long[] toLongArray(List<Long> list) {
        if (list == null) {
            return null;
        }
        Long[] array = (Long[]) list.toArray();
        return ArrayUtils.toPrimitive(array);
    }
}
