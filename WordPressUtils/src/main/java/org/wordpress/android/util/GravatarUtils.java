package org.wordpress.android.util;

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
