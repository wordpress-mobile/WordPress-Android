package org.wordpress.android.util;

import java.util.Comparator;
import java.util.Map;

public class BlogUtils {
    public static Comparator<Object> BlogNameComparator = new Comparator<Object>() {
        public int compare(Object blog1, Object blog2) {
            Map<String, Object> blogMap1 = (Map<String, Object>) blog1;
            Map<String, Object> blogMap2 = (Map<String, Object>) blog2;
            String blogName1 = getBlogNameOrHomeURLFromAccountMap(blogMap1);
            String blogName2 = getBlogNameOrHomeURLFromAccountMap(blogMap2);
            return blogName1.compareToIgnoreCase(blogName2);
        }
    };

    /**
     * Return a blog name or blog home URL if trimmed name is an empty string
     */
    public static String getBlogNameOrHomeURLFromAccountMap(Map<String, Object> account) {
        String blogName = getBlogNameFromAccountMap(account);
        if (blogName.trim().length() == 0) {
            blogName = BlogUtils.getHomeURLOrHostNameFromAccountMap(account);
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
     * Return the blog home URL setting or the host name if home URL is an empty string.
     */
    public static String getHomeURLOrHostNameFromAccountMap(Map<String, Object> account) {
        String homeURL = MapUtils.getMapStr(account, "homeURL").replace("http://", "").replace("https://", "").trim();
        if (homeURL.endsWith("/")) {
            homeURL = homeURL.substring(0, homeURL.length() -1);
        }

        if (homeURL.length() == 0) {
            return StringUtils.getHost(MapUtils.getMapStr(account, "url"));
        }

        return homeURL;
    }
}
