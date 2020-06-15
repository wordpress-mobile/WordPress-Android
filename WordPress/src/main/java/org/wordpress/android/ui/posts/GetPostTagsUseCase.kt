package org.wordpress.android.ui.posts

import android.text.TextUtils
import dagger.Reusable
import org.apache.commons.text.StringEscapeUtils
import javax.inject.Inject

@Reusable
class GetPostTagsUseCase @Inject constructor() {
    fun getTags(editPostRepository: EditPostRepository): String? {
        val tags = editPostRepository.getPost()?.tagNameList
        return tags?.let {
            if (it.isNotEmpty()) {
                formatTags(it)
            } else {
                null
            }
        }
    }

    private fun formatTags(tags: List<String>): String {
        val formattedTags = TextUtils.join(",", tags)
        return StringEscapeUtils.unescapeHtml4(formattedTags)
    }
}
