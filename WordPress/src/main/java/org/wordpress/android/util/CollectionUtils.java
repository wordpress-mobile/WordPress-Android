package org.wordpress.android.util;

import android.text.TextUtils;

import java.util.List;

public class CollectionUtils {
    /**
     * @return
     *  true only if both lists are null or if both lists contain exactly the same ordered elements
     */
    public static boolean areListsEqual(List<?> list1, List<?> list2) {
        if (list1 == null) return list2 == null;
        return !(list2 == null || list1.size() != list2.size()) && list1.equals(list2);
    }

    /**
     * Adds a comma-separated list of {@link Long} values to a {@link List}.
     *
     * @param dest
     *  the {@link List} to add the long values to
     * @param src
     *  a comma-separated list of {@link Long} values
     */
    public static void addLongsFromStringListToArrayList(List<Long> dest, String src) {
        if (dest == null || TextUtils.isEmpty(src)) return;
        for (String longString : src.split(",")) {
            dest.add(Long.valueOf(longString));
        }
    }
}
