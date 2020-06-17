package org.wordpress.android.ui.posts

import android.text.TextUtils
import dagger.Reusable
import javax.inject.Inject

@Reusable
class UpdatePostTagsUseCase @Inject constructor() {
    fun updateTags(selectedTags: String?, editPostRepository: EditPostRepository) {
        editPostRepository.updateAsync({ postModel ->
            if (selectedTags != null && !TextUtils.isEmpty(selectedTags)) {
                val tags: String = selectedTags.replace("\n", " ")
                postModel.setTagNameList(TextUtils.split(tags, ",").toList())
            } else {
                postModel.setTagNameList(ArrayList())
            }
            true
        })
    }
}
