package org.wordpress.android.util;

import android.text.TextUtils;

public class GravatarUtils {
    /*
     * see https://en.gravatar.com/site/implement/images/
     */

    public static enum DefaultImage {
        MYSTERY_MAN,
        STATUS_404,
        IDENTICON,
        MONSTER,
        WAVATAR,
        RETRO,
        BLANK;

        @Override
        public String toString() {
            switch (this) {
                case MYSTERY_MAN:
                    return "mm";
                case STATUS_404:
                    return "404";
                case IDENTICON:
                    return "identicon";
                case MONSTER:
                    return "monsterid";
                case WAVATAR:
                    return "wavatar";
                case RETRO:
                    return "retro";
                default:
                    return "blank";
            }
        }
    }

    /*
    * gravatars often contain the ?s= parameter which determines their size - detect this and
    * replace it with a new ?s= parameter which requests the avatar at the exact size needed
    */
    public static String fixGravatarUrl(final String imageUrl, int avatarSz) {
        if (TextUtils.isEmpty(imageUrl)) {
            return "";
        }

        // if this isn't a gravatar image, return as resized photon image url
        if (!imageUrl.contains("gravatar.com")) {
            return PhotonUtils.getPhotonImageUrl(imageUrl, avatarSz, avatarSz);
        }

        // remove all other params, then add query string for size and "mystery man" default
        return UrlUtils.removeQuery(imageUrl) + "?s=" + avatarSz + "&d=mm";
    }

    public static String gravatarFromEmail(final String email, int size) {
        return gravatarFromEmail(email, size, DefaultImage.MYSTERY_MAN);
    }
    public static String gravatarFromEmail(final String email, int size, DefaultImage defaultImage) {
        return "http://gravatar.com/avatar/"
              + StringUtils.getMd5Hash(StringUtils.notNullStr(email))
              + "?d=" + defaultImage.toString()
              + "&size=" + Integer.toString(size);
    }

    public static String blavatarFromUrl(final String url, int size, DefaultImage defaultImage) {
        return "http://gravatar.com/blavatar/"
                + StringUtils.getMd5Hash(UrlUtils.getDomainFromUrl(url))
                + "?d=" + defaultImage.toString()
                + "&size=" + Integer.toString(size);
    }
}
