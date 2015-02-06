package org.wordpress.android.util;

public class GravatarUtils {
    /*
     * see https://en.gravatar.com/site/implement/images/
     */
    public static String gravatarFromEmail(final String email, int size) {
        return "http://gravatar.com/avatar/"
              + StringUtils.getMd5Hash(StringUtils.notNullStr(email))
              + "?d=mm&size=" + Integer.toString(size);
    }

    public static String blavatarFromUrl(final String url, int size) {
        return "http://gravatar.com/blavatar/"
              + StringUtils.getMd5Hash(UrlUtils.getDomainFromUrl(url))
              + "?d=mm&size=" + Integer.toString(size);
    }
}
