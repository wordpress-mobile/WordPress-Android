package org.wordpress.android.ui.uploads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.PostStore.PostError;

public class UploadUtils {
    /**
     * Returns a post-type specific error message string.
     */
    static @NonNull String getErrorMessage(Context context, PostModel post, String specificMessage) {
        String postType = context.getString(post.isPage() ? R.string.page : R.string.post).toLowerCase();
        return String.format(context.getText(R.string.error_upload_params).toString(), postType, specificMessage);
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
    public static @NonNull String getErrorMessageFromMediaError(Context context, MediaError error) {
        switch (error.type) {
            case FS_READ_PERMISSION_DENIED:
                return context.getString(R.string.error_media_insufficient_fs_permissions);
            case NOT_FOUND:
                return context.getString(R.string.error_media_not_found);
            case AUTHORIZATION_REQUIRED:
                return context.getString(R.string.error_media_unauthorized);
            case PARSE_ERROR:
                return context.getString(R.string.error_media_parse_error);
            case REQUEST_TOO_LARGE:
                return context.getString(R.string.error_media_request_too_large);
            case SERVER_ERROR:
                return context.getString(R.string.media_error_internal_server_error);
            case TIMEOUT:
                return context.getString(R.string.media_error_timeout);
        }

        // In case of a generic or uncaught error, return the message from the API response or the error type
        return TextUtils.isEmpty(error.message) ? error.type.toString() : error.message;
    }
}
