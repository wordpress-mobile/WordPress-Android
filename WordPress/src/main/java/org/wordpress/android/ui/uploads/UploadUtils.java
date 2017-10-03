package org.wordpress.android.ui.uploads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.UploadStore.UploadError;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;

public class UploadUtils {
    /**
     * Returns a post-type specific error message string.
     */
    static @NonNull String getErrorMessage(Context context, PostModel post, String errorMessage, boolean isMediaError) {
        String baseErrorString;
        if (post.isPage()) {
            if (isMediaError) {
                baseErrorString = context.getString(R.string.error_upload_page_media_param);
            } else {
                baseErrorString = context.getString(R.string.error_upload_page_param);
            }
        } else {
            if (isMediaError) {
                baseErrorString = context.getString(R.string.error_upload_post_media_param);
            } else {
                baseErrorString = context.getString(R.string.error_upload_post_param);
            }
        }
        return String.format(baseErrorString, errorMessage);
    }

    /**
     * Returns an error message string for a failed post upload.
     */
    public static @NonNull String getErrorMessageFromPostError(Context context, PostModel post, PostError error) {
        switch (error.type) {
            case UNKNOWN_POST:
                return post.isPage() ? context.getString(R.string.error_unknown_page) : context.getString(R.string.error_unknown_post);
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

    public static boolean isMediaError(UploadError uploadError) {
        return uploadError != null && uploadError.mediaError != null;
    }

    public static void handleEditPostResultSnackbars(final Activity activity, View snackbarAttachView,
                                                     int resultCode, Intent data,
                                                     final PostModel post, final SiteModel site,
                                                     View.OnClickListener publishPostListener) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        boolean hasChanges = data.getBooleanExtra(EditPostActivity.EXTRA_HAS_CHANGES, false);
        if (!hasChanges) {
            // if there are no changes, we don't need to do anything
            return;
        }

        boolean savedLocally = data.getBooleanExtra(EditPostActivity.EXTRA_SAVED_AS_LOCAL_DRAFT, false);
        if (savedLocally && !NetworkUtils.isNetworkAvailable(activity)) {
            // The network is not available, we can't do anything
            ToastUtils.showToast(activity, R.string.error_publish_no_network,
                    ToastUtils.Duration.SHORT);
            return;
        }

        boolean hasFailedMedia = data.getBooleanExtra(EditPostActivity.EXTRA_HAS_FAILED_MEDIA, false);
        if (hasFailedMedia) {
            showSnackbar(snackbarAttachView, R.string.editor_post_saved_locally_failed_media, R.string.button_edit,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityLauncher.editPostOrPageForResult(activity, site, post);
                        }
                    });
            return;
        }

        boolean isScheduledPost = post != null && PostStatus.fromPost(post) == PostStatus.SCHEDULED;
        if (isScheduledPost) {
            // if it's a scheduled post, we only want to show a "Sync" button if it's locally saved
            if (savedLocally) {
                showSnackbar(snackbarAttachView, R.string.editor_post_saved_locally, R.string.button_sync,
                        publishPostListener);
            }
            return;
        }

        boolean isPublished = post != null && PostStatus.fromPost(post) == PostStatus.PUBLISHED;
        if (isPublished) {
            // if it's a published post, we only want to show a "Sync" button if it's locally saved
            if (savedLocally) {
                showSnackbar(snackbarAttachView, R.string.editor_post_saved_locally, R.string.button_sync,
                        publishPostListener);
            } else {
                showSnackbar(snackbarAttachView, R.string.editor_uploading_post);
            }
            return;
        }

        boolean isDraft = post != null && PostStatus.fromPost(post) == PostStatus.DRAFT;
        if (isDraft) {
            if (PostUtils.isPublishable(post)) {
                // if the post is publishable, we offer the PUBLISH button
                if (savedLocally) {
                    showSnackbar(snackbarAttachView, R.string.editor_draft_saved_locally, R.string.button_publish,
                            publishPostListener);
                }
                else {
                    if (UploadService.hasPendingOrInProgressMediaUploadsForPost(post) ||
                            UploadService.isPostUploadingOrQueued(post)) {
                        showSnackbar(snackbarAttachView, R.string.editor_uploading_post);
                    } else {
                        showSnackbar(snackbarAttachView, R.string.editor_draft_saved_online, R.string.button_publish,
                                publishPostListener);
                    }
                }
            } else {
                showSnackbar(snackbarAttachView, R.string.editor_draft_saved_locally);
            }
        } else {
            if (savedLocally) {
                showSnackbar(snackbarAttachView, R.string.editor_post_saved_locally, R.string.button_publish,
                        publishPostListener);
            }
            else {
                if (UploadService.hasPendingOrInProgressMediaUploadsForPost(post) ||
                        UploadService.isPostUploadingOrQueued(post)) {
                    showSnackbar(snackbarAttachView, R.string.editor_uploading_post);
                } else {
                    showSnackbar(snackbarAttachView, R.string.editor_post_saved_online, R.string.button_publish,
                            publishPostListener);
                }
            }
        }
    }

    private static void showSnackbar(View view, int messageRes, int buttonTitleRes,
                                     View.OnClickListener onClickListener) {
        Snackbar.make(view, messageRes, Snackbar.LENGTH_LONG)
                .setAction(buttonTitleRes, onClickListener).show();
    }

    private static void showSnackbar(View view, int messageRes) {
        Snackbar.make(view,
                messageRes, Snackbar.LENGTH_LONG).show();
    }

    public static void publishPost(Activity activity, final PostModel post, SiteModel site, Dispatcher dispatcher) {
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            ToastUtils.showToast(activity, R.string.error_publish_no_network,
                    ToastUtils.Duration.SHORT);
            return;
        }

        // If the post is empty, don't publish
        if (!PostUtils.isPublishable(post)) {
            String message = activity.getString(post.isPage() ? R.string.error_publish_empty_page : R.string.error_publish_empty_post);
            ToastUtils.showToast(activity, message, ToastUtils.Duration.SHORT);
            return;
        }

        PostUtils.updatePublishDateIfShouldBePublishedImmediately(post);
        boolean isFirstTimePublish = PostStatus.fromPost(post) == PostStatus.DRAFT
                || (PostStatus.fromPost(post) == PostStatus.PUBLISHED && post.isLocalDraft());
        post.setStatus(PostStatus.PUBLISHED.toString());

        // save the post in the DB so the UploadService will get the latest change
        dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));

        if (isFirstTimePublish) {
            UploadService.uploadPostAndTrackAnalytics(activity, post);
        } else {
            UploadService.uploadPost(activity, post);
        }

        PostUtils.trackSavePostAnalytics(post, site);
    }

    public static void onPostUploadedSnackbarHandler(final Activity activity, View snackbarAttachView,
                                                     PostStore.OnPostUploaded event,
                                                     final SiteModel site, final Dispatcher dispatcher) {
        final PostModel post = event.post;
        if (event.isError()) {
            UploadUtils.showSnackbar(snackbarAttachView, R.string.editor_draft_saved_locally);
        } else {
            boolean isDraft = PostStatus.fromPost(post) == PostStatus.DRAFT;
            if (isDraft) {
                View.OnClickListener publishPostListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        UploadUtils.publishPost(activity, post, site, dispatcher);
                    }
                };
                UploadUtils.showSnackbar(snackbarAttachView, R.string.editor_draft_saved_online,
                        R.string.button_publish, publishPostListener);
            } else {
                int messageRes = post.isPage() ? R.string.page_published : R.string.post_published;
                UploadUtils.showSnackbar(snackbarAttachView, messageRes);
            }
        }
    }
}
