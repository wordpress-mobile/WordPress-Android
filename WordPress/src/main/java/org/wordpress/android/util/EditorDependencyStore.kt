package org.wordpress.android.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ThemeCoroutineStore
import org.wordpress.gutenberg.model.EditorConfiguration
import org.wordpress.gutenberg.model.EditorDependencies
import org.wordpress.gutenberg.model.EditorDependenciesSerializer
import org.wordpress.gutenberg.services.EditorService

class EditorDependencyStore(val context: Context, val coroutineScope: CoroutineScope) {
    var dependencies: EditorDependencies? = null

    suspend fun fetch(configuration: EditorConfiguration): EditorDependencies {
        val service = EditorService.create(
            context = context,
            configuration = configuration,
            coroutineScope = coroutineScope
        )

        val dependencies = service.prepare(null)
        return dependencies
    }

    fun read(configuration: EditorConfiguration): EditorDependencies {
        return EditorDependencies.empty
    }
}
