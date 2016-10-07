package org.wordpress.android.ui.notifications;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.WindowManager;

import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMMessageService;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.notifications.blocks.NoteBlockRangeType;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.stats.StatsAbstractFragment;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsTimeframe;
import org.wordpress.android.ui.stats.StatsViewAllActivity;
import org.wordpress.android.ui.stats.StatsViewType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.models.Note.NOTE_COMMENT_TYPE;
import static org.wordpress.android.models.Note.NOTE_FOLLOW_TYPE;
import static org.wordpress.android.models.Note.NOTE_LIKE_TYPE;

public class NotificationsDetailActivity extends AppCompatActivity implements
        CommentActions.OnNoteCommentActionListener {
    private static final String ARG_TITLE = "activityTitle";
    private static final String DOMAIN_WPCOM = "wordpress.com";

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

            retrieveOrBuildNoteAndShowNoteDetail(noteId);

            GCMMessageService.removeNotificationWithNoteIdFromSystemBar(this, noteId);//clearNotifications();

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

    private void retrieveOrBuildNoteAndShowNoteDetail(String noteId) {
        Note note = null;

        if (noteId == null) return;

        if (SimperiumUtils.getNotesBucket() != null) {
            try {
                note = SimperiumUtils.getNotesBucket().get(noteId);
            } catch (BucketObjectMissingException e) {
                SimperiumUtils.trackBucketObjectMissingWarning(e.getMessage(), noteId);
            }
        }

        //if note was not found in bucket, try building up the note from the PN payload
        if (note == null) {
            String base64FullData = getIntent().getStringExtra(NotificationsListFragment.NOTE_FULL_DATA_EXTRA);
            note = new Note.Schema().buildFromBase64EncodedData(noteId, base64FullData);
        }

        //if we still couldn't build it up just fail, nothing else to do here
        if (note == null) {
            showErrorToastAndFinish();
            return;
        } else {
            //show the note

            Map<String, String> properties = new HashMap<>();
            properties.put("notification_type", note.getType());
            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS, properties);

            Fragment detailFragment = getDetailFragmentForNote(note);
            getFragmentManager().beginTransaction()
                    .add(R.id.notifications_detail_container, detailFragment)
                    .commitAllowingStateLoss();

            //set title
            if (getSupportActionBar() != null) {
                String title = note.getTitle();
                if (TextUtils.isEmpty(title)) {
                    //set a deafult title if title is not set within the note
                    switch(note.getType()) {
                        case NOTE_FOLLOW_TYPE:
                            title = getString(R.string.follows);
                            break;
                        case NOTE_LIKE_TYPE:
                            title = getString(R.string.like);
                            break;
                        case NOTE_COMMENT_TYPE:
                            title = getString(R.string.comment);
                            break;
                    }
                }
                getSupportActionBar().setTitle(title);
            }

            // mark the note as read if it's unread
            if (note.isUnread()) {
                // mark as read which syncs with simperium
                note.markAsRead();
                EventBus.getDefault().post(new NotificationEvents.NotificationsChanged());
            }
        }
    }

    private void showErrorToastAndFinish() {
        AppLog.e(AppLog.T.NOTIFS, "Note could not be found.");
        ToastUtils.showToast(this, R.string.error_notification_open);
        finish();
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
            boolean isInstantLike = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_LIKE_EXTRA, false);
            boolean isInstantApprove = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_APPROVE_EXTRA, false);
            fragment = isInstantLike ?
                        CommentDetailFragment.newInstanceForInstantLike(note) :
                        isInstantApprove ?
                            CommentDetailFragment.newInstanceForInstantApprove(note) :
                            CommentDetailFragment.newInstance(note);
        } else if (note.isAutomattcherType()) {
            // show reader post detail for automattchers about posts - note that comment
            // automattchers are handled by note.isCommentType() above
            boolean isPost = (note.getSiteId() != 0 && note.getPostId() != 0 && note.getCommentId() == 0);
            if (isPost) {
                fragment = ReaderPostDetailFragment.newInstance(note.getSiteId(), note.getPostId());
            } else {
                fragment = NotificationsDetailListFragment.newInstance(note.getId());
            }
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
}
