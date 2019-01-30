package org.wordpress.android.ui.uploads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.UploadStore.UploadError;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;

import java.util.ArrayList;
import java.util.List;

public class UploadUtils {
    private static final int K_SNACKBAR_WAIT_TIME_MS = 5000;

    /**
     * Returns a post-type specific error message string.
     */
    static @NonNull
    String getErrorMessage(Context context, PostModel post, String errorMessage, boolean isMediaError) {
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
    public static @NonNull
    String getErrorMessageFromPostError(Context context, boolean isPage, PostError error) {
        switch (error.type) {
            case UNKNOWN_POST:
                return isPage ? context.getString(R.string.error_unknown_page)
                        : context.getString(R.string.error_unknown_post);
            case UNKNOWN_POST_TYPE:
                return context.getString(R.string.error_unknown_post_type);
            case UNAUTHORIZED:
                return isPage ? context.getString(R.string.error_refresh_unauthorized_pages)
                        : context.getString(R.string.error_refresh_unauthorized_posts);
        }
        // In case of a generic or uncaught error, return the message from the API response or the error type
        return TextUtils.isEmpty(error.message) ? error.type.toString() : error.message;
    }

    /**
     * Returns an error message string for a failed media upload.
     */
    public static @NonNull
    String getErrorMessageFromMediaError(Context context, MediaModel media, MediaError error) {
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

    public static void handleEditPostResultSnackbars(@NonNull final Activity activity,
                                                     @NonNull View snackbarAttachView,
                                                     @NonNull Intent data,
                                                     @NonNull final PostModel post,
                                                     @NonNull final SiteModel site,
                                                     View.OnClickListener publishPostListener) {
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

        PostStatus postStatus = PostStatus.fromPost(post);

        boolean isScheduledPost = postStatus == PostStatus.SCHEDULED;
        if (isScheduledPost) {
            // if it's a scheduled post, we only want to show a "Sync" button if it's locally saved
            if (savedLocally) {
                showSnackbar(snackbarAttachView, R.string.editor_post_saved_locally, R.string.button_sync,
                             publishPostListener);
            }
            return;
        }

        boolean isPublished = postStatus == PostStatus.PUBLISHED;
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

        boolean isDraft = postStatus == PostStatus.DRAFT;
        if (isDraft) {
            if (PostUtils.isPublishable(post)) {
                // if the post is publishable, we offer the PUBLISH button
                if (savedLocally) {
                    showSnackbarSuccessAction(snackbarAttachView, R.string.editor_draft_saved_locally,
                                              R.string.button_publish,
                                              publishPostListener);
                } else {
                    if (UploadService.hasPendingOrInProgressMediaUploadsForPost(post)
                        || UploadService.isPostUploadingOrQueued(post)) {
                        showSnackbar(snackbarAttachView, R.string.editor_uploading_post);
                    } else {
                        showSnackbarSuccessAction(snackbarAttachView, R.string.editor_draft_saved_online,
                                                  R.string.button_publish,
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
            } else {
                if (UploadService.hasPendingOrInProgressMediaUploadsForPost(post)
                    || UploadService.isPostUploadingOrQueued(post)) {
                    showSnackbar(snackbarAttachView, R.string.editor_uploading_post);
                } else {
                    showSnackbarSuccessAction(snackbarAttachView, R.string.editor_post_saved_online,
                                              R.string.button_publish,
                                              publishPostListener);
                }
            }
        }
    }

    private static void showSnackbarError(View view, String message, int buttonTitleRes,
                                          View.OnClickListener onClickListener) {
        Snackbar.make(view, message, AccessibilityUtils.getSnackbarDuration(view.getContext(), K_SNACKBAR_WAIT_TIME_MS))
                .setAction(buttonTitleRes, onClickListener).show();
    }

    public static void showSnackbarError(View view, String message) {
        Snackbar.make(view, message, K_SNACKBAR_WAIT_TIME_MS).show();
    }

    private static void showSnackbar(View view, int messageRes, int buttonTitleRes,
                                     View.OnClickListener onClickListener) {
        Snackbar.make(view, messageRes,
                AccessibilityUtils.getSnackbarDuration(view.getContext(), K_SNACKBAR_WAIT_TIME_MS))
                .setAction(buttonTitleRes, onClickListener).show();
    }

    public static void showSnackbarSuccessAction(View view, int messageRes, int buttonTitleRes,
                                                  View.OnClickListener onClickListener) {
        Snackbar.make(view, messageRes,
                AccessibilityUtils.getSnackbarDuration(view.getContext(), K_SNACKBAR_WAIT_TIME_MS))
                .setAction(buttonTitleRes, onClickListener).
                        setActionTextColor(view.getResources().getColor(R.color.blue_medium))
                .show();
    }

    private static void showSnackbarSuccessAction(View view, String message, int buttonTitleRes,
                                                  View.OnClickListener onClickListener) {
        Snackbar.make(view, message, AccessibilityUtils.getSnackbarDuration(view.getContext(), K_SNACKBAR_WAIT_TIME_MS))
                .setAction(buttonTitleRes, onClickListener).
                        setActionTextColor(view.getResources().getColor(R.color.blue_medium))
                .show();
    }

    public static void showSnackbarSuccessActionOrange(View view, int messageRes, int buttonTitleRes,
                                                  View.OnClickListener onClickListener) {
        Snackbar.make(view, messageRes, Snackbar.LENGTH_LONG)
                .setAction(buttonTitleRes, onClickListener).
                        setActionTextColor(view.getResources().getColor(R.color.orange_jazzy))
                .show();
    }

    public static void showSnackbar(View view, int messageRes) {
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
            String message = activity.getString(
                    post.isPage() ? R.string.error_publish_empty_page : R.string.error_publish_empty_post);
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
                                                     boolean isError,
                                                     final PostModel post,
                                                     final String errorMessage,
                                                     final SiteModel site, final Dispatcher dispatcher) {
        boolean userCanPublish = !SiteUtils.isAccessedViaWPComRest(site) || site.getHasCapabilityPublishPosts();
        if (isError) {
            if (errorMessage != null) {
                // RETRY only available for Aztec
                if (AppPrefs.isAztecEditorEnabled()) {
                    UploadUtils.showSnackbarError(snackbarAttachView, errorMessage, R.string.retry,
                                                  new View.OnClickListener() {
                                                      @Override
                                                      public void onClick(View view) {
                                                          Intent intent = UploadService.getUploadPostServiceIntent(
                                                                  activity, post, PostUtils.isFirstTimePublish(post),
                                                                  false, true);
                                                          activity.startService(intent);
                                                      }
                                                  });
                } else {
                    UploadUtils.showSnackbarError(snackbarAttachView, errorMessage);
                }
            } else {
                UploadUtils.showSnackbar(snackbarAttachView, R.string.editor_draft_saved_locally);
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
                            publishPostListener);
                } else {
                    UploadUtils.showSnackbar(snackbarAttachView, snackbarMessageRes);
                }
            }
        }
    }

    public static void onMediaUploadedSnackbarHandler(final Activity activity, View snackbarAttachView,
                                                      boolean isError,
                                                      final List<MediaModel> mediaList, final SiteModel site,
                                                      final String messageForUser) {
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
                                                  });
                } else {
                    UploadUtils.showSnackbarError(snackbarAttachView, messageForUser);
                }
            } else {
                UploadUtils.showSnackbarError(snackbarAttachView, activity.getString(R.string.error_media_upload));
            }
        } else {
            if (mediaList == null || mediaList.isEmpty()) {
                return;
            }

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
                    });
        }
    }
}
