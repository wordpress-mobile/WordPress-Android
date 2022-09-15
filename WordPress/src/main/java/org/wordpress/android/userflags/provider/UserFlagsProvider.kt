package org.wordpress.android.userflags.provider

import android.database.Cursor
import android.net.Uri
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.provider.query.QueryContentProvider
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.util.publicdata.JetpackPublicData
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.signature.SignatureNotFoundException
import org.wordpress.android.util.signature.SignatureUtils
import javax.inject.Inject

class UserFlagsProvider : QueryContentProvider() {
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var signatureUtils: SignatureUtils
    @Inject lateinit var queryResult: QueryResult
    @Inject lateinit var jetpackPublicData: JetpackPublicData

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        inject()
        return context?.let {
            try {
                val callerPackageId = callingPackage
                val callerExpectedPackageId = jetpackPublicData.currentPackageId()
                val callerSignatureHash = signatureUtils.getSignatureHash(it, callerExpectedPackageId)
                val callerExpectedSignatureHash = jetpackPublicData.currentPublicKeyHash()
                if (callerPackageId == callerExpectedPackageId && callerSignatureHash == callerExpectedSignatureHash) {
                    val userFlagsMap = appPrefsWrapper.getAllPrefs()
                    queryResult.createCursor(userFlagsMap)
                } else null
            } catch (signatureNotFoundException: SignatureNotFoundException) {
                null
            }
        }
    }

    private fun inject() {
        if (!this::appPrefsWrapper.isInitialized) {
            (context?.applicationContext as WordPress).component().inject(this)
        }
    }
}
