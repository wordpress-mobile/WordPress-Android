package org.wordpress.android.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.databinding.NotificationsDetailActivityBinding;
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
import org.wordpress.android.ui.ScrollableViewInitializedListener;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.engagement.EngagedPeopleListFragment;
import org.wordpress.android.ui.engagement.ListScenarioUtils;
import org.wordpress.android.ui.notifications.adapters.Filter;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.posts.BasicFragmentDialog;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource;
import org.wordpress.android.ui.reader.tracker.ReaderTracker;
import org.wordpress.android.ui.stats.StatsViewType;
import org.wordpress.android.ui.stats.refresh.utils.StatsLaunchedFrom;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource;
import org.wordpress.android.util.config.LikesEnhancementsFeatureConfig;
import org.wordpress.android.util.extensions.AppBarLayoutExtensionsKt;
import org.wordpress.android.util.extensions.CompatExtensionsKt;
import org.wordpress.android.widgets.WPSwipeSnackbar;
import org.wordpress.android.widgets.WPViewPager2Transformer;
import org.wordpress.android.widgets.WPViewPager2Transformer.TransformType.SlideOver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import static org.wordpress.android.models.Note.NOTE_COMMENT_LIKE_TYPE;
import static org.wordpress.android.models.Note.NOTE_COMMENT_TYPE;
import static org.wordpress.android.models.Note.NOTE_FOLLOW_TYPE;
import static org.wordpress.android.models.Note.NOTE_LIKE_TYPE;
import static org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter.IS_TAPPED_ON_NOTIFICATION;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
@SuppressWarnings("deprecation")
public class NotificationsDetailActivity extends LocaleAwareActivity implements
        CommentActions.OnNoteCommentActionListener,
        BasicFragmentDialog.BasicDialogPositiveClickInterface, ScrollableViewInitializedListener {
    private static final String ARG_TITLE = "activityTitle";
    private static final String DOMAIN_WPCOM = "wordpress.com";

    private NotificationsListViewModel mViewModel;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject GCMMessageHandler mGCMMessageHandler;
    @Inject ReaderTracker mReaderTracker;
    @Inject LikesEnhancementsFeatureConfig mLikesEnhancementsFeatureConfig;
    @Inject ListScenarioUtils mListScenarioUtils;

    @Nullable private String mNoteId;
    private boolean mIsTappedOnNotification;

    @Nullable private ViewPager2.OnPageChangeCallback mOnPageChangeListener;
    @Nullable private NotificationDetailFragmentAdapter mAdapter;

    @Nullable private NotificationsDetailActivityBinding mBinding = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        AppLog.i(AppLog.T.NOTIFS, "Creating NotificationsDetailActivity");

        mViewModel = new ViewModelProvider(this).get(NotificationsListViewModel.class);
        mBinding = NotificationsDetailActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                CollapseFullScreenDialogFragment fragment = (CollapseFullScreenDialogFragment)
                        getSupportFragmentManager().findFragmentByTag(CollapseFullScreenDialogFragment.TAG);

                if (fragment != null) {
                    fragment.collapse();
                } else {
                    CompatExtensionsKt.onBackPressedCompat(getOnBackPressedDispatcher(), this);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        if (mBinding != null) {
            setSupportActionBar(mBinding.toolbarMain);
        }

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
        if (mBinding != null) {
            mBinding.viewpager.setPageTransformer(new WPViewPager2Transformer(SlideOver.INSTANCE));
        }

        Note note = NotificationsTable.getNoteById(mNoteId);
        // if this is coming from a tapped push notification, let's try refreshing it as its contents may have been
        // updated since the notification was first received and created on the system's dashboard
        updateUIAndNote((note == null) || mIsTappedOnNotification);

        // Hide the keyboard, unless we arrived here from the 'Reply' action in a push notification
        if (!getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false)) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        // track initial comment note view
        if (savedInstanceState == null && note != null) {
            trackCommentNote(note);
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

        Filter filter = Filter.ALL;
        if (getIntent().hasExtra(NotificationsListFragment.NOTE_CURRENT_LIST_FILTER_EXTRA)) {
            filter = (Filter) getIntent()
                    .getSerializableExtra(NotificationsListFragment.NOTE_CURRENT_LIST_FILTER_EXTRA);
        }

        mAdapter = buildNoteListAdapterAndSetPosition(note, filter);

        resetOnPageChangeListener();

        // set title
        setActionBarTitleForNote(note);
        mViewModel.markNoteAsRead(this, Collections.singletonList(note));

        // If `note.getTimestamp()` is not the most recent seen note, the server will discard the value.
        NotificationsActions.updateSeenTimestamp(note);

        // analytics tracking
        Map<String, String> properties = new HashMap<>();
        properties.put("notification_type", note.getRawType());
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS, properties);

        setProgressVisible(false);
    }

    private void resetOnPageChangeListener() {
        if (mOnPageChangeListener != null) {
            if (mBinding != null) {
                mBinding.viewpager.unregisterOnPageChangeCallback(mOnPageChangeListener);
            }
        } else {
            mOnPageChangeListener = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    if (mBinding != null && mAdapter != null) {
                        Fragment fragment = mAdapter.createFragment(mBinding.viewpager.getCurrentItem());
                        boolean hideToolbar = (fragment instanceof ReaderPostDetailFragment);
                        showHideToolbar(hideToolbar);

                        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_SWIPE_PAGE_CHANGED);
                        // change the action bar title for the current note
                        Note currentNote = mAdapter.getNoteAtPosition(position);
                        if (currentNote != null) {
                            setActionBarTitleForNote(currentNote);
                            mViewModel.markNoteAsRead(NotificationsDetailActivity.this,
                                    Collections.singletonList(currentNote));
                            NotificationsActions.updateSeenTimestamp(currentNote);
                            // track subsequent comment note views
                            trackCommentNote(currentNote);
                        }
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            };
        }
        if (mBinding != null) {
            mBinding.viewpager.registerOnPageChangeCallback(mOnPageChangeListener);
        }
    }

    private void trackCommentNote(@NonNull Note note) {
        if (note.isCommentType()) {
            SiteModel site = mSiteStore.getSiteBySiteId(note.getSiteId());
            AnalyticsUtils.trackCommentActionWithSiteDetails(
                    Stat.COMMENT_VIEWED, AnalyticsCommentActionSource.NOTIFICATIONS, site);
        }
    }

    public void showHideToolbar(boolean hide) {
        if (mBinding != null) {
            setSupportActionBar(mBinding.toolbarMain);
        }
        if (getSupportActionBar() != null) {
            if (hide) {
                getSupportActionBar().hide();
            } else {
                getSupportActionBar().show();
            }
            getSupportActionBar().setDisplayShowTitleEnabled(!hide);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
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
        if (!AppPrefs.isNotificationsSwipeToNavigateShown() && mAdapter != null && 1 < mAdapter.getItemCount()) {
            if (mBinding != null) {
                WPSwipeSnackbar.show(mBinding.viewpager);
                AppPrefs.setNotificationsSwipeToNavigateShown(true);
            }
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

    private void setActionBarTitleForNote(Note note) {
        if (getSupportActionBar() != null) {
            String title = note.getTitle();
            if (TextUtils.isEmpty(title)) {
                // set a default title if title is not set within the note
                switch (note.getRawType()) {
                    case NOTE_FOLLOW_TYPE:
                        title = getString(R.string.subscribers);
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
                                                                                 Filter filter) {
        NotificationDetailFragmentAdapter adapter;
        ArrayList<Note> notes = NotificationsTable.getLatestNotes();

        // apply filter to the list so we show the same items that the list show vertically, but horizontally
        ArrayList<Note> filteredNotes = NotesAdapter.buildFilteredNotesList(notes, filter);

        adapter = new NotificationDetailFragmentAdapter(getSupportFragmentManager(), getLifecycle(), filteredNotes);

        if (mBinding != null) {
            mBinding.viewpager.setAdapter(adapter);
            mBinding.viewpager.setCurrentItem(
                    NotificationsUtils.findNoteInNoteArray(filteredNotes, note.getId()), false);
        }

        return adapter;
    }

    /**
     * Tries to pick the correct fragment detail type for a given note
     * Defaults to NotificationDetailListFragment
     */
    @NonNull
    private Fragment createDetailFragmentForNote(@NonNull Note note) {
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
            if (mLikesEnhancementsFeatureConfig.isEnabled() && note.isLikeType()) {
                fragment = EngagedPeopleListFragment.newInstance(
                        mListScenarioUtils.mapLikeNoteToListScenario(note, this)
                );
            } else {
                fragment = NotificationsDetailListFragment.newInstance(note.getId());
            }
        }

        return fragment;
    }

    public void showBlogPreviewActivity(long siteId, @Nullable Boolean isFollowed) {
        if (isFinishing()) {
            return;
        }

        ReaderActivityLauncher.showReaderBlogPreview(
                this,
                siteId,
                isFollowed,
                ReaderTracker.SOURCE_NOTIFICATION,
                mReaderTracker
        );
    }

    public void showPostActivity(long siteId, long postId) {
        if (isFinishing()) {
            return;
        }

        ReaderActivityLauncher.showReaderPostDetail(this, siteId, postId);
    }

    public void showScanActivityForSite(long siteId) {
        SiteModel site = getSiteOrToast(siteId);
        if (site != null) {
            ActivityLauncher.viewScan(this, site);
        }
    }

    public void showStatsActivityForSite(long siteId, FormattableRangeType rangeType) {
        SiteModel site = getSiteOrToast(siteId);
        if (site != null) {
            showStatsActivityForSite(site, rangeType);
        }
    }

    public void showBackupForSite(long siteId) {
        SiteModel site = getSiteOrToast(siteId);
        if (site != null) {
            showBackupActivityForSite(site);
        }
    }

    @Nullable
    private SiteModel getSiteOrToast(long siteId) {
        SiteModel site = mSiteStore.getSiteBySiteId(siteId);
        if (site == null) {
            // One way the site can be null: new site created, receive a notification from this site,
            // but the site list is not yet updated in the app.
            ToastUtils.showToast(this, R.string.blog_not_found);
        }
        return site;
    }

    private void showStatsActivityForSite(@NonNull SiteModel site, FormattableRangeType rangeType) {
        if (isFinishing()) {
            return;
        }

        if (rangeType == FormattableRangeType.FOLLOW) {
            ActivityLauncher.viewAllTabbedInsightsStats(this, StatsViewType.FOLLOWERS, 0,
                    site.getId());
        } else {
            ActivityLauncher.viewBlogStats(this, site, StatsLaunchedFrom.NOTIFICATION);
        }
    }

    private void showBackupActivityForSite(@NonNull SiteModel site) {
        if (isFinishing()) {
            return;
        }

        ActivityLauncher.viewBackupList(this, site);
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

        ReaderActivityLauncher.showReaderComments(
                this,
                siteId,
                postId,
                commentId,
                ThreadedCommentsActionSource.COMMENT_NOTIFICATION.getSourceDescription()
        );
    }

    private void setProgressVisible(boolean visible) {
        if (mBinding != null) {
            mBinding.progressLoading.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onModerateCommentForNote(@NonNull Note note, @NonNull CommentStatus newStatus) {
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
        }
    }

    @Override
    public void onPositiveClicked(@NonNull String instanceTag) {
        if (mBinding != null && mAdapter != null) {
            Fragment fragment = mAdapter.createFragment(mBinding.viewpager.getCurrentItem());
            if (fragment instanceof BasicFragmentDialog.BasicDialogPositiveClickInterface) {
                ((BasicDialogPositiveClickInterface) fragment).onPositiveClicked(instanceTag);
            }
        }
    }

    @Override
    public void onScrollableViewInitialized(int containerId) {
        if (mBinding != null) {
            AppBarLayoutExtensionsKt.setLiftOnScrollTargetViewIdAndRequestLayout(mBinding.appbarMain, containerId);
        }
    }

    private class NotificationDetailFragmentAdapter extends FragmentStateAdapter {
        @NonNull
        private final ArrayList<Note> mNoteList;

        @SuppressWarnings("unchecked")
        NotificationDetailFragmentAdapter(@NonNull FragmentManager fm, @NonNull Lifecycle lifecycle,
                                          @NonNull ArrayList<Note> notes) {
            super(fm, lifecycle);
            mNoteList = (ArrayList<Note>) notes.clone();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return createDetailFragmentForNote(mNoteList.get(position));
        }

        @Override
        public int getItemCount() {
            return mNoteList.size();
        }

        boolean isValidPosition(int position) {
            return (position >= 0 && position < getItemCount());
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
