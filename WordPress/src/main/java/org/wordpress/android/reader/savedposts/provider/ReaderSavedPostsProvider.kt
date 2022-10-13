package org.wordpress.android.reader.savedposts.provider

import android.database.Cursor
import android.net.Uri
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.provider.query.QueryContentProvider
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.util.publicdata.ClientVerification
import org.wordpress.android.util.publicdata.JetpackPublicData
import org.wordpress.android.util.signature.SignatureNotFoundException
import javax.inject.Inject

class ReaderSavedPostsProvider : QueryContentProvider() {
    @Inject lateinit var queryResult: QueryResult
    @Inject lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    @Inject lateinit var jetpackPublicData: JetpackPublicData
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
        return context?.let {
            try {
                if (clientVerification.canTrust(callingPackage)) {
                    val posts = readerPostTableWrapper.getPostsWithTag(
                            readerTag = ReaderTag("", "", "", "", BOOKMARKED),
                            maxRows = 0,
                            excludeTextColumn = false
                    )
                    queryResult.createCursor(posts)
                } else null
            } catch (signatureNotFoundException: SignatureNotFoundException) {
                null
            }
        }
    }

    private fun inject() {
        if (!this::queryResult.isInitialized) {
            (context?.applicationContext as WordPress).component().inject(this)
        }
    }
}
