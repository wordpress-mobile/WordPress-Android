package org.wordpress.android.ui.reader_native;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTopicTable;
import org.wordpress.android.models.ReaderTopic;
import org.wordpress.android.models.ReaderTopic.ReaderTopicType;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.ui.reader_native.actions.ReaderTopicActions;
import org.wordpress.android.ui.reader_native.adapters.ReaderTopicAdapter;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.ToastUtils;

/**
 * Created by nbradbury on 8/7/13.
 */
public class ReaderTopicActivity extends FragmentActivity implements ReaderTopicAdapter.TopicActionListener {
    private ViewGroup mLayoutAddTopic;
    private EditText mEditAddTopic;
    private ListView mListView;
    private TextView mTxtMessageBar;
    private TextView mTxtTitle;

    private boolean mTopicsChanged;
    private String mLastAddedTopicName;
    private boolean mIsShowingFollowedTopics = true;
    private boolean mAlreadyUpdatedTopicList;

    protected static final String KEY_TOPICS_CHANGED   = "topics_changed";
    protected static final String KEY_LAST_ADDED_TOPIC = "last_added_topic";
    private static final String KEY_TOPIC_LIST_UPDATED = "topics_updated";
    private static final String KEY_SHOWING_FOLLOWED   = "showing_followed";

    private ListView getListView() {
        if (mListView==null) {
            mListView = (ListView) findViewById(android.R.id.list);
            mListView.setEmptyView(findViewById(R.id.text_empty));
        }
        return mListView;
    }

    private ReaderTopicAdapter mAdapter;
    private ReaderTopicAdapter getTopicAdapter() {
        if (mAdapter==null)
            mAdapter = new ReaderTopicAdapter(this, this);
        return mAdapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reader_topics);

        if (savedInstanceState!=null) {
            mTopicsChanged = savedInstanceState.getBoolean(KEY_TOPICS_CHANGED);
            mLastAddedTopicName = savedInstanceState.getString(KEY_LAST_ADDED_TOPIC);
            mAlreadyUpdatedTopicList = savedInstanceState.getBoolean(KEY_TOPIC_LIST_UPDATED);
            mIsShowingFollowedTopics = savedInstanceState.getBoolean(KEY_SHOWING_FOLLOWED, true);
        }

