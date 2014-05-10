package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions.TagAction;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.MessageBarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

public class ReaderTagActivity extends Activity implements ReaderTagAdapter.TagActionListener {
    private ViewGroup mLayoutAddTag;
    private EditText mEditAddTag;
    private ListView mListView;
    private TextView mTxtTitle;

    private boolean mTagsChanged;
    private String mLastAddedTag;
    private boolean mIsShowingFollowedTags = true;
    private boolean mAlreadyUpdatedTagList;

    static final String ARG_TAG_NAME       = "tag_name";

    static final String KEY_TAGS_CHANGED   = "tags_changed";
    static final String KEY_LAST_ADDED_TAG = "last_added_tag";
    private static final String KEY_TAG_LIST_UPDATED = "tags_updated";
    private static final String KEY_SHOWING_FOLLOWED = "showing_followed";

    private ListView getListView() {
        if (mListView==null) {
            mListView = (ListView) findViewById(android.R.id.list);
            mListView.setEmptyView(findViewById(R.id.text_empty));
        }
        return mListView;
    }

    private ReaderTagAdapter mAdapter;
    private ReaderTagAdapter getTagAdapter() {
        if (mAdapter==null)
            mAdapter = new ReaderTagAdapter(this, this);
        return mAdapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_tags);

        if (savedInstanceState!=null) {
            mTagsChanged = savedInstanceState.getBoolean(KEY_TAGS_CHANGED);
            mLastAddedTag = savedInstanceState.getString(KEY_LAST_ADDED_TAG);
            mAlreadyUpdatedTagList = savedInstanceState.getBoolean(KEY_TAG_LIST_UPDATED);
            mIsShowingFollowedTags = savedInstanceState.getBoolean(KEY_SHOWING_FOLLOWED, true);
        }

