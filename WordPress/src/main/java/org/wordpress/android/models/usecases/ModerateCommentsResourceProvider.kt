package org.wordpress.android.models.usecases

import dagger.Reusable
import org.wordpress.android.fluxc.store.CommentsStore
import javax.inject.Inject

@Reusable
class ModerateCommentsResourceProvider @Inject constructor(
    val commentsStore: CommentsStore
)