        // toggle between topic views when title is clicked
        mTxtTitle = (TextView) findViewById(R.id.text_title);
        mTxtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTopics();
            }
        });
        mTxtMessageBar = (TextView) findViewById(R.id.text_message_bar);

        mLayoutAddTopic = (ViewGroup) findViewById(R.id.layout_add_topic);
        mEditAddTopic = (EditText) findViewById(R.id.edit_add);
        mEditAddTopic.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId== EditorInfo.IME_ACTION_DONE)
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

        getListView().setAdapter(getTopicAdapter());
        getTopicAdapter().setTopicType(mIsShowingFollowedTopics ? ReaderTopicType.SUBSCRIBED : ReaderTopicType.RECOMMENDED);

        // update list of topics from the server
        if (!mAlreadyUpdatedTopicList) {
            updateTopicList();
            mAlreadyUpdatedTopicList = true;
        }
    }

    /*
     * update title & text based on whether we're showing followed or popular topics
     */
    private void updateTitle() {
        // update dialog title
        mTxtTitle.setText(mIsShowingFollowedTopics ? R.string.reader_title_followed_topics : R.string.reader_title_popular_topics);

        // show "navigate next" image to the right of the title if followed topics is showing, show "navigate
        // previous" to the left of the title if popular topics is showing
        if (mIsShowingFollowedTopics) {
            mTxtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_navigate_next, 0);
        } else {
            mTxtTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigate_previous, 0, 0, 0);
        }

        // update empty list text
        final TextView txtEmpty = (TextView) findViewById(R.id.text_empty);
        txtEmpty.setText(mIsShowingFollowedTopics ? R.string.reader_empty_followed_topics : R.string.reader_empty_popular_topics);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_TOPICS_CHANGED, mTopicsChanged);
        outState.putBoolean(KEY_TOPIC_LIST_UPDATED, mAlreadyUpdatedTopicList);
        outState.putBoolean(KEY_SHOWING_FOLLOWED, mIsShowingFollowedTopics);
        if (mLastAddedTopicName!=null)
            outState.putString(KEY_LAST_ADDED_TOPIC, mLastAddedTopicName);
    }

    @Override
    public void onBackPressed() {
        if (mTopicsChanged) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(KEY_TOPICS_CHANGED, true);
            if (mLastAddedTopicName!=null && ReaderTopicTable.topicExists(mLastAddedTopicName))
                bundle.putString(KEY_LAST_ADDED_TOPIC, mLastAddedTopicName);
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
        mIsShowingFollowedTopics = !mIsShowingFollowedTopics;

        hideMessageBar(true);
        updateTitle();
        mLayoutAddTopic.setVisibility(mIsShowingFollowedTopics ? View.VISIBLE : View.GONE);

        mAdapter.setTopicType(mIsShowingFollowedTopics ? ReaderTopicType.SUBSCRIBED : ReaderTopicType.RECOMMENDED);
        getListView().setAdapter(mAdapter);
    }

    /*
     * add the topic in the EditText to the user's topic list
     */
    private void addCurrentTopic() {
        String topicName = EditTextUtils.getText(mEditAddTopic);
        if (TextUtils.isEmpty(topicName))
            return;

        if (ReaderTopicTable.topicExists(topicName)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_topic_exists, ToastUtils.Duration.LONG);
            return;
        }

        if (!ReaderTopic.isValidTopicName(topicName)) {
            ToastUtils.showToast(this, R.string.reader_toast_err_topic_invalid, ToastUtils.Duration.LONG);
            return;
        }

        mEditAddTopic.setText(null);
        EditTextUtils.hideSoftInput(mEditAddTopic);

        onTopicAction(ReaderTopicActions.TopicAction.ADD, topicName);
    }

    /*
     * triggered by adapter when user chooses to add/remove a topic, or from this activity when
     * user chooses to add a topic
     */
    @Override
    public void onTopicAction(ReaderTopicActions.TopicAction action, final String topicName) {
        if (TextUtils.isEmpty(topicName))
            return;

        showMessageBar(action, topicName);

        switch (action) {
            case ADD:
                if (ReaderTopicActions.performTopicAction(ReaderTopicActions.TopicAction.ADD, topicName, null)) {
                    ReaderActions.DataLoadedListener dataListener = new ReaderActions.DataLoadedListener() {
                        @Override
                        public void onDataLoaded(boolean isEmpty) {
                            // make sure the added topic is scrolled into view if showing followed topics
                            if (mIsShowingFollowedTopics) {
                                int index = getTopicAdapter().indexOfTopicName(topicName);
                                if (index > -1)
                                    getListView().smoothScrollToPosition(index);
                            }
                        }
                    };
                    getTopicAdapter().refreshTopics(dataListener);
                    mTopicsChanged = true;
                    mLastAddedTopicName = topicName;
                }
                break;

            case DELETE:
                if (ReaderTopicActions.performTopicAction(ReaderTopicActions.TopicAction.DELETE, topicName, null)) {
                    getTopicAdapter().refreshTopics();
                    if (mLastAddedTopicName!=null && mLastAddedTopicName.equals(topicName))
                        mLastAddedTopicName = null;
                    mTopicsChanged = true;
                }
                break;
        }
    }

    /*
     * request latest list of topics from the server
     */
    protected void updateTopicList() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                if (result==ReaderActions.UpdateResult.CHANGED) {
                    mTopicsChanged = true;
                    getTopicAdapter().refreshTopics();
                }
            }
        };
        ReaderTopicActions.updateTopics(listener);
    }

    /*
     * animate in from the bottom a message stating that a topic has been added or removed,
     * then animate it back out after a brief delay
     */
    private void showMessageBar(ReaderTopicActions.TopicAction action, String topicName) {
        hideMessageBar(true);

        if (mTxtMessageBar==null || mTxtMessageBar.getVisibility()==View.VISIBLE)
            return;

        switch (action) {
            case ADD:
                mTxtMessageBar.setText(getString(R.string.reader_label_added_topic, topicName));
                mTxtMessageBar.setBackgroundResource(R.color.reader_message_bar_blue);
                break;
            case DELETE:
                mTxtMessageBar.setText(getString(R.string.reader_label_removed_topic, topicName));
                mTxtMessageBar.setBackgroundResource(R.color.reader_message_bar_orange);
                break;
            default :
                return;
        }

        ReaderAniUtils.startAnimation(mTxtMessageBar, R.anim.reader_bottom_bar_in);
        mTxtMessageBar.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideMessageBar(false);
            }
        }, 1500);
    }

    private void hideMessageBar(boolean immediate) {
        if (mTxtMessageBar==null || mTxtMessageBar.getVisibility()!=View.VISIBLE)
            return;

        if (immediate) {
            mTxtMessageBar.clearAnimation();
            mTxtMessageBar.setVisibility(View.GONE);
            return;
        }

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                mTxtMessageBar.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        ReaderAniUtils.startAnimation(mTxtMessageBar, R.anim.reader_bottom_bar_out, listener);
    }
}
