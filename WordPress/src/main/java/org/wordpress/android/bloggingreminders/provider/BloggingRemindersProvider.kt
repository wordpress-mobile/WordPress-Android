package org.wordpress.android.bloggingreminders.provider

import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.provider.query.QueryContentProvider
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.util.config.JetpackProviderSyncFeatureConfig
import org.wordpress.android.util.publicdata.ClientVerification
import org.wordpress.android.util.signature.SignatureNotFoundException
import javax.inject.Inject

typealias RemoteSiteId = Long

class BloggingRemindersProvider : QueryContentProvider() {
    @Inject lateinit var bloggingRemindersStore: BloggingRemindersStore
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var queryResult: QueryResult
    @Inject lateinit var clientVerification: ClientVerification
    @Inject lateinit var jetpackProviderSyncFeatureConfig: JetpackProviderSyncFeatureConfig

    override fun onCreate(): Boolean {
        return true
    }

    @Suppress("SwallowedException")
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        inject()
        if (!jetpackProviderSyncFeatureConfig.isEnabled()) {
            return null
        }
        return context?.let {
            try {
                if (clientVerification.canTrust(callingPackage)) {
                    runBlocking {
                        val allSiteModels = siteStore.sites
                        val filteredBloggingReminders = allSiteModels.map { siteModel ->
                            bloggingRemindersStore.bloggingRemindersModel(siteModel.id).first()
                        }.filter { bloggingRemindersModel ->
                            bloggingRemindersModel.enabledDays.isNotEmpty()
                        }
                        val filteredSiteIds = filteredBloggingReminders.map { bloggingReminder ->
                            siteStore.getSiteIdForLocalId(bloggingReminder.siteId)
                        }
                        val result: Map<RemoteSiteId?, BloggingRemindersModel?> = filteredSiteIds.zip(
                                filteredBloggingReminders
                        ).toMap()
                        queryResult.createCursor(result)
                    }
                } else null
            } catch (signatureNotFoundException: SignatureNotFoundException) {
                null
            }
        }
    }

    private fun inject() {
        if (!this::bloggingRemindersStore.isInitialized) {
            (context?.applicationContext as WordPress).component().inject(this)
        }
    }
}
