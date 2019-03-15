package org.wordpress.android.ui.posts

import org.wordpress.android.util.image.ImageManager

class PostViewHolderConfig(
    val endlistIndicatorHeight: Int,
    val photonWidth: Int,
    val photonHeight: Int,
    val isPhotonCapable: Boolean,
    val showAllButtons: Boolean,
    val imageManager: ImageManager,
    val isAztecEditorEnabled: Boolean,
    val hasCapabilityPublishPosts: Boolean
)
