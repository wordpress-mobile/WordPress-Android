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
    BLAVATAR_CIRCULAR,
    IMAGE,
    PHOTO,
    PHOTO_ROUNDED_CORNERS,
    PLAN,
    PLUGIN,
    THEME,
    UNKNOWN,
    USER,
    VIDEO,
    ICON,
    NO_PLACEHOLDER
}
