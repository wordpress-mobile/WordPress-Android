package org.wordpress.android.localcontentmigration

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.BloggingRemindersData
import javax.inject.Inject

typealias RemoteSiteId = Long

class BloggingRemindersProviderHelper @Inject constructor(
    private val bloggingRemindersStore: BloggingRemindersStore,
    private val siteStore: SiteStore,
): LocalDataProviderHelper {
    override fun getData(localEntityId: Int?) = BloggingRemindersData(
            reminders = runBlocking {
                siteStore.sites.mapNotNull { site ->
                    bloggingRemindersStore.bloggingRemindersModel(site.id).first().let {
                        shouldInclude(it)
                    }
                }
            }
    )
    private fun shouldInclude(reminder: BloggingRemindersModel) =
             if (reminder.enabledDays.isNotEmpty()) reminder else null
}
