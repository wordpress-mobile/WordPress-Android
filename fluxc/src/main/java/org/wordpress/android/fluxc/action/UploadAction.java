package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.UploadStore.ClearMediaPayload;

@ActionEnum
public enum UploadAction implements IAction {
    // Local actions
    @Action(payloadType = ClearMediaPayload.class)
    CLEAR_MEDIA
}
