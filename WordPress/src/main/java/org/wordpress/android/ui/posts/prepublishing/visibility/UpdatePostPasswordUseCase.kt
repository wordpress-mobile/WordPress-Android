package org.wordpress.android.ui.posts.prepublishing.visibility

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import javax.inject.Inject

class UpdatePostPasswordUseCase @Inject constructor() {
    fun updatePassword(password: String, editPostRepository: EditPostRepository, onPostPasswordUpdated: () -> Unit) {
        editPostRepository.updateAsync({ postModel: PostModel ->
            postModel.setPassword(password)
            true
        }, { _, result ->
            if (result == UpdatePostResult.Updated) {
                onPostPasswordUpdated.invoke()
            }
        })
    }
}
