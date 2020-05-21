package org.wordpress.android.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.tools.FormattableRangeType;
import org.wordpress.android.models.Note;
import org.wordpress.android.push.GCMMessageHandler;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.CollapseFullScreenDialogFragment;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.posts.BasicFragmentDialog;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
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

import static org.wordpress.android.models.Note.NOTE_COMMENT_LIKE_TYPE;
import static org.wordpress.android.models.Note.NOTE_COMMENT_TYPE;
import static org.wordpress.android.models.Note.NOTE_FOLLOW_TYPE;
import static org.wordpress.android.models.Note.NOTE_LIKE_TYPE;
import static org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION;

public class NotificationsDetailActivity extends LocaleAwareActivity implements
        CommentActions.OnNoteCommentActionListener,
        BasicFragmentDialog.BasicDialogPositiveClickInterface {
    private static final String ARG_TITLE = "activityTitle";
    private static final String DOMAIN_WPCOM = "wordpress.com";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject GCMMessageHandler mGCMMessageHandler;

    private String mNoteId;
    private boolean mIsTappedOnNotification;

    private WPViewPager mViewPager;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;
    private NotificationDetailFragmentAdapter mAdapter;

    @Override
    public void onBackPressed() {
        CollapseFullScreenDialogFragment fragment = (CollapseFullScreenDialogFragment)
                getSupportFragmentManager().findFragmentByTag(CollapseFullScreenDialogFragment.TAG);

        if (fragment != null) {
            fragment.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        AppLog.i(AppLog.T.NOTIFS, "Creating NotificationsDetailActivity");

        setContentView(R.layout.notifications_detail_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mNoteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            mIsTappedOnNotification = getIntent().getBooleanExtra(IS_TAPPED_ON_NOTIFICATION, false);
        } else {
            if (savedInstanceState.containsKey(ARG_TITLE) && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(StringUtils.notNullStr(savedInstanceState.getString(ARG_TITLE)));
            }
            mNoteId = savedInstanceState.getString(NotificationsListFragment.NOTE_ID_EXTRA);
            mIsTappedOnNotification = savedInstanceState.getBoolean(IS_TAPPED_ON_NOTIFICATION);
        }

        // set up the viewpager and adapter for lateral navigation
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setPageTransformer(false,
                new WPViewPagerTransformer(WPViewPagerTransformer.TransformType.SLIDE_OVER));

        Note note = NotificationsTable.getNoteById(mNoteId);
        // if this is coming from a tapped push notification, let's try refreshing it as its contents may have been
        // updated since the notification was first received and created on the system's dashboard
        updateUIAndNote((note == null) || mIsTappedOnNotification);

        // Hide the keyboard, unless we arrived here from the 'Reply' action in a push notification
        if (!getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false)) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
    }

    private void updateUIAndNote(boolean doRefresh) {
        if (mNoteId == null) {
            showErrorToastAndFinish();
            return;
        }

        if (doRefresh) {
            setProgressVisible(true);
            // here start the service and wait for it
            NotificationsUpdateServiceStarter.startService(this, mNoteId);
            return;
        }

        Note note = NotificationsTable.getNoteById(mNoteId);
        if (note == null) {
            // no note found
            showErrorToastAndFinish();
            return;
        }

        // here compare the current Note - is it any different from the Note the user is
        // currently viewing?
        // if it is, then replace the dataset with the new one.
        // If not, just let it be.
        if (mAdapter != null) {
            Note currentNote = mAdapter.getNoteWithId(mNoteId);
            if (note.equalsTimeAndLength(currentNote)) {
                return;
            }
        }

        NotesAdapter.FILTERS filter = NotesAdapter.FILTERS.FILTER_ALL;
        if (getIntent().hasExtra(NotificationsListFragment.NOTE_CURRENT_LIST_FILTER_EXTRA)) {
            filter = (NotesAdapter.FILTERS) getIntent()
                    .getSerializableExtra(NotificationsListFragment.NOTE_CURRENT_LIST_FILTER_EXTRA);
        }

        mAdapter = buildNoteListAdapterAndSetPosition(note, filter);

        resetOnPageChangeListener();

        // set title
        setActionBarTitleForNote(note);
        markNoteAsRead(note);

        // If `note.getTimestamp()` is not the most recent seen note, the server will discard the value.
        NotificationsActions.updateSeenTimestamp(note);

        // analytics tracking
        Map<String, String> properties = new HashMap<>();
        properties.put("notification_type", note.getType());
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS, properties);

        setProgressVisible(false);
    }

    private void resetOnPageChangeListener() {
        if (mOnPageChangeListener != null) {
            mViewPager.removeOnPageChangeListener(mOnPageChangeListener);
        } else {
            mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_SWIPE_PAGE_CHANGED);
                    // change the action bar title for the current note
                    Note currentNote = mAdapter.getNoteAtPosition(position);
                    if (currentNote != null) {
                        setActionBarTitleForNote(currentNote);
                        markNoteAsRead(currentNote);
                        NotificationsActions.updateSeenTimestamp(currentNote);
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            };
        }
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
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
        outState.putBoolean(IS_TAPPED_ON_NOTIFICATION, mIsTappedOnNotification);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        // If the user hasn't used swipe yet and if the adapter is initialised and have at least 2 notifications,
        // show a hint to promote swipe usage on the ViewPager
        if (!AppPrefs.isNotificationsSwipeToNavigateShown() && mAdapter != null && mAdapter.getCount() > 1) {
            WPSwipeSnackbar.show(mViewPager);
            AppPrefs.setNotificationsSwipeToNavigateShown(true);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    private void showErrorToastAndFinish() {
        AppLog.e(AppLog.T.NOTIFS, "Note could not be found.");
        ToastUtils.showToast(this, R.string.error_notification_open);
        finish();
    }

    private void markNoteAsRead(Note note) {
        mGCMMessageHandler.removeNotificationWithNoteIdFromSystemBar(this, note.getId());
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
                // set a default title if title is not set within the note
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
            // important for accessibility - talkback
            setTitle(getString(R.string.notif_detail_screen_title, title));
        }
    }

    private NotificationDetailFragmentAdapter buildNoteListAdapterAndSetPosition(Note note,
                                                                                 NotesAdapter.FILTERS filter) {
        NotificationDetailFragmentAdapter adapter;
        ArrayList<Note> notes = NotificationsTable.getLatestNotes();
        ArrayList<Note> filteredNotes = new ArrayList<>();

        // apply filter to the list so we show the same items that the list show vertically, but horizontally
        NotesAdapter.buildFilteredNotesList(filteredNotes, notes, filter);
        adapter = new NotificationDetailFragmentAdapter(getSupportFragmentManager(), filteredNotes);

        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(NotificationsUtils.findNoteInNoteArray(filteredNotes, note.getId()));

        return adapter;
    }

    /**
     * Tries to pick the correct fragment detail type for a given note
     * Defaults to NotificationDetailListFragment
     */
    private Fragment getDetailFragmentForNote(Note note) {
        if (note == null) {
            return null;
        }

        Fragment fragment;
        if (note.isCommentType()) {
            // show comment detail for comment notifications
            boolean isInstantReply = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA,
                    false);
            fragment = CommentDetailFragment.newInstance(note.getId(),
                    getIntent().getStringExtra(NotificationsListFragment.NOTE_PREFILLED_REPLY_EXTRA));

            if (isInstantReply) {
                ((CommentDetailFragment) fragment).enableShouldFocusReplyField();
            }
        } else if (note.isAutomattcherType()) {
            // show reader post detail for automattchers about posts - note that comment
            // automattchers are handled by note.isCommentType() above
            boolean isPost = (note.getSiteId() != 0 && note.getPostId() != 0 && note.getCommentId() == 0);
            if (isPost) {
                fragment = ReaderPostDetailFragment.Companion.newInstance(
                        note.getSiteId(),
                        note.getPostId()
                );
            } else {
                fragment = NotificationsDetailListFragment.newInstance(note.getId());
            }
        } else if (note.isNewPostType()) {
            fragment = ReaderPostDetailFragment.Companion.newInstance(
                    note.getSiteId(),
                    note.getPostId()
            );
        } else {
            fragment = NotificationsDetailListFragment.newInstance(note.getId());
        }

        return fragment;
    }

    public void showBlogPreviewActivity(long siteId) {
        if (isFinishing()) {
            return;
        }

        ReaderActivityLauncher.showReaderBlogPreview(this, siteId);
    }

    public void showPostActivity(long siteId, long postId) {
        if (isFinishing()) {
            return;
        }

        ReaderActivityLauncher.showReaderPostDetail(this, siteId, postId);
    }

    public void showStatsActivityForSite(long siteId, FormattableRangeType rangeType) {
        SiteModel site = mSiteStore.getSiteBySiteId(siteId);
        if (site == null) {
            // One way the site can be null: new site created, receive a notification from this site,
            // but the site list is not yet updated in the app.
            ToastUtils.showToast(this, R.string.blog_not_found);
            return;
        }
        showStatsActivityForSite(site, rangeType);
    }

    private void showStatsActivityForSite(@NonNull SiteModel site, FormattableRangeType rangeType) {
        if (isFinishing()) {
            return;
        }

        if (rangeType == FormattableRangeType.FOLLOW) {
            ActivityLauncher.viewAllTabbedInsightsStats(this, StatsViewType.FOLLOWERS, 0,
                    site.getId());
        } else {
            ActivityLauncher.viewBlogStats(this, site);
        }
    }

    public void showWebViewActivityForUrl(String url) {
        if (isFinishing() || url == null) {
            return;
        }

        if (url.contains(DOMAIN_WPCOM)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(this, url);
        } else {
            WPWebViewActivity.openURL(this, url);
        }
    }

    public void showReaderPostLikeUsers(long blogId, long postId) {
        if (isFinishing()) {
            return;
        }

        ReaderActivityLauncher.showReaderLikingUsers(this, blogId, postId);
    }

    public void showReaderCommentsList(long siteId, long postId, long commentId) {
        if (isFinishing()) {
            return;
        }

        ReaderActivityLauncher.showReaderComments(this, siteId, postId, commentId);
    }

    private void setProgressVisible(boolean visible) {
        final ProgressBar progress =
                findViewById(R.id.progress_loading);
        if (progress != null) {
            progress.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onModerateCommentForNote(Note note, CommentStatus newStatus) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(NotificationsListFragment.NOTE_MODERATE_ID_EXTRA, note.getId());
        resultIntent.putExtra(NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA, newStatus.toString());

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(final NotificationEvents.NotificationsRefreshCompleted event) {
        setProgressVisible(false);
        updateUIAndNote(false);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NotificationEvents.NotificationsRefreshError error) {
        setProgressVisible(false);
        if (mNoteId == null) {
            showErrorToastAndFinish();
            return;
        }

        Note note = NotificationsTable.getNoteById(mNoteId);
        if (note == null) {
            // no note found
            showErrorToastAndFinish();
            return;
        }
    }

    @Override
    public void onPositiveClicked(@NotNull String instanceTag) {
        Fragment fragment = mAdapter.getItem(mViewPager.getCurrentItem());
        if (fragment instanceof BasicFragmentDialog.BasicDialogPositiveClickInterface) {
            ((BasicDialogPositiveClickInterface) fragment).onPositiveClicked(instanceTag);
        }
    }

    private class NotificationDetailFragmentAdapter extends FragmentStatePagerAdapter {
        private final ArrayList<Note> mNoteList;

        NotificationDetailFragmentAdapter(FragmentManager fm, ArrayList<Note> notes) {
            super(fm);
            mNoteList = (ArrayList<Note>) notes.clone();
        }

        @Override
        public Fragment getItem(int position) {
            return getDetailFragmentForNote(mNoteList.get(position));
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
            Bundle bundle = (Bundle) super.saveState();
            if (bundle == null) {
                bundle = new Bundle();
            }
            // This is a possible solution to https://github.com/wordpress-mobile/WordPress-Android/issues/5456
            // See https://issuetracker.google.com/issues/37103380#comment77 for more details
            bundle.putParcelableArray("states", null);
            return bundle;
        }

        boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
        }

        private Note getNoteAtPosition(int position) {
            if (isValidPosition(position)) {
                return mNoteList.get(position);
            } else {
                return null;
            }
        }

        private Note getNoteWithId(String id) {
            for (Note note : mNoteList) {
                if (note.getId().equalsIgnoreCase(id)) {
                    return note;
                }
            }
            return null;
        }
    }
}
