package org.wordpress.android.ui.posts

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

@Reusable
class GetCategoriesUseCase @Inject constructor(
    private val taxonomyStore: TaxonomyStore,
    private val dispatcher: Dispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    // todo: annmarie should these all be in suspendable functions?
    fun getPostCategories(editPostRepository: EditPostRepository, siteModel: SiteModel): String? {
        val post = editPostRepository.getPost() ?: return null
        val categories: List<TermModel> = taxonomyStore.getCategoriesForPost(
                post,
                siteModel
        )
        return formatCategories(categories)
    }

    private fun formatCategories(categoryList: List<TermModel>): String? {
        if (categoryList.isEmpty()) return null

        val formattedCategories = categoryList.joinToString { it -> it.name }
        return StringEscapeUtils.unescapeHtml4(formattedCategories)
    }

    fun getCategoryLevels(siteModel: SiteModel): ArrayList<CategoryNode> {
        val rootCategory = CategoryNode.createCategoryTreeFromList(
                getCategoriesForSite(siteModel)
        )
        return CategoryNode.getSortedListOfCategoriesFromRoot(rootCategory) ?: arrayListOf()
    }

    private fun getCategoriesForSite(siteModel: SiteModel): List<TermModel> {
        return taxonomyStore.getCategoriesForSite(siteModel)
    }

    suspend fun fetchNewCategories(siteModel: SiteModel) {
        withContext(ioDispatcher) {
            dispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(siteModel))
        }
    }
}
