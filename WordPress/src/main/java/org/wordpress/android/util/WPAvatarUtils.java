package org.wordpress.android.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gravatar.DefaultAvatarImage;

import static com.gravatar.GravatarUtilsKt.rewriteGravatarImageUrlQueryParams;

/**
 * This file contains utility functions for working with avatar urls coming from WordPress accounts.
 * <p>
 * see https://docs.gravatar.com/general/images/
 */
public class WPAvatarUtils {
    public static final DefaultAvatarImage DEFAULT_AVATAR = DefaultAvatarImage.MYSTERY_PERSON;

    /**
     * Remove all query params from a gravatar url and set them to the given size and
     * default image. If the imageUrl parameters is not a gravatar link,
     * then use Photon to resize according to the avatarSz parameter.
     *
     * @param imageUrl     the url of the avatar image
     * @param avatarSz     the size of the avatar image
     * @param defaultImage the default image to use if the user doesn't have a gravatar
     * @return the fixed url
     */
    public static String rewriteAvatarUrl(@NonNull final String imageUrl, int avatarSz,
                                          @Nullable DefaultAvatarImage defaultImage) {
        if (TextUtils.isEmpty(imageUrl)) {
            return "";
        }

        // if this isn't a gravatar image, return as resized photon image url
        if (!imageUrl.contains("gravatar.com")) {
            return PhotonUtils.getPhotonImageUrl(imageUrl, avatarSz, avatarSz);
        } else {
            return rewriteGravatarImageUrlQueryParams(imageUrl, avatarSz, defaultImage, null, null);
        }
    }

    public static String rewriteAvatarUrl(@NonNull final String imageUrl, int avatarSz) {
        return rewriteAvatarUrl(imageUrl, avatarSz, DEFAULT_AVATAR);
    }
}
