package org.wordpress.android.ui.posts.prepublishing.home.usecases

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostSettingsUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import java.lang.Exception
import javax.inject.Inject

class GetPublishDateLabelUseCase @Inject constructor(private val postSettingsUtils: PostSettingsUtils) {
    fun getLabel(editPostRepository: EditPostRepository): UiString {
        if (editPostRepository.status == PRIVATE) {
            return UiStringRes(R.string.immediately)
        } else {
            editPostRepository.getPost()?.let { postImmutableModel ->
                val label = postSettingsUtils.getPublishDateLabel(postImmutableModel)
                return if (label.isNotEmpty()) {
                    UiStringText(label)
                } else {
                    UiStringRes(R.string.immediately)
                }
            } ?: run {
                throw Exception("Post can't be null")
            }
        }
    }
}
