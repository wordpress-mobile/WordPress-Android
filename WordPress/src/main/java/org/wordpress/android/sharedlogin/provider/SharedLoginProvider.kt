package org.wordpress.android.sharedlogin.provider

import android.database.Cursor
import android.net.Uri
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.provider.query.QueryContentProvider
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.util.publicdata.ClientVerification
import org.wordpress.android.util.signature.SignatureNotFoundException
import javax.inject.Inject

class SharedLoginProvider : QueryContentProvider() {
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var queryResult: QueryResult
    @Inject lateinit var clientVerification: ClientVerification

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
                    queryResult.createCursor(accountStore.accessToken)
                } else null
            } catch (signatureNotFoundException: SignatureNotFoundException) {
                null
            }
        }
    }

    private fun inject() {
        if (!this::accountStore.isInitialized) {
            (context?.applicationContext as WordPress).component().inject(this)
        }
    }
}
