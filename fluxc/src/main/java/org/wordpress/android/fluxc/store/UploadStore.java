package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.action.UploadAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.store.PostStore.PostError;
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
    public static class ClearMediaPayload extends Payload {
        public PostModel post;
        public Set<MediaModel> media;
        public ClearMediaPayload(PostModel post, Set<MediaModel> media) {
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
        if (actionType instanceof MediaAction) {
            onMediaAction((MediaAction) actionType, action.getPayload());
        } else if (actionType instanceof PostAction) {
            onPostAction((PostAction) actionType, action.getPayload());
        } else if (actionType instanceof UploadAction) {
            onUploadAction((UploadAction) actionType, action.getPayload());
        }
    }

    private void onMediaAction(MediaAction actionType, Object payload) {
        switch (actionType) {
            case UPLOAD_MEDIA:
                handleUploadMedia((MediaPayload) payload);
                break;
            case UPLOADED_MEDIA:
                handleMediaUploaded((ProgressPayload) payload);
                break;
            case CANCEL_MEDIA_UPLOAD:
                handleCancelMedia((CancelMediaPayload) payload);
                break;
        }
    }

    private void onPostAction(PostAction actionType, Object payload) {
        switch (actionType) {
            case PUSHED_POST:
                handlePostUploaded((RemotePostPayload) payload);
                break;
        }
    }

    private void onUploadAction(UploadAction actionType, Object payload) {
        switch (actionType) {
            case CLEAR_MEDIA:
                handleClearMediaAction((ClearMediaPayload) payload);
                break;
        }
    }

    // TODO Might be better never to return UploadModels and instead have methods like getUploadProgressForMedia()
    public MediaUploadModel getMediaUploadModelForMediaModel(MediaModel mediaModel) {
        return UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
    }

    public void registerPostModel(PostModel postModel, List<MediaModel> mediaModelList) {
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
        UploadSqlUtils.insertOrUpdatePost(postUploadModel);
    }

    public PostUploadModel getPostUploadModelForPostModel(PostModel postModel) {
        return UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId());
    }

    public @NonNull Set<MediaModel> getUploadingMediaForPost(PostModel post) {
        return getMediaForPostWithState(post, MediaUploadModel.UPLOADING);
    }

    public @NonNull Set<MediaModel> getCompletedMediaForPost(PostModel post) {
        return getMediaForPostWithState(post, MediaUploadModel.COMPLETED);
    }

    public @NonNull Set<MediaModel> getFailedMediaForPost(PostModel post) {
        return getMediaForPostWithState(post, MediaUploadModel.FAILED);
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
            mediaUploadModel = new MediaUploadModel(payload.media.getId());
        }

        if (payload.isError() || payload.canceled) {
            mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
            if (payload.isError()) {
                mediaUploadModel.setMediaError(payload.error);
            }
            UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
            cancelPostWithAssociatedMedia(payload.media);
            return;
        }

        if (payload.completed) {
            mediaUploadModel.setUploadState(MediaUploadModel.COMPLETED);
            mediaUploadModel.setProgress(1F);
        } else {
            if (mediaUploadModel.getProgress() < payload.progress) {
                mediaUploadModel.setProgress(payload.progress);
            }
        }
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
    }

    private void handleCancelMedia(@NonNull CancelMediaPayload payload) {
        if (payload.media == null || payload.delete) {
            // If the cancel action has the delete flag, the corresponding MediaModel will be deleted anyway - ignore
            return;
        }
        MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(payload.media.getId());
        if (mediaUploadModel == null) {
            mediaUploadModel = new MediaUploadModel(payload.media.getId());
        }

        mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        cancelPostWithAssociatedMedia(payload.media);
    }

    private void handlePostUploaded(@NonNull RemotePostPayload payload) {
        if (payload.post == null) {
            return;
        }

        PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(payload.post.getId());

        if (payload.isError()) {
            if (postUploadModel == null) {
                postUploadModel = new PostUploadModel(payload.post.getId());
            }
            postUploadModel.setUploadState(PostUploadModel.FAILED);
            postUploadModel.setPostError(payload.error);
            UploadSqlUtils.insertOrUpdatePost(postUploadModel);
            return;
        }

        if (postUploadModel != null) {
            // Delete all MediaUploadModels associated with this post since we're finished with it
            UploadSqlUtils.deleteMediaUploadModelsWithLocalIds(postUploadModel.getAssociatedMediaIdSet());

            // Delete the PostUploadModel itself
            UploadSqlUtils.deletePostUploadModelWithLocalId(payload.post.getId());
        }
    }

    private void handleClearMediaAction(ClearMediaPayload payload) {
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

        emitChange(new OnUploadChanged(UploadAction.CLEAR_MEDIA));
    }

    private @NonNull Set<MediaModel> getMediaForPostWithState(PostModel post, @MediaUploadModel.UploadState int state) {
        PostUploadModel postUploadModel = getPostUploadModelForPostModel(post);
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

    private void cancelPostWithAssociatedMedia(@NonNull MediaModel mediaModel) {
        if (mediaModel.getLocalPostId() > 0) {
            PostUploadModel postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(mediaModel.getLocalPostId());
            if (postUploadModel != null) {
                postUploadModel.setUploadState(PostUploadModel.CANCELLED);
                UploadSqlUtils.insertOrUpdatePost(postUploadModel);
            }
        }
    }
}
