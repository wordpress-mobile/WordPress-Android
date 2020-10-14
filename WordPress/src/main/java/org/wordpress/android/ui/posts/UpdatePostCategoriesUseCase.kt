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
//        editPostRepository.updateAsync(
//                { postModel: PostModel? ->
//                    postModel?.setCategoryIdList(categoryList)
//                    true
//                },
//                { _: PostImmutableModel?, result: UpdatePostResult? ->
//                    result?.let {
//                        Log.i(javaClass.simpleName, "***=> updatePostResult $it")
//                    }
//                    null
//                })
//    }
}
