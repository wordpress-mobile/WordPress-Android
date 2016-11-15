package org.wordpress.android.ui.notifications;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.WindowManager;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.notifications.blocks.NoteBlockRangeType;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.ReaderViewPagerTransformer;
import org.wordpress.android.ui.stats.StatsAbstractFragment;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsTimeframe;
import org.wordpress.android.ui.stats.StatsViewAllActivity;
import org.wordpress.android.ui.stats.StatsViewType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPViewPager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.models.Note.NOTE_COMMENT_LIKE_TYPE;
import static org.wordpress.android.models.Note.NOTE_COMMENT_TYPE;
import static org.wordpress.android.models.Note.NOTE_FOLLOW_TYPE;
import static org.wordpress.android.models.Note.NOTE_LIKE_TYPE;

public class NotificationsDetailActivity extends AppCompatActivity implements
        CommentActions.OnNoteCommentActionListener {
    private static final String ARG_TITLE = "activityTitle";
    private static final String DOMAIN_WPCOM = "wordpress.com";

    private WPViewPager mViewPager;
    private NotificationDetailFragmentAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.i(AppLog.T.NOTIFS, "Creating NotificationsDetailActivity");

        setContentView(R.layout.notifications_detail_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            String noteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            if (noteId == null) {
                showErrorToastAndFinish();
                return;
            }

            final Note note = NotificationsTable.getNoteById(noteId);
            if (note == null) {
                showErrorToastAndFinish();
                return;
            }

            // If `note.getTimestamp()` is not the most recent seen note, the server will discard the value.
            NotificationsActions.updateSeenTimestamp(note);

            Map<String, String> properties = new HashMap<>();
            properties.put("notification_type", note.getType());
            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS, properties);

            //set up the viewpager and adapter for lateral navigation
            mViewPager = (WPViewPager) findViewById(R.id.viewpager);
            mViewPager.setPageTransformer(false,
                    new ReaderViewPagerTransformer(ReaderViewPagerTransformer.TransformType.SLIDE_OVER));
            boolean allowNavigateList = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_ALLOW_NAVIGATE_LIST_EXTRA, false);
            mAdapter = buildNoteListAdapterAndSetPosition(allowNavigateList, note);

            //set title
            setActionBarTitleForNote(note);
            markNoteAsRead(note);

            mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    //change the action bar title for the current note
                    Note currentNote = mAdapter.getNoteAtPosition(position);
                    if (currentNote != null) {
                        setActionBarTitleForNote(currentNote);
                        markNoteAsRead(currentNote);
                    }
                }
            });


        } else if (savedInstanceState.containsKey(ARG_TITLE) && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(StringUtils.notNullStr(savedInstanceState.getString(ARG_TITLE)));
        }

        // Hide the keyboard, unless we arrived here from the 'Reply' action in a push notification
        if (!getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false)) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (getSupportActionBar() != null && getSupportActionBar().getTitle() != null) {
            outState.putString(ARG_TITLE, getSupportActionBar().getTitle().toString());
        }

        super.onSaveInstanceState(outState);
    }

    private void showErrorToastAndFinish() {
        AppLog.e(AppLog.T.NOTIFS, "Note could not be found.");
        ToastUtils.showToast(this, R.string.error_notification_open);
        finish();
    }

    private void markNoteAsRead(Note note) {
        GCMMessageService.removeNotificationWithNoteIdFromSystemBar(this, note.getId());
        // mark the note as read if it's unread
        if (note.isUnread()) {
            NotificationsActions.markNoteAsRead(note);
            note.setRead();
            NotificationsTable.saveNote(note);
            EventBus.getDefault().post(new NotificationEvents.NotificationsChanged());
        }
    }

    private void setActionBarTitleForNote(Note note) {
        if (getSupportActionBar() != null) {
            String title = note.getTitle();
            if (TextUtils.isEmpty(title)) {
                //set a default title if title is not set within the note
                switch (note.getType()) {
                    case NOTE_FOLLOW_TYPE:
                        title = getString(R.string.follows);
                        break;
                    case NOTE_COMMENT_LIKE_TYPE:
                        title = getString(R.string.comment_likes);
                        break;
                    case NOTE_LIKE_TYPE:
                        title = getString(R.string.like);
                        break;
                    case NOTE_COMMENT_TYPE:
                        title = getString(R.string.comment);
                        break;
                }
            }

            // Force change the Action Bar title for 'new_post' notifications.
            if (note.isNewPostType()) {
                title = getString(R.string.reader_title_post_detail);
            }

            getSupportActionBar().setTitle(title);
        }
    }

    private NotificationDetailFragmentAdapter buildNoteListAdapterAndSetPosition(boolean allowNavigateList, Note note) {
        NotificationDetailFragmentAdapter adapter;
        ArrayList<Note> notes = NotificationsTable.getLatestNotes();

        if (allowNavigateList) {
            adapter = new NotificationDetailFragmentAdapter(getFragmentManager(), notes);
        } else {
            ArrayList<Note> oneNoteList = new ArrayList<>();
            oneNoteList.add(note);
            adapter = new NotificationDetailFragmentAdapter(getFragmentManager(),
                    oneNoteList);
        }

        mViewPager.setAdapter(adapter);

        if (allowNavigateList) {
            mViewPager.setCurrentItem(findNoteInNoteArray(notes, note));
        }

        return adapter;
    }

    private int findNoteInNoteArray(ArrayList<Note> notes, Note noteToSearchFor) {
        if (notes == null || noteToSearchFor == null || noteToSearchFor.getId() == null) return -1;

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (noteToSearchFor.getId().equals(note.getId()))
                return i;
        }
        return -1;
    }


    /**
     * Tries to pick the correct fragment detail type for a given note
     * Defaults to NotificationDetailListFragment
     */
    private Fragment getDetailFragmentForNote(Note note) {
        if (note == null)
            return null;

        Fragment fragment;
        if (note.isCommentType()) {
            // show comment detail for comment notifications
            boolean isInstantReply = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false);
            fragment = CommentDetailFragment.newInstance(note.getId(), getIntent().getStringExtra(NotificationsListFragment.NOTE_PREFILLED_REPLY_EXTRA));

            // fragment is never null at this point, and always of CommentDetailFragment type. Just add this check for safety :)
            if ( fragment != null && fragment instanceof  CommentDetailFragment) {
                if (isInstantReply) {
                    ((CommentDetailFragment) fragment).enableShouldFocusReplyField();
                }
            }

        } else if (note.isAutomattcherType()) {
            // show reader post detail for automattchers about posts - note that comment
            // automattchers are handled by note.isCommentType() above
            boolean isPost = (note.getSiteId() != 0 && note.getPostId() != 0 && note.getCommentId() == 0);
            if (isPost) {
                fragment = ReaderPostDetailFragment.newInstance(note.getSiteId(), note.getPostId());
            } else {
                fragment = NotificationsDetailListFragment.newInstance(note.getId());
            }
        } else if (note.isNewPostType()) {
            fragment = ReaderPostDetailFragment.newInstance(note.getSiteId(), note.getPostId());
        } else {
            fragment = NotificationsDetailListFragment.newInstance(note.getId());
        }

        return fragment;
    }

    public void showBlogPreviewActivity(long siteId) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderBlogPreview(this, siteId);
    }

    public void showPostActivity(long siteId, long postId) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderPostDetail(this, siteId, postId);
    }

    public void showStatsActivityForSite(int localTableSiteId, NoteBlockRangeType rangeType) {
        if (isFinishing()) return;

        if (rangeType == NoteBlockRangeType.FOLLOW) {
            Intent intent = new Intent(this, StatsViewAllActivity.class);
            intent.putExtra(StatsAbstractFragment.ARGS_VIEW_TYPE, StatsViewType.FOLLOWERS);
            intent.putExtra(StatsAbstractFragment.ARGS_TIMEFRAME, StatsTimeframe.DAY);
            intent.putExtra(StatsAbstractFragment.ARGS_SELECTED_DATE, "");
            intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableSiteId);
            intent.putExtra(StatsViewAllActivity.ARG_STATS_VIEW_ALL_TITLE, getString(R.string.stats_view_followers));
            startActivity(intent);
        } else {
            ActivityLauncher.viewBlogStats(this, localTableSiteId);
        }
    }

    public void showWebViewActivityForUrl(String url) {
        if (isFinishing() || url == null) return;

        if (url.contains(DOMAIN_WPCOM)) {
            WPWebViewActivity.openUrlByUsingWPCOMCredentials(this, url, AccountHelper.getDefaultAccount().getUserName());
        } else {
            WPWebViewActivity.openURL(this, url);
        }
    }

    public void showReaderPostLikeUsers(long blogId, long postId) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderLikingUsers(this, blogId, postId);
    }

    public void showReaderCommentsList(long siteId, long postId, long commentId) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderComments(this, siteId, postId, commentId);
    }

    @Override
    public void onModerateCommentForNote(Note note, CommentStatus newStatus) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(NotificationsListFragment.NOTE_MODERATE_ID_EXTRA, note.getId());
        resultIntent.putExtra(NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA, CommentStatus.toRESTString(newStatus));

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private class NotificationDetailFragmentAdapter extends FragmentStatePagerAdapter {

        private ArrayList<Note> mNoteList;

        NotificationDetailFragmentAdapter(FragmentManager fm, ArrayList<Note> notes) {
            super(fm);
            mNoteList = (ArrayList<Note>)notes.clone();
        }

        @Override
        public Fragment getItem(int position) {
            return getDetailFragmentForNote(mNoteList.get(position));
        }

        @Override
        public int getCount() {
            return mNoteList.size();
        }

        boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
        }

        public Note getNoteAtPosition(int position){
            if (isValidPosition(position))
                return mNoteList.get(position);
            else
                return null;
        }

    }
}
