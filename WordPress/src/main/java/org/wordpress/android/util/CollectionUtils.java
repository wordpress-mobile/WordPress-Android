package org.wordpress.android.util;

import java.util.List;

public class CollectionUtils {
    public static boolean areListsEqual(List<?> list1, List<?> list2) {
        if (list1 == null) return list2 == null;
        return !(list2 == null || list1.size() != list2.size()) && list1.equals(list2);
    }
}
