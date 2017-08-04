package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UploadStore extends Store {
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
        if (actionType instanceof PostAction) {
            onPostAction((PostAction) actionType, action.getPayload());
        }
        if (actionType instanceof MediaAction) {
            onMediaAction((MediaAction) actionType, action.getPayload());
        }
    }

    private void onPostAction(PostAction actionType, Object payload) {
        switch (actionType) {
            case PUSHED_POST:
                // TODO
                break;
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
            // TODO Find waiting posts and mark them as cancelled
            mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
            if (payload.isError()) {
                mediaUploadModel.setMediaError(payload.error);
            }
            UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
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

        // TODO Find waiting posts and mark them as cancelled
        mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
    }
}
