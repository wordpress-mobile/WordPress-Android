package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.UploadStockMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.UploadedStockMediaPayload;

@ActionEnum
public enum MediaAction implements IAction {
    // Remote actions
    @Action(payloadType = MediaPayload.class)
    PUSH_MEDIA,
    @Action(payloadType = UploadMediaPayload.class)
    UPLOAD_MEDIA,
    @Action(payloadType = FetchMediaListPayload.class)
    FETCH_MEDIA_LIST,
    @Action(payloadType = MediaPayload.class)
    FETCH_MEDIA,
    @Action(payloadType = MediaPayload.class)
    DELETE_MEDIA,
    @Action(payloadType = CancelMediaPayload.class)
    CANCEL_MEDIA_UPLOAD,
    @Action(payloadType = UploadStockMediaPayload.class)
    UPLOAD_STOCK_MEDIA,

    // Remote responses
    @Action(payloadType = MediaPayload.class)
    PUSHED_MEDIA,
    @Action(payloadType = ProgressPayload.class)
    UPLOADED_MEDIA,
    @Action(payloadType = FetchMediaListResponsePayload.class)
    FETCHED_MEDIA_LIST,
    @Action(payloadType = MediaPayload.class)
    FETCHED_MEDIA,
    @Action(payloadType = MediaPayload.class)
    DELETED_MEDIA,
    @Action(payloadType = ProgressPayload.class)
    CANCELED_MEDIA_UPLOAD,
    @Action(payloadType = UploadedStockMediaPayload.class)
    UPLOADED_STOCK_MEDIA,

    // Local actions
    @Action(payloadType = MediaModel.class)
    UPDATE_MEDIA,
    @Action(payloadType = MediaModel.class)
    REMOVE_MEDIA,
    @Action
    REMOVE_ALL_MEDIA
}
