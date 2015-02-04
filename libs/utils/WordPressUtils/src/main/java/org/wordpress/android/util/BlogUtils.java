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

    /**
     * Return a blog name or blog url (host part only) if trimmed name is an empty string
     */
    public static String getBlogNameOrHostNameFromAccountMap(Map<String, Object> account) {
        String blogName = getBlogNameFromAccountMap(account);
        if (blogName.trim().length() == 0) {
            blogName = StringUtils.getHost(MapUtils.getMapStr(account, "url"));
        }
        return blogName;
    }

    /**
     * Return a blog name or blog url (host part only) if trimmed name is an empty string
     */
    public static String getBlogNameFromAccountMap(Map<String, Object> account) {
        return StringUtils.unescapeHTML(MapUtils.getMapStr(account, "blogName"));
    }

    /**
     * Return blog url (host part only) if trimmed name is an empty string
     */
    public static String getHostNameFromAccountMap(Map<String, Object> account) {
        return StringUtils.getHost(MapUtils.getMapStr(account, "url"));
    }
}
