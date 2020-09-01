package org.wordpress.android.ui.uploads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.UploadStore.UploadError;
import org.wordpress.android.fluxc.utils.MimeTypes;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction;
import org.wordpress.android.ui.utils.UiString;
import org.wordpress.android.ui.utils.UiString.UiStringRes;
import org.wordpress.android.ui.utils.UiString.UiStringText;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.SnackbarItem;
import org.wordpress.android.util.SnackbarItem.Action;
import org.wordpress.android.util.SnackbarItem.Info;
import org.wordpress.android.util.SnackbarSequencer;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UploadWorkerKt;
import org.wordpress.android.util.WPMediaUtils;

import java.util.ArrayList;
import java.util.List;

public class UploadUtils {
    private static final int K_SNACKBAR_WAIT_TIME_MS = 5000;
    private static final MimeTypes MIME_TYPES = new MimeTypes();

    /**
     * Returns a post-type specific error message string.
     */
    static @NonNull
    String getErrorMessage(Context context, boolean isPage, String errorMessage,
                           boolean isMediaError) {
        String baseErrorString;
        if (isPage) {
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
    public static @NonNull
    UiString getErrorMessageResIdFromPostError(PostStatus postStatus, boolean isPage, PostError error,
                                               boolean eligibleForAutoUpload) {
        switch (error.type) {
            case UNKNOWN_POST:
                return isPage ? new UiStringRes(R.string.error_unknown_page)
                        : new UiStringRes(R.string.error_unknown_post);
            case UNKNOWN_POST_TYPE:
                return isPage ? new UiStringRes(R.string.error_unknown_page_type)
                        : new UiStringRes(R.string.error_unknown_post_type);
            case UNAUTHORIZED:
                return isPage ? new UiStringRes(R.string.error_refresh_unauthorized_pages)
                        : new UiStringRes(R.string.error_refresh_unauthorized_posts);
            case UNSUPPORTED_ACTION:
            case INVALID_RESPONSE:
            case GENERIC_ERROR:
            default:
                AppLog.w(T.MAIN, "Error message: " + error.message + " ,Error Type: " + error.type);
                if (eligibleForAutoUpload) {
                    switch (postStatus) {
                        case PRIVATE:
                            return isPage ? new UiStringRes(R.string.error_page_not_published_retrying_private)
                                    : new UiStringRes(R.string.error_post_not_published_retrying_private);
                        case PUBLISHED:
                            return isPage ? new UiStringRes(R.string.error_page_not_published_retrying)
                                    : new UiStringRes(R.string.error_post_not_published_retrying);
                        case SCHEDULED:
                            return isPage ? new UiStringRes(R.string.error_page_not_scheduled_retrying)
                                    : new UiStringRes(R.string.error_post_not_scheduled_retrying);
                        case PENDING:
                            return isPage ? new UiStringRes(R.string.error_page_not_submitted_retrying)
                                    : new UiStringRes(R.string.error_post_not_submitted_retrying);
                        case UNKNOWN:
                        case DRAFT:
                        case TRASHED:
                            return new UiStringRes(R.string.error_generic_error_retrying);
                    }
                } else {
                    switch (postStatus) {
                        case PRIVATE:
                            return isPage ? new UiStringRes(R.string.error_page_not_published_private)
                                    : new UiStringRes(R.string.error_post_not_published_private);
                        case PUBLISHED:
                            return isPage ? new UiStringRes(R.string.error_page_not_published)
                                    : new UiStringRes(R.string.error_post_not_published);
                        case SCHEDULED:
                            return isPage ? new UiStringRes(R.string.error_page_not_scheduled)
                                    : new UiStringRes(R.string.error_post_not_scheduled);
                        case PENDING:
                            return isPage ? new UiStringRes(R.string.error_page_not_submitted)
                                    : new UiStringRes(R.string.error_post_not_submitted);
                        case UNKNOWN:
                        case DRAFT:
                        case TRASHED:
                            return new UiStringRes(R.string.error_generic_error);
                    }
                }
                return new UiStringRes(R.string.error_generic_error);
        }
    }

    /**
     * Returns an error message string for a failed media upload.
     */
    public static @NonNull
    String getErrorMessageFromMediaError(Context context, MediaModel media, MediaError error) {
        String errorMessage = WPMediaUtils.getErrorMessage(context, media, error);

        if (errorMessage == null) {
            // In case of a generic or uncaught error, return the message from the API response or the error type
            String msg = error.getApiUserMessageIfAvailable();
            errorMessage = TextUtils.isEmpty(msg) ? error.type.toString() : msg;
        }

        return errorMessage;
    }

    public static boolean isMediaError(UploadError uploadError) {
        return uploadError != null && uploadError.mediaError != null;
    }

    public static void handleEditPostModelResultSnackbars(@NonNull final Activity activity,
                                                          @NonNull final Dispatcher dispatcher,
                                                          @NonNull View snackbarAttachView,
                                                          @NonNull Intent data,
                                                          @NonNull final PostModel post,
                                                          @NonNull final SiteModel site,
                                                          @NonNull final UploadAction uploadAction,
                                                          SnackbarSequencer sequencer,
                                                          View.OnClickListener publishPostListener) {
        boolean hasChanges = data.getBooleanExtra(EditPostActivity.EXTRA_HAS_CHANGES, false);
        if (!hasChanges) {
            // if there are no changes, we don't need to do anything
            return;
        }

        boolean uploadNotStarted = data.getBooleanExtra(EditPostActivity.EXTRA_UPLOAD_NOT_STARTED, false);
        if (uploadNotStarted && !NetworkUtils.isNetworkAvailable(activity)) {
            // The network is not available, we can enqueue a request to upload local changes later
            UploadWorkerKt.enqueueUploadWorkRequestForSite(site);
            // And tell the user about it
            showSnackbar(snackbarAttachView, getDeviceOfflinePostModelNotUploadedMessage(post, uploadAction),
                    R.string.cancel,
                    v -> {
                        int msgRes = cancelPendingAutoUpload(post, dispatcher);
                        showSnackbar(snackbarAttachView, msgRes, sequencer);
                    }, sequencer);
            return;
        }

        boolean hasFailedMedia = data.getBooleanExtra(EditPostActivity.EXTRA_HAS_FAILED_MEDIA, false);
        if (hasFailedMedia) {
            showSnackbar(snackbarAttachView, post.isPage() ? R.string.editor_page_saved_locally_failed_media
                            : R.string.editor_post_saved_locally_failed_media, R.string.button_edit,
                         new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 ActivityLauncher.editPostOrPageForResult(activity, site, post);
                             }
                         }, sequencer);
            return;
        }

        PostStatus postStatus = PostStatus.fromPost(post);

        boolean isScheduledPost = postStatus == PostStatus.SCHEDULED;
        if (isScheduledPost) {
            // if it's a scheduled post, we only want to show a "Sync" button if it's locally saved
            if (uploadNotStarted) {
                showSnackbar(snackbarAttachView,
                        post.isPage() ? R.string.editor_page_saved_locally : R.string.editor_post_saved_locally,
                        R.string.button_sync,
                        publishPostListener, sequencer);
            }
            return;
        }

        boolean isPublished = postStatus == PostStatus.PUBLISHED;
        if (isPublished) {
            // if it's a published post, we only want to show a "Sync" button if it's locally saved
            if (uploadNotStarted) {
                showSnackbar(snackbarAttachView,
                        post.isPage() ? R.string.editor_page_saved_locally : R.string.editor_post_saved_locally,
                        R.string.button_sync,
                        publishPostListener, sequencer);
            } else {
                showSnackbar(snackbarAttachView,
                        post.isPage() ? R.string.editor_uploading_page : R.string.editor_uploading_post, sequencer);
            }
            return;
        }

        boolean isDraft = postStatus == PostStatus.DRAFT;
        if (isDraft) {
            if (PostUtils.isPublishable(post)) {
                // if the post is publishable, we offer the PUBLISH button
                if (uploadNotStarted) {
                    showSnackbarSuccessAction(snackbarAttachView, R.string.editor_draft_saved_locally,
                                              R.string.button_publish,
                                              publishPostListener, sequencer);
                } else {
                    if (UploadService.hasPendingOrInProgressMediaUploadsForPost(post)
                        || UploadService.isPostUploadingOrQueued(post)) {
                        showSnackbar(snackbarAttachView, R.string.editor_uploading_draft, sequencer);
                    } else {
                        showSnackbarSuccessAction(snackbarAttachView, R.string.editor_draft_saved_online,
                                                  R.string.button_publish,
                                                  publishPostListener, sequencer);
                    }
                }
            } else {
                showSnackbar(snackbarAttachView, R.string.editor_draft_saved_locally, sequencer);
            }
        } else {
            if (uploadNotStarted) {
                showSnackbar(snackbarAttachView,
                        post.isPage() ? R.string.editor_page_saved_locally : R.string.editor_post_saved_locally,
                        R.string.button_publish,
                             publishPostListener, sequencer);
            } else {
                if (UploadService.hasPendingOrInProgressMediaUploadsForPost(post)
                    || UploadService.isPostUploadingOrQueued(post)) {
                    showSnackbar(snackbarAttachView,
                            post.isPage() ? R.string.editor_uploading_page : R.string.editor_uploading_post, sequencer);
                } else {
                    showSnackbarSuccessAction(snackbarAttachView,
                            post.isPage() ? R.string.editor_page_saved_online : R.string.editor_post_saved_online,
                                              R.string.button_publish,
                                              publishPostListener, sequencer);
                }
            }
        }
    }

