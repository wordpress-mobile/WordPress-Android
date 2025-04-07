package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.EditorSettingsStore.FetchEditorSettingsPayload

object EditorSettingsActionBuilder {
    fun newFetchEditorSettingsAction(payload: FetchEditorSettingsPayload): Action<FetchEditorSettingsPayload> {
        return Action(EditorSettingsAction.FETCH_EDITOR_SETTINGS, payload)
    }
}
