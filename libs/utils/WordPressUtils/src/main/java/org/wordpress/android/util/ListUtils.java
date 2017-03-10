package org.wordpress.android.util;

import android.support.annotation.Nullable;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUtils {
    @Nullable
    public static ArrayList<Long> fromLongArray(long[] array) {
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
        Long[] array = list.toArray(new Long[list.size()]);
        return ArrayUtils.toPrimitive(array);
    }

    @Nullable
    public static ArrayList<Integer> fromIntArray(int[] array) {
        if (array == null) {
            return null;
        }
        Integer[] intObjects = ArrayUtils.toObject(array);
        return new ArrayList<>(Arrays.asList(intObjects));
    }

    @Nullable
    public static int[] toIntArray(List<Integer> list) {
        if (list == null) {
            return null;
        }
        Integer[] array = list.toArray(new Integer[list.size()]);
        return ArrayUtils.toPrimitive(array);
    }
}
