package org.wordpress.android.fluxc.store;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.action.UploadAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.fluxc.store.PostStore.RemoteAutoSavePostPayload;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UploadStore extends Store {
    public static class ClearMediaPayload extends Payload<BaseNetworkError> {
        public PostImmutableModel post;
        public Set<MediaModel> media;
        public ClearMediaPayload(PostImmutableModel post, Set<MediaModel> media) {
            this.post = post;
            this.media = media;
        }
    }

    public static class OnUploadChanged extends OnChanged<UploadError> {
        public UploadAction cause;

        public OnUploadChanged(UploadAction cause) {
            this(cause, null);
        }

        public OnUploadChanged(UploadAction cause, UploadError error) {
            this.cause = cause;
            this.error = error;
        }
    }

    public static class UploadError implements OnChangedError {
        public PostError postError;
        public MediaError mediaError;

        public UploadError(PostError postError) {
            this.postError = postError;
        }

        public UploadError(MediaError mediaError) {
            this.mediaError = mediaError;
        }
    }

    @Inject
    public UploadStore(Dispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "UploadStore onRegister");
    }

    // Ensure that events reach the UploadStore before their main stores (MediaStore, PostStore)
    @Subscribe(threadMode = ThreadMode.ASYNC, priority = 1)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (actionType instanceof UploadAction) {
            onUploadAction((UploadAction) actionType, action.getPayload());
        } else if (actionType instanceof MediaAction) {
            onMediaAction((MediaAction) actionType, action.getPayload());
        }
    }

    private void onUploadAction(UploadAction actionType, Object payload) {
        switch (actionType) {
            case UPLOADED_MEDIA:
                handleMediaUploaded((ProgressPayload) payload);
                mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction((ProgressPayload) payload));
                break;
            case PUSHED_POST:
                handlePostUploaded((RemotePostPayload) payload);
                mDispatcher.dispatch(PostActionBuilder.newPushedPostAction((RemotePostPayload) payload));
                break;
            case REMOTE_AUTO_SAVED_POST:
                handleRemoteAutoSavedPost((RemoteAutoSavePostPayload) payload);
                mDispatcher
                        .dispatch(PostActionBuilder.newRemoteAutoSavedPostAction((RemoteAutoSavePostPayload) payload));
                break;
            case INCREMENT_NUMBER_OF_AUTO_UPLOAD_ATTEMPTS:
                handleIncrementNumberOfAutoUploadAttempts((PostImmutableModel) payload);
                break;
            case CANCEL_POST:
                handleCancelPost((PostImmutableModel) payload);
                break;
            case CLEAR_MEDIA_FOR_POST:
                handleClearMediaForPost((ClearMediaPayload) payload);
                break;
        }
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases")
    private void onMediaAction(MediaAction actionType, Object payload) {
        switch (actionType) {
            case UPLOAD_MEDIA:
                handleUploadMedia((MediaPayload) payload);
                break;
            case CANCEL_MEDIA_UPLOAD:
                handleCancelMedia((CancelMediaPayload) payload);
                break;
            case UPDATE_MEDIA:
                handleUpdateMedia((MediaModel) payload);
                break;
        }
    }

    public void registerPostModel(PostImmutableModel postModel, List<MediaModel> mediaModelList) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId());
        Set<Integer> mediaIdSet = new HashSet<>();

        if (postUploadModel != null) {
            // Keep any existing media associated with this post
            mediaIdSet.addAll(postUploadModel.getAssociatedMediaIdSet());
        } else {
            postUploadModel = new PostUploadModel(postModel.getId());
        }

        for (MediaModel mediaModel : mediaModelList) {
            mediaIdSet.add(mediaModel.getId());
        }

        postUploadModel.setAssociatedMediaIdSet(mediaIdSet);
        postUploadModel.setUploadState(PostUploadModel.PENDING);
        UploadSqlUtils.insertOrUpdatePost(postUploadModel);
    }

    public @NonNull Set<MediaModel> getUploadingMediaForPost(PostImmutableModel post) {
        return getMediaForPostWithState(post, MediaUploadModel.UPLOADING);
    }

    public @NonNull Set<MediaModel> getCompletedMediaForPost(PostImmutableModel post) {
        return getMediaForPostWithState(post, MediaUploadModel.COMPLETED);
    }

    public @NonNull Set<MediaModel> getFailedMediaForPost(PostImmutableModel post) {
        return getMediaForPostWithState(post, MediaUploadModel.FAILED);
    }

    public @NonNull List<PostModel> getPendingPosts() {
        List<PostUploadModel> postUploadModels = UploadSqlUtils.getPostUploadModelsWithState(PostUploadModel.PENDING);
        return UploadSqlUtils.getPostModelsForPostUploadModels(postUploadModels);
    }

    public @NonNull List<PostModel> getFailedPosts() {
        List<PostUploadModel> postUploadModels = UploadSqlUtils.getPostUploadModelsWithState(PostUploadModel.FAILED);
        return UploadSqlUtils.getPostModelsForPostUploadModels(postUploadModels);
    }

    public @NonNull List<PostModel> getCancelledPosts() {
        List<PostUploadModel> postUploadModels = UploadSqlUtils.getPostUploadModelsWithState(PostUploadModel.CANCELLED);
        return UploadSqlUtils.getPostModelsForPostUploadModels(postUploadModels);
    }

    public @NonNull List<PostModel> getAllRegisteredPosts() {
        List<PostUploadModel> postUploadModels = UploadSqlUtils.getAllPostUploadModels();
        return UploadSqlUtils.getPostModelsForPostUploadModels(postUploadModels);
    }

    public boolean isPendingPost(PostImmutableModel post) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(post.getId());
        return postUploadModel != null && postUploadModel.getUploadState() == PostUploadModel.PENDING;
    }

    public boolean isFailedPost(PostImmutableModel post) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(post.getId());
        return postUploadModel != null && postUploadModel.getUploadState() == PostUploadModel.FAILED;
    }

    public boolean isCancelledPost(PostImmutableModel post) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(post.getId());
        return postUploadModel != null && postUploadModel.getUploadState() == PostUploadModel.CANCELLED;
    }

    public boolean isRegisteredPostModel(PostImmutableModel post) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(post.getId());
        return postUploadModel != null;
    }

    public int getNumberOfPostAutoUploadAttempts(PostImmutableModel post) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(post.getId());
        if (postUploadModel == null) {
            return 0;
        }
        return postUploadModel.getNumberOfAutoUploadAttempts();
    }

    /**
     * If the {@code postModel} has been registered as uploading with the UploadStore, this will return the associated
     * {@link PostError}, if any.
     * Otherwise, whether or not the {@code postModel} has been registered as uploading with the UploadStore, this
     * will check all media attached to the {@code postModel} and will return the first error it finds.
     */
    public @Nullable UploadError getUploadErrorForPost(PostImmutableModel postModel) {
        if (postModel == null) return null;

        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId());
        if (postUploadModel == null) {
            // If there's no matching PostUploadModel, we might still have associated MediaUploadModels that have errors
            Set<MediaUploadModel> mediaUploadModels = UploadSqlUtils.getMediaUploadModelsForPostId(postModel.getId());
            for (MediaUploadModel mediaUploadModel : mediaUploadModels) {
                if (mediaUploadModel.getMediaError() != null) {
                    return new UploadError(mediaUploadModel.getMediaError());
                }
            }
            return null;
        }

        if (postUploadModel.getPostError() != null) {
            return new UploadError(postUploadModel.getPostError());
        } else {
            for (int localMediaId : postUploadModel.getAssociatedMediaIdSet()) {
                MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(localMediaId);
                if (mediaUploadModel != null && mediaUploadModel.getMediaError() != null) {
                    return new UploadError(mediaUploadModel.getMediaError());
                }
            }
        }
        return null;
    }

    public float getUploadProgressForMedia(MediaModel mediaModel) {
        MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
        if (mediaUploadModel != null) {
            return mediaUploadModel.getProgress();
        }
        return 0;
    }

    private void handleUploadMedia(MediaPayload payload) {
        MediaUploadModel mediaUploadModel = new MediaUploadModel(payload.media.getId());
        String errorMessage = MediaUtils.getMediaValidationError(payload.media);
        if (errorMessage != null) {
            mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
            mediaUploadModel.setMediaError(new MediaError(MediaErrorType.MALFORMED_MEDIA_ARG, errorMessage));
        }
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
    }

    private void handleMediaUploaded(@NonNull ProgressPayload payload) {
        if (payload.media == null) {
            return;
        }

        MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(payload.media.getId());
        if (mediaUploadModel == null) {
            if (!payload.isError() && !payload.canceled && !payload.completed) {
                // This is a progress event, and the upload seems to have already been cancelled
                // We don't want to store a new MediaUploadModel in this case, just move on
                return;
            }
            mediaUploadModel = new MediaUploadModel(payload.media.getId());
        }

        if (payload.isError() || payload.canceled) {
            mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
            if (payload.isError()) {
                mediaUploadModel.setMediaError(payload.error);
            }
            UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
            if (payload.media.getLocalPostId() > 0) {
                cancelPost(payload.media.getLocalPostId());
            }
            return;
        }

        if (payload.completed) {
            mediaUploadModel.setUploadState(MediaUploadModel.COMPLETED);
            mediaUploadModel.setProgress(1F);
            UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
        } else {
            if (mediaUploadModel.getProgress() < payload.progress) {
                mediaUploadModel.setProgress(payload.progress);
                // To avoid conflicts with another action handler updating the state of the MediaUploadModel,
                // update the progress value only, since that's all the new information this event gives us
                UploadSqlUtils.updateMediaProgressOnly(mediaUploadModel);
            }
        }
    }

    private void handleCancelMedia(@NonNull CancelMediaPayload payload) {
        if (payload.media == null) {
            return;
        }

        // If the cancel action has the delete flag, the corresponding MediaModel will be deleted once this action
        // reaches the MediaStore, along with the MediaUploadModel (because of the FOREIGN KEY association)
        // Otherwise, we should mark the MediaUploadModel as FAILED
        if (!payload.delete) {
            MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(payload.media.getId());
            if (mediaUploadModel == null) {
                mediaUploadModel = new MediaUploadModel(payload.media.getId());
            }

            mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
            UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
        }

        if (payload.media.getLocalPostId() > 0) {
            cancelPost(payload.media.getLocalPostId());
        }
    }

    private void handleUpdateMedia(@NonNull MediaModel payload) {
        MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(payload.getId());
        if (mediaUploadModel == null) {
            return;
        }

        // If the new MediaModel state is different from ours, update the MediaUploadModel to reflect it
        MediaUploadState newUploadState = MediaUploadState.fromString(payload.getUploadState());
        switch (mediaUploadModel.getUploadState()) {
            case MediaUploadModel.UPLOADING:
                if (newUploadState == MediaUploadState.FAILED) {
                    mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
                    mediaUploadModel.setMediaError(new MediaError(MediaErrorType.GENERIC_ERROR));
                    mediaUploadModel.setProgress(0);
                    UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
                    // Also cancel the associated post
                    if (payload.getLocalPostId() > 0) {
                        cancelPost(payload.getLocalPostId());
                    }
                }
                break;
            case MediaUploadModel.COMPLETED:
                // We never care about changes to MediaModels that are already COMPLETED
                break;
            case MediaUploadModel.FAILED:
                if (newUploadState == MediaUploadState.UPLOADING || newUploadState == MediaUploadState.QUEUED) {
                    mediaUploadModel.setUploadState(MediaUploadModel.UPLOADING);
                    mediaUploadModel.setMediaError(null); // clear any previous errors
                    UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
                }
                break;
        }
    }

    private void handleRemoteAutoSavedPost(@NonNull RemoteAutoSavePostPayload payload) {
        if (payload.error != null && payload.error.type == PostErrorType.UNSUPPORTED_ACTION) {
            // The remote-auto-save is not supported -> lets just delete the post from the queue
            UploadSqlUtils.deletePostUploadModelWithLocalId(payload.localPostId);
        } else {
            handlePostUploadedOrAutoSaved(payload.localPostId, payload.error);
        }
    }

    private void handlePostUploaded(@NonNull RemotePostPayload payload) {
        if (payload.post == null) {
            return;
        }

        handlePostUploadedOrAutoSaved(payload.post.getId(), payload.error);
    }

    private void handlePostUploadedOrAutoSaved(int localPostId, PostError error) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(localPostId);

        if (error != null) {
            if (postUploadModel == null) {
                postUploadModel = new PostUploadModel(localPostId);
            }
            if (postUploadModel.getUploadState() != PostUploadModel.FAILED) {
                postUploadModel.setUploadState(PostUploadModel.FAILED);
            }
            postUploadModel.setPostError(error);
            UploadSqlUtils.insertOrUpdatePost(postUploadModel);
            return;
        }

        if (postUploadModel != null) {
            // Delete all MediaUploadModels associated with this post since we're finished with it
            UploadSqlUtils.deleteMediaUploadModelsWithLocalIds(postUploadModel.getAssociatedMediaIdSet());

            // Delete the PostUploadModel itself
            UploadSqlUtils.deletePostUploadModelWithLocalId(localPostId);
        }
    }

    private void handleIncrementNumberOfAutoUploadAttempts(PostImmutableModel post) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(post.getId());
        if (postUploadModel != null) {
            postUploadModel.incNumberOfAutoUploadAttempts();
            UploadSqlUtils.insertOrUpdatePost(postUploadModel);
        }
    }

    private void handleCancelPost(PostImmutableModel payload) {
        if (payload != null) {
            cancelPost(payload.getId());
        }
        emitChange(new OnUploadChanged(UploadAction.CANCEL_POST));
    }

    private void handleClearMediaForPost(ClearMediaPayload payload) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(payload.post.getId());
        if (postUploadModel == null) {
            return;
        }

        // Remove media from the list of associated media for the post
        Set<Integer> associatedMediaIdList = postUploadModel.getAssociatedMediaIdSet();
        for (MediaModel mediaModel : payload.media) {
            associatedMediaIdList.remove(mediaModel.getId());
        }
        postUploadModel.setAssociatedMediaIdSet(associatedMediaIdList);
        UploadSqlUtils.insertOrUpdatePost(postUploadModel);

        // Clear the MediaUploadModels
        Set<Integer> localMediaIds = new HashSet<>();
        for (MediaModel mediaModel : payload.media) {
            localMediaIds.add(mediaModel.getId());
        }
        UploadSqlUtils.deleteMediaUploadModelsWithLocalIds(localMediaIds);

        emitChange(new OnUploadChanged(UploadAction.CLEAR_MEDIA_FOR_POST));
    }

    private @NonNull Set<MediaModel> getMediaForPostWithState(PostImmutableModel post,
                                                              @MediaUploadModel.UploadState int state) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(post.getId());
        if (postUploadModel == null) {
            return Collections.emptySet();
        }

        Set<MediaModel> mediaModels = new HashSet<>();
        for (int localMediaId : postUploadModel.getAssociatedMediaIdSet()) {
            MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(localMediaId);
            if (mediaUploadModel != null && mediaUploadModel.getUploadState() == state) {
                mediaModels.add(MediaSqlUtils.getMediaWithLocalId(localMediaId));
            }
        }
        return mediaModels;
    }

    private void cancelPost(int localPostId) {
        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(localPostId);
        if (postUploadModel != null && postUploadModel.getUploadState() != PostUploadModel.CANCELLED) {
            postUploadModel.setUploadState(PostUploadModel.CANCELLED);
            UploadSqlUtils.insertOrUpdatePost(postUploadModel);
        }
    }
}
