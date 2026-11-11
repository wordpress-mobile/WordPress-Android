package org.wordpress.android.ui.posts

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import org.wordpress.gutenberg.model.EditorConfiguration
import org.wordpress.gutenberg.model.EditorDependencies
import org.wordpress.gutenberg.services.EditorService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditorServiceProviderImpl @Inject constructor() :
    EditorServiceProvider {
    override suspend fun prepare(
        context: Context,
        configuration: EditorConfiguration,
        coroutineScope: CoroutineScope
    ): EditorDependencies {
        val service = EditorService.create(
            context = context,
            configuration = configuration,
            coroutineScope = coroutineScope
        )
        return service.prepare(null)
    }
}
