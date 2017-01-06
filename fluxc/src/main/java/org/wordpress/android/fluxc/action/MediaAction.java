package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.MediaStore.MediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;

@ActionEnum
public enum MediaAction implements IAction {
    // Remote actions
    @Action(payloadType = MediaListPayload.class)
    PUSH_MEDIA,
    @Action(payloadType = UploadMediaPayload.class)
    UPLOAD_MEDIA,
    @Action(payloadType = MediaListPayload.class)
    FETCH_ALL_MEDIA,
    @Action(payloadType = MediaListPayload.class)
    FETCH_MEDIA,
    @Action(payloadType = MediaListPayload.class)
    DELETE_MEDIA,
    @Action(payloadType = MediaListPayload.class)
    CANCEL_MEDIA_UPLOAD,

    // Remote responses
    @Action(payloadType = MediaListPayload.class)
    PUSHED_MEDIA,
    @Action(payloadType = ProgressPayload.class)
    UPLOADED_MEDIA,
    @Action(payloadType = MediaListPayload.class)
    FETCHED_MEDIA,
    @Action(payloadType = MediaListPayload.class)
    DELETED_MEDIA,
    @Action(payloadType = ProgressPayload.class)
    CANCELED_MEDIA_UPLOAD,

    // Local actions
    @Action(payloadType = MediaListPayload.class)
    UPDATE_MEDIA,
    @Action(payloadType = MediaListPayload.class)
    REMOVE_MEDIA,
}
