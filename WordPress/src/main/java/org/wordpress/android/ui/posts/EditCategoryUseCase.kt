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
class EditCategoryUseCase @Inject constructor(
    private val dispatcher: Dispatcher
) {
    fun editCategory(
        categoryId: Long,
        existingCategorySlug: String,
        categoryName: String,
        parentCategoryId: Long,
        siteModel: SiteModel
    ) {
        val existingCategory = TermModel()
        existingCategory.remoteTermId = categoryId
        existingCategory.taxonomy = TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
        existingCategory.name = categoryName
        existingCategory.slug = existingCategorySlug
        existingCategory.parentRemoteId = parentCategoryId
        val payload = RemoteTermPayload(existingCategory, siteModel)
        dispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(payload))
    }
}
