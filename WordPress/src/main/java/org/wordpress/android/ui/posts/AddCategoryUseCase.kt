package org.wordpress.android.ui.posts

import dagger.Reusable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload
import javax.inject.Inject

@Reusable
class AddCategoryUseCase @Inject constructor(
    private val dispatcher: Dispatcher
) {
    fun addCategory(categoryName: String, parentCategoryId: Long, siteModel: SiteModel) {
        val newCategory = TermModel()
        newCategory.taxonomy = TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
        newCategory.name = categoryName
        newCategory.parentRemoteId = parentCategoryId
        val payload = RemoteTermPayload(newCategory, siteModel)
        dispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(payload))
    }
}
