package org.wordpress.android.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gravatar.AvatarQueryOptions;
import com.gravatar.AvatarUrl;
import com.gravatar.DefaultAvatarOption;
import com.gravatar.DefaultAvatarOption.MysteryPerson;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This file contains utility functions for working with avatar urls coming from WordPress accounts.
 * <p>
 * see https://docs.gravatar.com/general/images/
 */
public class WPAvatarUtils {
    private WPAvatarUtils() {
        throw new IllegalStateException("Utility class");
    }
    public static final DefaultAvatarOption DEFAULT_AVATAR = MysteryPerson.INSTANCE;

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
                                          @Nullable DefaultAvatarOption defaultImage) {
        if (TextUtils.isEmpty(imageUrl)) {
            return "";
        }

        // if this isn't a gravatar image, return as resized photon image url
        if (!imageUrl.contains("gravatar.com")) {
            return PhotonUtils.getPhotonImageUrl(imageUrl, avatarSz, avatarSz);
        } else {
            try {
                return new AvatarUrl(new URL(imageUrl),
                        new AvatarQueryOptions.Builder()
                            .setPreferredSize(avatarSz)
                            .setDefaultAvatarOption(defaultImage)
                            .build()
                        ).url(null).toString();
            } catch (MalformedURLException | IllegalArgumentException e) {
                return "";
            }
        }
    }

    public static String rewriteAvatarUrl(@NonNull final String imageUrl, int avatarSz) {
        return rewriteAvatarUrl(imageUrl, avatarSz, DEFAULT_AVATAR);
    }
}
