package org.wordpress.android.widgets

// PostListButton.java types - from attrs.xml
enum class PostListButtonType constructor(val value: Int) {
    BUTTON_NONE(0),
    BUTTON_EDIT(1),
    BUTTON_VIEW(2),
    BUTTON_PREVIEW(3),
    BUTTON_STATS(4),
    BUTTON_TRASH(5),
    BUTTON_DELETE(6),
    BUTTON_PUBLISH(7),
    BUTTON_SYNC(8),
    BUTTON_MORE(9),
    BUTTON_BACK(10),
    BUTTON_SUBMIT(11),
    BUTTON_RETRY(12);

    companion object {
        fun fromInt(value: Int): PostListButtonType? {
            for (item in PostListButtonType.values()) {
                if (item.value == value) {
                    return item
                }
            }
            return null
        }
    }
}
