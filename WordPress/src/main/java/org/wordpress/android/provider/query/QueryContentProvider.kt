package org.wordpress.android.provider.query

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri

abstract class QueryContentProvider : ContentProvider() {
    override fun getType(uri: Uri): String? =
            throw IllegalAccessException("getType cannot be called on this this ContentProvider")

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
            throw IllegalAccessException("insert cannot be called on this this ContentProvider")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
            throw IllegalAccessException("delete cannot be called on this this ContentProvider")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
            throw IllegalAccessException("update cannot be called on this this ContentProvider")
}
