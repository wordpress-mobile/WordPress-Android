package org.wordpress.android.ui.reader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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

/**
 * Created by nbradbury on 8/7/13.
 */
public class ReaderTagActivity extends FragmentActivity implements ReaderTagAdapter.TopicActionListener {
    private ViewGroup mLayoutAddTag;
    private EditText mEditAddTag;
    private ListView mListView;
    private TextView mTxtTitle;

    private boolean mTagsChanged;
    private String mLastAddedTag;
    private boolean mIsShowingFollowedTags = true;
    private boolean mAlreadyUpdatedTagList;

    protected static final String ARG_TAG_NAME       = "tag_name";

    protected static final String KEY_TAGS_CHANGED   = "tags_changed";
    protected static final String KEY_LAST_ADDED_TAG = "last_added_tag";
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

        // toggle between topic views when title is clicked
        mTxtTitle = (TextView) findViewById(R.id.text_title);
        mTxtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTopics();
            }
        });

        mLayoutAddTag = (ViewGroup) findViewById(R.id.layout_add_topic);
        mEditAddTag = (EditText) findViewById(R.id.edit_add);
        mEditAddTag.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    addCurrentTopic();
                return false;
            }
        });

        final ImageButton btnAddTopic = (ImageButton) findViewById(R.id.btn_add);
        btnAddTopic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCurrentTopic();
            }
        });

        updateTitle();

        getListView().setAdapter(getTagAdapter());
        getTagAdapter().setTopicType(mIsShowingFollowedTags ? ReaderTag.ReaderTagType.SUBSCRIBED : ReaderTag.ReaderTagType.RECOMMENDED);

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
     * update title & text based on whether we're showing followed or popular topics
     */
    private void updateTitle() {
        // update dialog title
        mTxtTitle.setText(mIsShowingFollowedTags ? R.string.reader_title_followed_tags : R.string.reader_title_popular_tags);

        // show "navigate next" image to the right of the title if followed topics is showing, show "navigate
        // previous" to the left of the title if popular topics is showing
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
     * toggle between followed & popular topics
     */
    private void toggleTopics() {
        mIsShowingFollowedTags = !mIsShowingFollowedTags;

        MessageBarUtils.hideMessageBar(this, null, true);
        updateTitle();
        mLayoutAddTag.setVisibility(mIsShowingFollowedTags ? View.VISIBLE : View.GONE);

        mAdapter.setTopicType(mIsShowingFollowedTags ? ReaderTag.ReaderTagType.SUBSCRIBED : ReaderTag.ReaderTagType.RECOMMENDED);
        getListView().setAdapter(mAdapter);
    }

    /*
     * add the topic in the EditText to the user's topic list
     */
    private void addCurrentTopic() {
        String topicName = EditTextUtils.getText(mEditAddTag);
        if (TextUtils.isEmpty(topicName))
            return;

        if (!NetworkUtils.checkConnection(this))
            return;

        if (ReaderTagTable.tagExists(topicName)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_exists, ToastUtils.Duration.LONG);
            return;
        }

        if (!ReaderTag.isValidTagName(topicName)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_tag_invalid, ToastUtils.Duration.LONG);
            return;
        }

        mEditAddTag.setText(null);
        EditTextUtils.hideSoftInput(mEditAddTag);

        onTopicAction(TagAction.ADD, topicName);
    }

    /*
     * triggered by adapter when user chooses to add/remove a topic, or from this activity when
     * user chooses to add a topic
     */
    @Override
    public void onTopicAction(final TagAction action, final String topicName) {
        if (TextUtils.isEmpty(topicName))
            return;

        if (!NetworkUtils.checkConnection(this))
            return;

        final String messageBarText;
        final MessageBarUtils.MessageBarType messageBarType;
        switch (action) {
            case ADD:
                messageBarText = getString(R.string.reader_label_added_tag, topicName);
                messageBarType = MessageBarUtils.MessageBarType.INFO;
                break;
            case DELETE:
                messageBarText = getString(R.string.reader_label_removed_tag, topicName);
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
                if (!succeeded) {
                    getTagAdapter().refreshTopics();
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
                if (ReaderTagActions.performTagAction(TagAction.ADD, topicName, actionListener)) {
                    ReaderActions.DataLoadedListener dataListener = new ReaderActions.DataLoadedListener() {
                        @Override
                        public void onDataLoaded(boolean isEmpty) {
                            // make sure the added topic is scrolled into view if showing followed topics
                            if (mIsShowingFollowedTags) {
                                int index = getTagAdapter().indexOfTopicName(topicName);
                                if (index > -1)
                                    getListView().smoothScrollToPosition(index);
                            }
                        }
                    };
                    getTagAdapter().refreshTopics(dataListener);
                    mTagsChanged = true;
                    mLastAddedTag = topicName;
                }
                break;

            case DELETE:
                if (ReaderTagActions.performTagAction(TagAction.DELETE, topicName, actionListener)) {
                    getTagAdapter().refreshTopics();
                    if (mLastAddedTag !=null && mLastAddedTag.equals(topicName))
                        mLastAddedTag = null;
                    mTagsChanged = true;
                }
                break;
        }
    }

    /*
     * request latest list of tags from the server
     */
    protected void updateTagList() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (result==ReaderActions.UpdateResult.CHANGED) {
                    mTagsChanged = true;
                    getTagAdapter().refreshTopics();
                }
            }
        };
        ReaderTagActions.updateTags(listener);
    }
}
