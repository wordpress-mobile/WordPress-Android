package org.wordpress.android.ui.posts

import dagger.Reusable
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.models.CategoryNode
import java.util.ArrayList
import javax.inject.Inject

@Reusable
class GetCategoriesUseCase @Inject constructor(
    private val taxonomyStore: TaxonomyStore,
    private val dispatcher: Dispatcher
) {
    fun getPostCategoriesString(
        editPostRepository: EditPostRepository,
        siteModel: SiteModel
    ): String? {
        val post = editPostRepository.getPost() ?: return null
        val categories: List<TermModel> = taxonomyStore.getCategoriesForPost(
                post,
                siteModel
        )
        return formatCategories(categories)
    }

    fun getPostCategories(editPostRepository: EditPostRepository, siteModel: SiteModel) =
            editPostRepository.getPost()?.categoryIdList ?: listOf()

    fun getSiteCategories(siteModel: SiteModel): ArrayList<CategoryNode> {
        val rootCategory = CategoryNode.createCategoryTreeFromList(
                getCategoriesForSite(siteModel)
        )
        return CategoryNode.getSortedListOfCategoriesFromRoot(rootCategory) ?: arrayListOf()
    }

    fun fetchSiteCategories(siteModel: SiteModel) {
            dispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(siteModel))
    }

    private fun formatCategories(categoryList: List<TermModel>): String? {
        if (categoryList.isEmpty()) return null

        val formattedCategories = categoryList.joinToString { it -> it.name }
        return StringEscapeUtils.unescapeHtml4(formattedCategories)
    }

    private fun getCategoriesForSite(siteModel: SiteModel): List<TermModel> {
        return taxonomyStore.getCategoriesForSite(siteModel)
    }
}
