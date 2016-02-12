package org.wordpress.android.util;

import android.text.TextUtils;

/**
 * see https://en.gravatar.com/site/implement/images/
 */
public class GravatarUtils {

    // by default tell gravatar to respond to non-existent images with a 404 - this means
    // it's up to the caller to catch the 404 and provide a suitable default image
    private static final DefaultImage DEFAULT_GRAVATAR = DefaultImage.STATUS_404;

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
        return fixGravatarUrl(imageUrl, avatarSz, DEFAULT_GRAVATAR);
    }
    public static String fixGravatarUrl(final String imageUrl, int avatarSz, DefaultImage defaultImage) {
        if (TextUtils.isEmpty(imageUrl)) {
            return "";
        }

        // if this isn't a gravatar image, return as resized photon image url
        if (!imageUrl.contains("gravatar.com")) {
            return PhotonUtils.getPhotonImageUrl(imageUrl, avatarSz, avatarSz);
        }

        // remove all other params, then add query string for size and default image
        return UrlUtils.removeQuery(imageUrl) + "?s=" + avatarSz + "&d=" + defaultImage.toString();
    }

    public static String gravatarFromEmail(final String email, int size) {
        return gravatarFromEmail(email, size, DEFAULT_GRAVATAR);
    }
    public static String gravatarFromEmail(final String email, int size, DefaultImage defaultImage) {
        return "http://gravatar.com/avatar/"
              + StringUtils.getMd5Hash(StringUtils.notNullStr(email))
              + "?d=" + defaultImage.toString()
              + "&size=" + Integer.toString(size);
    }

    public static String blavatarFromUrl(final String url, int size) {
        return blavatarFromUrl(url, size, DEFAULT_GRAVATAR);
    }
    public static String blavatarFromUrl(final String url, int size, DefaultImage defaultImage) {
        return "http://gravatar.com/blavatar/"
                + StringUtils.getMd5Hash(UrlUtils.getHost(url))
                + "?d=" + defaultImage.toString()
                + "&size=" + Integer.toString(size);
    }
}
