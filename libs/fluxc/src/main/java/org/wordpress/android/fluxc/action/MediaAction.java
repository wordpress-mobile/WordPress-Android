package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.MediaStore.ChangeMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;

@ActionEnum
public enum MediaAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchMediaPayload.class)  FETCH_ALL_MEDIA,
    @Action(payloadType = FetchMediaPayload.class)  FETCH_MEDIA,
    @Action(payloadType = ChangeMediaPayload.class) PUSH_MEDIA,
    @Action(payloadType = UploadMediaPayload.class) UPLOAD_MEDIA,
    @Action(payloadType = ChangeMediaPayload.class) DELETE_MEDIA,

    // Local actions
    @Action(payloadType = ChangeMediaPayload.class) UPDATE_MEDIA,
    @Action(payloadType = ChangeMediaPayload.class) REMOVE_MEDIA,
}
