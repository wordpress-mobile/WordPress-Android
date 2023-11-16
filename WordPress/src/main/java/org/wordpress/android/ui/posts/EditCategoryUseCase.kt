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
        existingCategory: TermModel,
        categoryId: Long,
        categoryName: String,
        parentCategoryId: Long,
        siteModel: SiteModel
    ) {
        val editedCategory = TermModel(
            existingCategory.id,
            existingCategory.localSiteId,
            categoryId,
            TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY,
            categoryName,
            existingCategory.slug,
            existingCategory.description,
            parentCategoryId,
            existingCategory.postCount
        )
        val payload = RemoteTermPayload(editedCategory, siteModel)
        dispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(payload))
    }
}
