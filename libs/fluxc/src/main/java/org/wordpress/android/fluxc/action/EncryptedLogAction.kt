package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogPayload

@ActionEnum
enum class EncryptedLogAction : IAction {
    @Action(payloadType = UploadEncryptedLogPayload::class)
    UPLOAD_LOG
}
