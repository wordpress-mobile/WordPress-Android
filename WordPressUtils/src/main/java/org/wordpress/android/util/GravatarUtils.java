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

    /*
     * important: the 404 default means the request will 404 if there is no blavatar
     * for the passed site - so the caller needs to trap the 404 to provide a default
     */
    public static String blavatarFromUrl(final String url, int size) {
        return "http://gravatar.com/blavatar/"
                + StringUtils.getMd5Hash(UrlUtils.getDomainFromUrl(url))
                + "?d=404&size=" + Integer.toString(size);
    }
}
