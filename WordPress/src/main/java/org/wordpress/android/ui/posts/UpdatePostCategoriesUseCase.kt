package org.wordpress.android.ui.posts

import dagger.Reusable
import javax.inject.Inject

@Reusable
class UpdatePostCategoriesUseCase @Inject constructor() {
    fun updateCategories(categoryList: List<Long>, editPostRepository: EditPostRepository) {
        editPostRepository.updateAsync({ postModel ->
            postModel.setCategoryIdList(categoryList)
            true
        })
    }
}
