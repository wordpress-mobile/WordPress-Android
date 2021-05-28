package org.wordpress.android.fluxc.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.persistence.BloggingRemindersSqlUtils.BloggingReminders
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloggingRemindersRepository
@Inject constructor(private val sqlUtils: BloggingRemindersSqlUtils) {
    private val state = MutableStateFlow(loadData())

    fun bloggingRemindersModel(siteId: Int): Flow<BloggingReminders> {
        return state.map { it[siteId] ?: BloggingReminders(localSiteId = siteId) }
                .distinctUntilChanged()
    }

    fun updateBloggingReminders(model: BloggingReminders) {
        sqlUtils.replaceBloggingReminder(model)
        refresh()
    }

    private fun refresh() {
        val updatedMap = loadData()
        if (state.value != updatedMap) {
            state.value = updatedMap
        }
    }

    private fun loadData() = sqlUtils.getBloggingReminders().associateBy { it.localSiteId }
}
