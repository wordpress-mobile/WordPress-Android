package org.wordpress.android.resolver

import android.content.ContentResolver
import android.database.Cursor
import androidx.core.net.toUri
import javax.inject.Inject

class ContentResolverWrapper @Inject constructor() {
    fun queryUri(contentResolver: ContentResolver, uriValue: String): Cursor? =
        contentResolver.query(uriValue.toUri(), arrayOf(), "", arrayOf(), "")
}