        // toggle between tag views when title is clicked
        mTxtTitle = (TextView) findViewById(R.id.text_title);
        mTxtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTags();
            }
        });

        mLayoutAddTag = (ViewGroup) findViewById(R.id.layout_add_topic);
        mEditAddTag = (EditText) findViewById(R.id.edit_add);
        mEditAddTag.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    addCurrentTag();
                return false;
            }
        });

        final ImageButton btnAddTag = (ImageButton) findViewById(R.id.btn_add);
        btnAddTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCurrentTag();
            }
        });

        updateTitle();

        getListView().setAdapter(getTagAdapter());
        getTagAdapter().setTagType(mIsShowingFollowedTags ? ReaderTag.ReaderTagType.SUBSCRIBED : ReaderTag.ReaderTagType.RECOMMENDED);

        // if a tag was passed in the intent, display it in the edit field
        final String tagName = getIntent().getStringExtra(ARG_TAG_NAME);
        if (!TextUtils.isEmpty(tagName)) {
            getIntent().removeExtra(ARG_TAG_NAME);
            mEditAddTag.setText(tagName);
        }

        // update list of tags from the server
        if (!mAlreadyUpdatedTagList) {
            updateTagList();
            mAlreadyUpdatedTagList = true;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    /*
     * update title & text based on whether we're showing followed or popular tags
     */
    private void updateTitle() {
        // update dialog title
        mTxtTitle.setText(mIsShowingFollowedTags ? R.string.reader_title_followed_tags : R.string.reader_title_popular_tags);

        // show "navigate next" image to the right of the title if followed tags is showing, show "navigate
        // previous" to the left of the title if popular tags is showing
        if (mIsShowingFollowedTags) {
            mTxtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_navigate_next, 0);
        } else {
            mTxtTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigate_previous, 0, 0, 0);
        }

        // update empty list text
        final TextView txtEmpty = (TextView) findViewById(R.id.text_empty);
        txtEmpty.setText(mIsShowingFollowedTags ? R.string.reader_empty_followed_tags : R.string.reader_empty_popular_tags);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_TAGS_CHANGED, mTagsChanged);
        outState.putBoolean(KEY_TAG_LIST_UPDATED, mAlreadyUpdatedTagList);
        outState.putBoolean(KEY_SHOWING_FOLLOWED, mIsShowingFollowedTags);
        if (mLastAddedTag !=null)
            outState.putString(KEY_LAST_ADDED_TAG, mLastAddedTag);
    }

    @Override
    public void onBackPressed() {
        if (mTagsChanged) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(KEY_TAGS_CHANGED, true);
            if (mLastAddedTag !=null && ReaderTagTable.tagExists(mLastAddedTag))
                bundle.putString(KEY_LAST_ADDED_TAG, mLastAddedTag);
            Intent intent = new Intent();
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
        }

        super.onBackPressed();
    }

    /*
     * toggle between followed & popular tags
     */
    private void toggleTags() {
        mIsShowingFollowedTags = !mIsShowingFollowedTags;

        MessageBarUtils.hideMessageBar(this, null, true);
        updateTitle();
        mLayoutAddTag.setVisibility(mIsShowingFollowedTags ? View.VISIBLE : View.GONE);

        mAdapter.setTagType(mIsShowingFollowedTags ? ReaderTag.ReaderTagType.SUBSCRIBED : ReaderTag.ReaderTagType.RECOMMENDED);
        getListView().setAdapter(mAdapter);
    }

    /*
     * add the tag in the EditText to the user's followed tags
     */
    private void addCurrentTag() {
        String tagName = EditTextUtils.getText(mEditAddTag);
        if (TextUtils.isEmpty(tagName))
            return;

        if (!NetworkUtils.checkConnection(this))
            return;

        if (ReaderTagTable.tagExists(tagName)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_exists, ToastUtils.Duration.LONG);
            return;
        }

        if (!ReaderTag.isValidTagName(tagName)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_invalid, ToastUtils.Duration.LONG);
            return;
        }

        mEditAddTag.setText(null);
        EditTextUtils.hideSoftInput(mEditAddTag);

        onTagAction(TagAction.ADD, tagName);
    }

    /*
     * triggered by adapter when user chooses to add/remove a tag, or from this activity when
     * user chooses to add a tag
     */
    @Override
    public void onTagAction(final TagAction action, final String tagName) {
        if (TextUtils.isEmpty(tagName))
            return;

        if (!NetworkUtils.checkConnection(this))
            return;

        final String messageBarText;
        final MessageBarUtils.MessageBarType messageBarType;
        switch (action) {
            case ADD:
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_FOLLOWED_READER_TAG);
                messageBarText = getString(R.string.reader_label_added_tag, tagName);
                messageBarType = MessageBarUtils.MessageBarType.INFO;
                break;
            case DELETE:
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_UNFOLLOWED_READER_TAG);
                messageBarText = getString(R.string.reader_label_removed_tag, tagName);
                messageBarType = MessageBarUtils.MessageBarType.ALERT;
                break;
            default :
                return;
        }
        MessageBarUtils.showMessageBar(this, messageBarText, messageBarType, null);

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                // handle failure when adding/removing tags below
                if (!succeeded && !isFinishing()) {
                    getTagAdapter().refreshTags();
                    switch (action) {
                        case ADD:
                            ToastUtils.showToast(ReaderTagActivity.this, R.string.reader_toast_err_add_tag);
                            mLastAddedTag = null;
                            break;
                        case DELETE:
                            ToastUtils.showToast(ReaderTagActivity.this, R.string.reader_toast_err_remove_tag);
                            break;
                    }
                }
            }
        };

        switch (action) {
            case ADD:
                if (ReaderTagActions.performTagAction(TagAction.ADD, tagName, actionListener)) {
                    ReaderActions.DataLoadedListener dataListener = new ReaderActions.DataLoadedListener() {
                        @Override
                        public void onDataLoaded(boolean isEmpty) {
                            // make sure the added tag is scrolled into view if showing followed tags
                            if (mIsShowingFollowedTags) {
                                int index = getTagAdapter().indexOfTagName(tagName);
                                if (index > -1)
                                    getListView().smoothScrollToPosition(index);
                            }
                        }
                    };
                    getTagAdapter().refreshTags(dataListener);
                    mTagsChanged = true;
                    mLastAddedTag = tagName;
                }
                break;

            case DELETE:
                if (ReaderTagActions.performTagAction(TagAction.DELETE, tagName, actionListener)) {
                    getTagAdapter().refreshTags();
                    if (mLastAddedTag !=null && mLastAddedTag.equals(tagName))
                        mLastAddedTag = null;
                    mTagsChanged = true;
                }
                break;
        }
    }

    /*
     * request latest list of tags from the server
     */
    void updateTagList() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (result==ReaderActions.UpdateResult.CHANGED) {
                    mTagsChanged = true;
                    getTagAdapter().refreshTags();
                }
            }
        };
        ReaderTagActions.updateTags(listener);
    }
}
