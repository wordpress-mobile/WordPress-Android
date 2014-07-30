package org.wordpress.android.util;

import java.util.Comparator;
import java.util.Map;

public class BlogUtils {
    public static Comparator<Object> BlogNameComparator = new Comparator<Object>() {
        public int compare(Object blog1, Object blog2) {
            Map<Object, Object> blogMap1 = (Map<Object, Object>) blog1;
            Map<Object, Object> blogMap2 = (Map<Object, Object>) blog2;

            String blogName1 = MapUtils.getMapStr(blogMap1, "blogName");
            if (blogName1.length() == 0) {
                blogName1 = MapUtils.getMapStr(blogMap1, "url");
            }

            String blogName2 = MapUtils.getMapStr(blogMap2, "blogName");
            if (blogName2.length() == 0) {
                blogName2 = MapUtils.getMapStr(blogMap2, "url");
            }

            return blogName1.compareToIgnoreCase(blogName2);
        }
    };
}