    public static void showSnackbarError(View view, String message, int buttonTitleRes,
                                          OnClickListener onClickListener, SnackbarSequencer sequencer) {
        sequencer.enqueue(
                new SnackbarItem(
                        new Info(
                             view,
                             new UiStringText(message),
                             K_SNACKBAR_WAIT_TIME_MS
                        ),
                        new Action(
                             new UiStringRes(buttonTitleRes),
                             onClickListener
                        ),
                        null
                )
        );
    }

    public static void showSnackbarError(View view, String message, SnackbarSequencer sequencer) {
        sequencer.enqueue(
                new SnackbarItem(
                        new Info(
                                view,
                                new UiStringText(message),
                                K_SNACKBAR_WAIT_TIME_MS
                        ),
                        null,
                        null
                )
        );
    }

    private static void showSnackbar(View view, int messageRes, int buttonTitleRes,
                                     OnClickListener onClickListener, SnackbarSequencer sequencer) {
        sequencer.enqueue(
                new SnackbarItem(
                        new Info(
                                view,
                                new UiStringRes(messageRes),
                                K_SNACKBAR_WAIT_TIME_MS
                        ),
                        new Action(
                                new UiStringRes(buttonTitleRes),
                                onClickListener
                        ),
                        null
                )
        );
    }

