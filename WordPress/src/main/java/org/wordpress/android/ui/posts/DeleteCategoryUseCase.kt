package org.wordpress.android.ui.posts

import dagger.Reusable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload
import javax.inject.Inject

@Reusable
class DeleteCategoryUseCase @Inject constructor(
    private val dispatcher: Dispatcher
) {
    fun deleteCategory(termModel: TermModel, site: SiteModel) {
        val payload = RemoteTermPayload(termModel, site)
        dispatcher.dispatch(TaxonomyActionBuilder.newDeleteTermAction(payload))
    }
}
