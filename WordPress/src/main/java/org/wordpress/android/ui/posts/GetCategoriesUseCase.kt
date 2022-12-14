package org.wordpress.android.ui.posts

import dagger.Reusable
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.models.wrappers.CategoryNodeWrapper
import org.wordpress.android.util.AppLog.T.PREPUBLISHING_NUDGES
import java.util.ArrayList
import javax.inject.Inject

@Reusable
class GetCategoriesUseCase @Inject constructor(
    private val taxonomyStore: TaxonomyStore,
    private val dispatcher: Dispatcher,
    private val appLogWrapper: AppLogWrapper,
    private val categoryNodeWrapper: CategoryNodeWrapper
) {
    fun getPostCategoriesString(
        editPostRepository: EditPostRepository,
        siteModel: SiteModel
    ): String {
        val post = editPostRepository.getPost()
                if (post == null) {
                    appLogWrapper.d(PREPUBLISHING_NUDGES, "Post is null in EditPostRepository")
                    return ""
                }
        val categories: List<TermModel> = taxonomyStore.getCategoriesForPost(
                post,
                siteModel
        )
        return formatCategories(categories)
    }

    fun getPostCategories(editPostRepository: EditPostRepository) =
            editPostRepository.getPost()?.categoryIdList ?: listOf()

    fun getSiteCategories(siteModel: SiteModel): ArrayList<CategoryNode> {
        val rootCategory = categoryNodeWrapper.createCategoryTreeFromList(
                getCategoriesForSite(siteModel)
        )
        return categoryNodeWrapper.getSortedListOfCategoriesFromRoot(rootCategory)
    }

    fun fetchSiteCategories(siteModel: SiteModel) {
        dispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(siteModel))
    }

    private fun formatCategories(categoryList: List<TermModel>): String {
        if (categoryList.isEmpty()) return ""

        val formattedCategories = categoryList.joinToString { it.name }
        return StringEscapeUtils.unescapeHtml4(formattedCategories)
    }

    fun getCategoriesForSite(siteModel: SiteModel): List<TermModel> {
        return taxonomyStore.getCategoriesForSite(siteModel)
    }
}
