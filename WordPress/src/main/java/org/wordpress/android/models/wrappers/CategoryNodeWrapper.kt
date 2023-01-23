package org.wordpress.android.models.wrappers

import dagger.Reusable
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.models.CategoryNode
import javax.inject.Inject

@Reusable
class CategoryNodeWrapper @Inject constructor() {
    fun createCategoryTreeFromList(categories: List<TermModel>): CategoryNode =
        CategoryNode.createCategoryTreeFromList(categories)

    fun getSortedListOfCategoriesFromRoot(node: CategoryNode): ArrayList<CategoryNode> =
        CategoryNode.getSortedListOfCategoriesFromRoot(node)
}
