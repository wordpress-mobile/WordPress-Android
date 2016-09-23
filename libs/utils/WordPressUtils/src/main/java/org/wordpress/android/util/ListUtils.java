package org.wordpress.android.util;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUtils {
    public static ArrayList<Long> toLongList(long[] array) {
        Long[] longObjects = ArrayUtils.toObject(array);
        return new ArrayList<>(Arrays.asList(longObjects));
    }

    public static long[] toLongArray(List<Long> list) {
        Long[] array = (Long[]) list.toArray();
        return ArrayUtils.toPrimitive(array);
    }
}
