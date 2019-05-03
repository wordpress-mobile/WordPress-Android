package org.wordpress.android.util.image

enum class ImageType {
    @Deprecated(
            message = "Use AVATAR_WITH_BACKGROUND or AVATAR_WITHOUT_BACKGROUND instead.",
            replaceWith = ReplaceWith(
                    expression = "AVATAR_WITH_BACKGROUND",
                    imports = ["org.wordpress.android.util.image.ImageType"]))
    AVATAR,
    AVATAR_WITH_BACKGROUND,
    AVATAR_WITHOUT_BACKGROUND,
    BLAVATAR,
    IMAGE,
    PHOTO,
    PLAN,
    PLUGIN,
    THEME,
    UNKNOWN,
    VIDEO,
    ICON,
    NO_PLACEHOLDER
}
