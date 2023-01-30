package org.wordpress.android.ui.bloggingprompts

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import javax.inject.Inject

class BloggingPromptsSettingsHelper @Inject constructor(
    private val bloggingRemindersStore: BloggingRemindersStore,
) {
    fun getPromptsCardEnabledLiveData(
        siteId: Int
    ): LiveData<Boolean> = bloggingRemindersStore.bloggingRemindersModel(siteId)
        .asLiveData()
        .map { it.isPromptsCardEnabled }

    fun updatePromptsCardEnabledBlocking(siteId: Int, isEnabled: Boolean) = runBlocking {
        updatePromptsCardEnabled(siteId, isEnabled)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun updatePromptsCardEnabled(siteId: Int, isEnabled: Boolean) {
        val current = bloggingRemindersStore.bloggingRemindersModel(siteId).firstOrNull() ?: return
        bloggingRemindersStore.updateBloggingReminders(current.copy(isPromptsCardEnabled = isEnabled))
    }
}
