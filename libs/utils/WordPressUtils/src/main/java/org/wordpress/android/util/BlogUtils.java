package org.wordpress.android.util;

import java.util.Comparator;
import java.util.Map;

public class BlogUtils {
    public static Comparator<Object> BlogNameComparator = new Comparator<Object>() {
        public int compare(Object blog1, Object blog2) {
            Map<String, Object> blogMap1 = (Map<String, Object>) blog1;
            Map<String, Object> blogMap2 = (Map<String, Object>) blog2;
            String blogName1 = getBlogNameOrHostNameFromAccountMap(blogMap1);
            String blogName2 = getBlogNameOrHostNameFromAccountMap(blogMap2);
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