    public static void showSnackbarSuccessAction(View view, int messageRes, int buttonTitleRes,
                                                 OnClickListener onClickListener, SnackbarSequencer sequencer) {
        sequencer.enqueue(
                new SnackbarItem(
                        new Info(
                                view,
                                new UiStringRes(messageRes),
                                K_SNACKBAR_WAIT_TIME_MS
                        ),
                        new Action(
                                new UiStringRes(buttonTitleRes),
                                onClickListener
                        ),
                        null
                )
        );
    }

    private static void showSnackbarSuccessAction(View view, String message, int buttonTitleRes,
                                                  OnClickListener onClickListener, SnackbarSequencer sequencer) {
        sequencer.enqueue(
                new SnackbarItem(
                        new Info(
                                view,
                                new UiStringText(message),
                                K_SNACKBAR_WAIT_TIME_MS
                        ),
                        new Action(
                                new UiStringRes(buttonTitleRes),
                                onClickListener
                        ),
                        null
                )
        );
    }

    public static void showSnackbarSuccessActionOrange(View view, int messageRes, int buttonTitleRes,
                                                       View.OnClickListener onClickListener,
                                                       SnackbarSequencer snackbarSequencer) {
        snackbarSequencer.enqueue(new SnackbarItem(
                new Info(
                        view,
                        new UiStringRes(messageRes),
                        Snackbar.LENGTH_LONG
                ),
                new Action(new UiStringRes(buttonTitleRes), onClickListener),
                null
        ));
    }

    public static void showSnackbar(View view, int messageRes, SnackbarSequencer sequencer) {
        sequencer.enqueue(
                new SnackbarItem(
                        new Info(
                                view,
                                new UiStringRes(messageRes),
                                Snackbar.LENGTH_LONG
                        ),
                        null,
                        null
                )
        );
    }

    public static void showSnackbar(View view, String messageText, SnackbarSequencer sequencer) {
        sequencer.enqueue(
                new SnackbarItem(
                        new Info(
                                view,
                                new UiStringText(messageText),
                                Snackbar.LENGTH_LONG
                        ),
                        null,
                        null
                )
        );
    }

