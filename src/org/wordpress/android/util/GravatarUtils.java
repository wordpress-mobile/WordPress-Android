package org.wordpress.android.util;

import android.text.TextUtils;

/**
 * Created by nbradbury on 11/11/13.
 */
public class GravatarUtils {

    /*
     * see https://en.gravatar.com/site/implement/images/
     */
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

}
