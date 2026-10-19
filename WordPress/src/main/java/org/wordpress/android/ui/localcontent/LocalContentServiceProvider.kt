package org.wordpress.android.ui.localcontent

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import uniffi.wp_mobile.LocalContentService
import uniffi.wp_mobile_local_content.WpLocalContentStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens the local content database on first use and provides the [LocalContentService] over it.
 *
 * The database is a file of its own in the app's files directory, apart from the wordpress-rs
 * cache: the cache can be fetched again, local content can't.
 */
@Singleton
class LocalContentServiceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var service: LocalContentService? = null

    @Synchronized
    fun getService(): LocalContentService = service ?: run {
        val directory = File(context.filesDir, "local_content")
        if (!directory.exists()) directory.mkdirs()
        val store = WpLocalContentStore(File(directory, "local_content.db").absolutePath)
        store.performMigrations()
        LocalContentService(store).also { service = it }
    }
}