    public static void publishPost(Activity activity, final PostModel post, SiteModel site, Dispatcher dispatcher) {
        // If the post is empty, don't publish
        if (!PostUtils.isPublishable(post)) {
            String message = activity.getString(
                    post.isPage() ? R.string.error_publish_empty_page : R.string.error_publish_empty_post);
            ToastUtils.showToast(activity, message, ToastUtils.Duration.SHORT);
            return;
        }

        boolean isFirstTimePublish = PostUtils.isFirstTimePublish(post);

        PostUtils.preparePostForPublish(post, site);

        // save the post in the DB so the UploadService will get the latest change
        dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));

        if (NetworkUtils.isNetworkAvailable(activity)) {
            UploadService.uploadPost(activity, post.getId(), isFirstTimePublish);
        }
        PostUtils.trackSavePostAnalytics(post, site);
    }

    /*
     * returns true if the user has permission to publish the post - assumed to be true for
     * dot.org sites because we can't retrieve their capabilities
     */
    public static boolean userCanPublish(SiteModel site) {
        return !SiteUtils.isAccessedViaWPComRest(site) || site.getHasCapabilityPublishPosts();
    }

    public static void onPostUploadedSnackbarHandler(final Activity activity, View snackbarAttachView,
                                                     boolean isError,
                                                     final PostModel post,
                                                     final String errorMessage,
                                                     final SiteModel site, final Dispatcher dispatcher,
                                                     SnackbarSequencer sequencer) {
        boolean userCanPublish = userCanPublish(site);
        if (isError) {
            if (errorMessage != null) {
                // RETRY only available for Aztec
                if (AppPrefs.isAztecEditorEnabled()) {
                    UploadUtils.showSnackbarError(snackbarAttachView, errorMessage, R.string.retry,
                                                  new View.OnClickListener() {
                                                      @Override
                                                      public void onClick(View view) {
                                                          Intent intent = UploadService.getRetryUploadServiceIntent(
                                                                  activity, post, false);
                                                          activity.startService(intent);
                                                      }
                                                  }, sequencer);
                } else {
                    UploadUtils.showSnackbarError(snackbarAttachView, errorMessage, sequencer);
                }
            } else {
                UploadUtils.showSnackbar(snackbarAttachView, R.string.editor_draft_saved_locally, sequencer);
            }
        } else {
            if (post != null) {
                PostStatus status = PostStatus.fromPost(post);
                int snackbarMessageRes;
                int snackbarButtonRes = 0;
                View.OnClickListener publishPostListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // jump to Editor Preview mode to show this Post
                        ActivityLauncher.browsePostOrPage(activity, site, post);
                    }
                };

                switch (status) {
                    case DRAFT:
                        snackbarMessageRes = R.string.editor_draft_saved_online;
                        if (userCanPublish) {
                            publishPostListener = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UploadUtils.publishPost(activity, post, site, dispatcher);
                                }
                            };
                            snackbarButtonRes = R.string.button_publish;
                        }
                        break;
                    case PUBLISHED:
                        snackbarButtonRes = R.string.button_view;

                        if (post.isPage()) {
                            snackbarMessageRes = R.string.page_published;
                        } else if (userCanPublish) {
                            snackbarMessageRes = R.string.post_published;
                        } else {
                            snackbarMessageRes = R.string.post_submitted;
                        }
                        break;
                    case SCHEDULED:
                        snackbarButtonRes = R.string.button_view;
                        snackbarMessageRes = post.isPage() ? R.string.page_scheduled : R.string.post_scheduled;
                        break;
                    default:
                        snackbarButtonRes = R.string.button_view;
                        snackbarMessageRes = post.isPage() ? R.string.page_updated : R.string.post_updated;
                        break;
                }

                if (snackbarButtonRes > 0) {
                    UploadUtils.showSnackbarSuccessAction(snackbarAttachView, snackbarMessageRes, snackbarButtonRes,
                            publishPostListener, sequencer);
                } else {
                    UploadUtils.showSnackbar(snackbarAttachView, snackbarMessageRes, sequencer);
                }
            }
        }
    }

    public static void onMediaUploadedSnackbarHandler(final Activity activity, View snackbarAttachView,
                                                      boolean isError,
                                                      final List<MediaModel> mediaList, final SiteModel site,
                                                      final String messageForUser,
                                                      SnackbarSequencer sequencer) {
        if (isError) {
            if (messageForUser != null) {
                // RETRY only available for Aztec
                if (mediaList != null && !mediaList.isEmpty()) {
                    UploadUtils.showSnackbarError(snackbarAttachView, messageForUser, R.string.retry,
                                                  new View.OnClickListener() {
                                                      @Override
                                                      public void onClick(View view) {
                                                          ArrayList<MediaModel> mediaListToRetry = new ArrayList<>();
                                                          mediaListToRetry.addAll(mediaList);
                                                          Intent retryIntent = UploadService
                                                                  .getUploadMediaServiceIntent(activity,
                                                                                               mediaListToRetry, true);
                                                          activity.startService(retryIntent);
                                                      }
                                                  }, sequencer);
                } else {
                    UploadUtils.showSnackbarError(snackbarAttachView, messageForUser, sequencer);
                }
            } else {
                UploadUtils.showSnackbarError(
                        snackbarAttachView,
                        activity.getString(R.string.error_media_upload),
                        sequencer
                );
            }
        } else {
            if (mediaList == null || mediaList.isEmpty()) {
                return;
            }
            boolean showPostAction = false;
            for (MediaModel mediaModel : mediaList) {
                showPostAction |= MIME_TYPES.isImageType(mediaModel.getMimeType()) || MIME_TYPES
                        .isVideoType(mediaModel.getMimeType());
            }
            if (showPostAction) {
                // show success snackbar for media only items and offer the WRITE POST functionality)
                UploadUtils.showSnackbarSuccessAction(snackbarAttachView, messageForUser,
                        R.string.media_files_uploaded_write_post, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // WRITE POST functionality: show pre-populated Post
                                ArrayList<MediaModel> mediaListToInsertInPost = new ArrayList<>();
                                mediaListToInsertInPost.addAll(mediaList);

                                Intent writePostIntent = new Intent(activity, EditPostActivity.class);
                                writePostIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                writePostIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                writePostIntent.putExtra(WordPress.SITE, site);
                                writePostIntent.putExtra(EditPostActivity.EXTRA_IS_PAGE, false);
                                writePostIntent.putExtra(EditPostActivity.EXTRA_INSERT_MEDIA, mediaListToInsertInPost);
                                activity.startActivity(writePostIntent);
                            }
                        }, sequencer);
            } else {
                // Do not show action for audio/document files until there is a block handling them in GB
                UploadUtils.showSnackbar(snackbarAttachView, messageForUser, sequencer);
            }
        }
    }

    @StringRes
    private static int getDeviceOfflinePostModelNotUploadedMessage(@NonNull final PostModel post,
                                                                   @NonNull final UploadAction uploadAction) {
        if (uploadAction != UploadAction.UPLOAD) {
            return post.isPage() ? R.string.error_publish_page_no_network : R.string.error_publish_no_network;
        } else {
            switch (PostStatus.fromPost(post)) {
                case PUBLISHED:
                case UNKNOWN:
                    return post.isPage() ? R.string.page_waiting_for_connection_publish
                            : R.string.post_waiting_for_connection_publish;
                case DRAFT:
                    return post.isPage() ? R.string.page_waiting_for_connection_draft
                            : R.string.post_waiting_for_connection_draft;
                case PRIVATE:
                    return post.isPage() ? R.string.page_waiting_for_connection_private
                            : R.string.post_waiting_for_connection_private;
                case PENDING:
                    return post.isPage() ? R.string.page_waiting_for_connection_pending
                            : R.string.post_waiting_for_connection_pending;
                case SCHEDULED:
                    return post.isPage() ? R.string.page_waiting_for_connection_scheduled
                            : R.string.post_waiting_for_connection_scheduled;
                case TRASHED:
                    throw new IllegalArgumentException("Trashing posts should be handled in a different code path.");
            }
        }
        throw new RuntimeException("This code should be unreachable. Missing case in switch statement.");
    }

    public static boolean postLocalChangesAlreadyRemoteAutoSaved(PostImmutableModel post) {
        return !TextUtils.isEmpty(post.getAutoSaveModified())
               && DateTimeUtils.dateFromIso8601(post.getDateLocallyChanged())
                               .before(DateTimeUtils.dateFromIso8601(post.getAutoSaveModified()));
    }

    public static int cancelPendingAutoUpload(PostModel post, Dispatcher dispatcher) {
        /*
         * `changesConfirmedContentHashcode` field holds a hashcode of the post content at the time when user pressed
         * updated/publish/sync/submit/.. buttons. Clearing the hashcode will prevent the PostUploadHandler to
         * auto-upload the changes - it'll only remote-auto-save them -> which is exactly what the cancel action is
         * supposed to do.
         */
        post.setChangesConfirmedContentHashcode(0);
        dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));

        int messageRes = 0;
        switch (PostStatus.fromPost(post)) {
            case UNKNOWN:
            case PUBLISHED:
            case PRIVATE:
                messageRes = R.string.post_waiting_for_connection_publish_cancel;
                break;
            case PENDING:
                messageRes = R.string.post_waiting_for_connection_pending_cancel;
                break;
            case SCHEDULED:
                messageRes = R.string.post_waiting_for_connection_scheduled_cancel;
                break;
            case DRAFT:
                messageRes = R.string.post_waiting_for_connection_draft_cancel;
                break;
            case TRASHED:
                AppLog.e(T.POSTS,
                        "This code should be unreachable. Canceling pending auto-upload on Trashed and Draft posts "
                        + "isn't supported.");
                break;
        }
        return messageRes;
    }
}
