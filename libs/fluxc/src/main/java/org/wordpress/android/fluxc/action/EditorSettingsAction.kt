package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.EditorSettingsStore.FetchEditorSettingsPayload

@ActionEnum
enum class EditorSettingsAction : IAction {
    @Action(payloadType = FetchEditorSettingsPayload::class)
    FETCH_EDITOR_SETTINGS
}
