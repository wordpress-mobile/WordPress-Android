package org.wordpress.android.ui.posts;

import android.support.v7.app.AppCompatActivity;

public abstract class EditPostBaseActivity extends AppCompatActivity {
    public static final String EXTRA_POST_LOCAL_ID = "postModelLocalId";
    public static final String EXTRA_POST_REMOTE_ID = "postModelRemoteId";
    public static final String EXTRA_IS_PAGE = "isPage";
    public static final String EXTRA_IS_PROMO = "isPromo";
    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_QUICKPRESS_BLOG_ID = "quickPressBlogId";
    public static final String EXTRA_SAVED_AS_LOCAL_DRAFT = "savedAsLocalDraft";
    public static final String EXTRA_HAS_FAILED_MEDIA = "hasFailedMedia";
    public static final String EXTRA_HAS_CHANGES = "hasChanges";
    public static final String EXTRA_IS_DISCARDABLE = "isDiscardable";
    public static final String EXTRA_INSERT_MEDIA = "insertMedia";
    protected static final String STATE_KEY_EDITOR_FRAGMENT = "editorFragment";
    protected static final String STATE_KEY_DROPPED_MEDIA_URIS = "stateKeyDroppedMediaUri";
    protected static final String STATE_KEY_POST_LOCAL_ID = "stateKeyPostModelLocalId";
    protected static final String STATE_KEY_POST_REMOTE_ID = "stateKeyPostModelRemoteId";
    protected static final String STATE_KEY_IS_DIALOG_PROGRESS_SHOWN = "stateKeyIsDialogProgressShown";
    protected static final String STATE_KEY_IS_DISCARDING_CHANGES = "stateKeyIsDiscardingChanges";
    protected static final String STATE_KEY_IS_NEW_POST = "stateKeyIsNewPost";
    protected static final String STATE_KEY_IS_PHOTO_PICKER_VISIBLE = "stateKeyPhotoPickerVisible";
    protected static final String STATE_KEY_HTML_MODE_ON = "stateKeyHtmlModeOn";
    protected static final String STATE_KEY_REVISION = "stateKeyRevision";
    protected static final String TAG_DISCARDING_CHANGES_ERROR_DIALOG = "tag_discarding_changes_error_dialog";
    protected static final String TAG_DISCARDING_CHANGES_NO_NETWORK_DIALOG = "tag_discarding_changes_no_network_dialog";
    protected static final String TAG_PUBLISH_CONFIRMATION_DIALOG = "tag_publish_confirmation_dialog";
    protected static final String TAG_REMOVE_FAILED_UPLOADS_DIALOG = "tag_remove_failed_uploads_dialog";

    protected static final int PAGE_CONTENT = 0;
    protected static final int PAGE_SETTINGS = 1;
    protected static final int PAGE_PREVIEW = 2;
    protected static final int PAGE_HISTORY = 3;

    protected static final String PHOTO_PICKER_TAG = "photo_picker";
    protected static final String ASYNC_PROMO_DIALOG_TAG = "async_promo";

    protected static final String WHAT_IS_NEW_IN_MOBILE_URL =
            "https://make.wordpress.org/mobile/whats-new-in-android-media-uploading/";
    protected static final int CHANGE_SAVE_DELAY = 500;
    public static final int MAX_UNSAVED_POSTS = 50;

    protected enum AddExistingdMediaSource {
        WP_MEDIA_LIBRARY,
        STOCK_PHOTO_LIBRARY
    }

}
