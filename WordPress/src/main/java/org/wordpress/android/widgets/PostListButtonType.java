package org.wordpress.android.widgets;

// PostListButton.java types - from attrs.xml
public enum PostListButtonType {
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
    BUTTON_RETRY(12),
    BUTTON_RESTORE(13);

    private int mAttrsValue;

    PostListButtonType(int attrsValue) {
        this.mAttrsValue = attrsValue;
    }

    public int getValue() {
        return mAttrsValue;
    }

    public static PostListButtonType fromInt(int value) {
        for (PostListButtonType item : PostListButtonType.values()) {
            if (item.mAttrsValue == value) {
                return item;
            }
        }
        return null;
    }
}
