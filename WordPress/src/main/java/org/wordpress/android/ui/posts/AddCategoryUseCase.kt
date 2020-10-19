package org.wordpress.android.ui.posts

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

@Reusable
class AddCategoryUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun addCategory(categoryName: String, parentCategoryId: Long, siteModel: SiteModel) {
        withContext(ioDispatcher) {
            val newCategory = TermModel()
            newCategory.taxonomy = TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
            newCategory.name = categoryName
            newCategory.parentRemoteId = parentCategoryId
            val payload = RemoteTermPayload(newCategory, siteModel)
            dispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(payload))
        }
    }
}
