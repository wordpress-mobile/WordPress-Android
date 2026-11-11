package org.wordpress.android.ui.posts

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import org.wordpress.gutenberg.model.EditorConfiguration
import org.wordpress.gutenberg.model.EditorDependencies

/**
 * Abstracts the creation and preparation of the GutenbergKit
 * [EditorService] so callers can be tested without the real
 * service.
 */
fun interface EditorServiceProvider {
    suspend fun prepare(
        context: Context,
        configuration: EditorConfiguration,
        coroutineScope: CoroutineScope
    ): EditorDependencies
}
