package org.wordpress.android.ui.notifications;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.WindowManager;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.Note;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.blocks.NoteBlockRangeType;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.stats.StatsAbstractFragment;
import org.wordpress.android.ui.stats.StatsTimeframe;
import org.wordpress.android.ui.stats.StatsViewAllActivity;
import org.wordpress.android.ui.stats.StatsViewType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPSwipeSnackbar;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.android.widgets.WPViewPagerTransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.models.Note.NOTE_COMMENT_LIKE_TYPE;
import static org.wordpress.android.models.Note.NOTE_COMMENT_TYPE;
import static org.wordpress.android.models.Note.NOTE_FOLLOW_TYPE;
import static org.wordpress.android.models.Note.NOTE_LIKE_TYPE;

public class NotificationsDetailActivity extends AppCompatActivity implements
        CommentActions.OnNoteCommentActionListener {
    private static final String ARG_TITLE = "activityTitle";
    private static final String DOMAIN_WPCOM = "wordpress.com";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    private String mNoteId;

    private WPViewPager mViewPager;
    private NotificationDetailFragmentAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        AppLog.i(AppLog.T.NOTIFS, "Creating NotificationsDetailActivity");

        setContentView(R.layout.notifications_detail_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mNoteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
        } else {
            if (savedInstanceState.containsKey(ARG_TITLE) && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(StringUtils.notNullStr(savedInstanceState.getString(ARG_TITLE)));
            }
            mNoteId = savedInstanceState.getString(NotificationsListFragment.NOTE_ID_EXTRA);
        }

        if (mNoteId == null) {
            showErrorToastAndFinish();
            return;
        }

        final Note note = NotificationsTable.getNoteById(mNoteId);
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
                new WPViewPagerTransformer(WPViewPagerTransformer.TransformType.SLIDE_OVER));

        NotesAdapter.FILTERS filter = NotesAdapter.FILTERS.FILTER_ALL;
        if (getIntent().hasExtra(NotificationsListFragment.NOTE_CURRENT_LIST_FILTER_EXTRA)) {
            filter = (NotesAdapter.FILTERS) getIntent().getSerializableExtra(NotificationsListFragment.NOTE_CURRENT_LIST_FILTER_EXTRA);
        }
        mAdapter = buildNoteListAdapterAndSetPosition(note, filter);

        //set title
        setActionBarTitleForNote(note);
        markNoteAsRead(note);

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_SWIPE_PAGE_CHANGED);
                AppPrefs.setNotificationsSwipeToNavigateShown(true);
                //change the action bar title for the current note
                Note currentNote = mAdapter.getNoteAtPosition(position);
                if (currentNote != null) {
                    setActionBarTitleForNote(currentNote);
                    markNoteAsRead(currentNote);
                }
            }
        });

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
        outState.putString(NotificationsListFragment.NOTE_ID_EXTRA, mNoteId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //if the user hasn't used swipe yet, hint the user they can navigate through notifications detail
        //using swipe on the ViewPager
        if (!AppPrefs.notificationsSwipeToNavigateShownEh() && (mAdapter.getCount() > 1)) {
            WPSwipeSnackbar.show(mViewPager);
        }
    }

    private void showErrorToastAndFinish() {
        AppLog.e(AppLog.T.NOTIFS, "Note could not be found.");
        ToastUtils.showToast(this, R.string.error_notification_open);
        finish();
    }

    private void markNoteAsRead(Note note) {
        GCMMessageService.removeNotificationWithNoteIdFromSystemBar(this, note.getId());
        // mark the note as read if it's unread
        if (note.unreadEh()) {
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

    private NotificationDetailFragmentAdapter buildNoteListAdapterAndSetPosition(Note note,
                                                                                 NotesAdapter.FILTERS filter) {
        NotificationDetailFragmentAdapter adapter;
        ArrayList<Note> notes = NotificationsTable.getLatestNotes();
        ArrayList<Note> filteredNotes = new ArrayList<>();

        //apply filter to the list so we show the same items that the list show vertically, but horizontally
        NotesAdapter.buildFilteredNotesList(filteredNotes, notes, filter);
        adapter = new NotificationDetailFragmentAdapter(getFragmentManager(), filteredNotes);

        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(NotificationsUtils.findNoteInNoteArray(filteredNotes, note.getId()));

        return adapter;
    }

    /**
     * Tries to pick the correct fragment detail type for a given note
     * Defaults to NotificationDetailListFragment
     */
    private Fragment getDetailFragmentForNote(Note note, int idForFragmentContainer) {
        if (note == null)
            return null;

        Fragment fragment;
        if (note.isCommentType()) {
            // show comment detail for comment notifications
            boolean instantReplyEh = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA,
                    false);
            fragment = CommentDetailFragment.newInstance(note.getId(),
                    getIntent().getStringExtra(NotificationsListFragment.NOTE_PREFILLED_REPLY_EXTRA),
                    idForFragmentContainer);

            if (instantReplyEh) {
                ((CommentDetailFragment) fragment).enableShouldFocusReplyField();
            }
        } else if (note.isAutomattcherType()) {
            // show reader post detail for automattchers about posts - note that comment
            // automattchers are handled by note.isCommentType() above
            boolean postEh = (note.getSiteId() != 0 && note.getPostId() != 0 && note.getCommentId() == 0);
            if (postEh) {
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

    public void showStatsActivityForSite(long siteId, NoteBlockRangeType rangeType) {
        SiteModel site = mSiteStore.getSiteBySiteId(siteId);
        if (site == null) {
            // One way the site can be null: new site created, receive a notification from this site,
            // but the site list is not yet updated in the app.
            ToastUtils.showToast(this, R.string.blog_not_found);
            return;
        }
        showStatsActivityForSite(site, rangeType);
    }

    public void showStatsActivityForSite(@NonNull SiteModel site, NoteBlockRangeType rangeType) {
        if (isFinishing()) return;

        if (rangeType == NoteBlockRangeType.FOLLOW) {
            Intent intent = new Intent(this, StatsViewAllActivity.class);
            intent.putExtra(StatsAbstractFragment.ARGS_VIEW_TYPE, StatsViewType.FOLLOWERS);
            intent.putExtra(StatsAbstractFragment.ARGS_TIMEFRAME, StatsTimeframe.DAY);
            intent.putExtra(StatsAbstractFragment.ARGS_SELECTED_DATE, "");
            intent.putExtra(WordPress.SITE, site);
            intent.putExtra(StatsViewAllActivity.ARG_STATS_VIEW_ALL_TITLE, getString(R.string.stats_view_followers));
            startActivity(intent);
        } else {
            ActivityLauncher.viewBlogStats(this, site);
        }
    }

    public void showWebViewActivityForUrl(String url) {
        if (isFinishing() || url == null) return;

        if (url.contains(DOMAIN_WPCOM)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(this, url);
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
        resultIntent.putExtra(NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA, newStatus.toString());

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
            return getDetailFragmentForNote(mNoteList.get(position), position);
        }

        @Override
        public int getCount() {
            return mNoteList.size();
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // work around "Fragment no longer exists for key" Android bug
            // by catching the IllegalStateException
            // https://code.google.com/p/android/issues/detail?id=42601
            try {
                AppLog.d(AppLog.T.NOTIFS, "notifications pager > adapter restoreState");
                super.restoreState(state, loader);
            } catch (IllegalStateException e) {
                AppLog.e(AppLog.T.NOTIFS, e);
            }
        }

        @Override
        public Parcelable saveState() {
            AppLog.d(AppLog.T.NOTIFS, "notifications pager > adapter saveState");
            return super.saveState();
        }

        boolean validPositionEh(int position) {
            return (position >= 0 && position < getCount());
        }

        public Note getNoteAtPosition(int position){
            if (validPositionEh(position))
                return mNoteList.get(position);
            else
                return null;
        }

    }
}
