package org.wordpress.android.util;

import android.text.TextUtils;

/**
 * Created by nbradbury on 11/11/13.
 */
public class GravatarUtils {

    /*
     * see https://en.gravatar.com/site/implement/images/
     */
    public static String gravatarUrlFromEmail(final String email) {
        return gravatarUrlFromEmail(email, 0);
    }
    public static String gravatarUrlFromEmail(final String email, int size) {
        if (TextUtils.isEmpty(email))
            return "";

        String url = "http://gravatar.com/avatar/"
                    + StringUtils.getMd5Hash(email)
                    + "?d=mm";

        if (size > 0)
            url += "&s=" + Integer.toString(size);

        return url;
    }

    /*
     * returns the passed avatar url with d=mm to ensure that the default avatar is
     * the "mystery man" image - necessary since our API often returns avatar URLs
     * with "d=identicon"
     */
    public static String fixGravatarUrl(final String url) {
        if (TextUtils.isEmpty(url))
            return url;
        if (url.contains("d=identicon"))
            return url.replace("d=identicon", "d=mm");
        String prefix = (url.contains("?") ? "&" : "?");
        return url + prefix + "d=mm";
    }

}
