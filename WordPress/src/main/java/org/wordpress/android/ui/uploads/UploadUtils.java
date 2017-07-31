package org.wordpress.android.ui.uploads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.util.WPMediaUtils;

public class UploadUtils {
    /**
     * Returns a post-type specific error message string.
     */
    static @NonNull String getErrorMessage(Context context, PostModel post, String errorMessage, boolean isMediaError) {
        String baseErrorString = context.getString(
                isMediaError ? R.string.error_upload_post_media_params : R.string.error_upload_post_params);
        String postType = context.getString(post.isPage() ? R.string.page : R.string.post).toLowerCase();
        return String.format(baseErrorString, postType, errorMessage);
    }

    /**
     * Returns an error message string for a failed post upload.
     */
    public static @NonNull String getErrorMessageFromPostError(Context context, PostModel post, PostError error) {
        switch (error.type) {
            case UNKNOWN_POST:
                return context.getString(R.string.error_unknown_post);
            case UNKNOWN_POST_TYPE:
                return context.getString(R.string.error_unknown_post_type);
            case UNAUTHORIZED:
                return post.isPage() ? context.getString(R.string.error_refresh_unauthorized_pages) :
                        context.getString(R.string.error_refresh_unauthorized_posts);
        }
        // In case of a generic or uncaught error, return the message from the API response or the error type
        return TextUtils.isEmpty(error.message) ? error.type.toString() : error.message;
    }

    /**
     * Returns an error message string for a failed media upload.
     */
    public static @NonNull String getErrorMessageFromMediaError(Context context, MediaModel media, MediaError error) {
        String errorMessage = WPMediaUtils.getErrorMessage(context, media, error);

        if (errorMessage == null) {
            // In case of a generic or uncaught error, return the message from the API response or the error type
            errorMessage = TextUtils.isEmpty(error.message) ? error.type.toString() : error.message;
        }

        return errorMessage;
    }
}
