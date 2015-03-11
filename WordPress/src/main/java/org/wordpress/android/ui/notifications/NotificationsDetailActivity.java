package org.wordpress.android.ui.notifications;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.WindowManager;

import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailActivity;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

public class NotificationsDetailActivity extends ActionBarActivity implements
        CommentActions.OnNoteCommentActionListener {
    private static final String ARG_TITLE = "activityTitle";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.i(AppLog.T.NOTIFS, "Creating NotificationsDetailActivity");

        setContentView(R.layout.notifications_detail_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0.0f);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            String noteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
            if (noteId == null) {
                ToastUtils.showToast(this, R.string.error_notification_open);
                return;
            }

            if (SimperiumUtils.getNotesBucket() != null) {
                try {
                    Note note = SimperiumUtils.getNotesBucket().get(noteId);

                    // mark the note as read if it's unread
                    if (note.isUnread()) {
                        // mark as read which syncs with simperium
                        note.markAsRead();
                    }

                    Fragment detailFragment = getDetailFragmentForNote(note);
                    getFragmentManager().beginTransaction()
                            .add(R.id.notifications_detail_container, detailFragment)
                            .commit();

                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(note.getTitle());
                    }
                } catch (BucketObjectMissingException e) {
                    AppLog.e(AppLog.T.NOTIFS, "Note could not be found.");
                    ToastUtils.showToast(this, R.string.error_notification_open);
                }
            }
        } else if (savedInstanceState.containsKey(ARG_TITLE) && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(StringUtils.notNullStr(savedInstanceState.getString(ARG_TITLE)));
        }

        // Hide the keyboard, unless we arrived here from the 'Reply' action in a push notification
        if (!getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false)) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

        GCMIntentService.clearNotificationsMap();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
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
            fragment = CommentDetailFragment.newInstance(note.getId());
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

    public void showCommentDetailForNote(Note note) {
        if (isFinishing() || note == null) return;

        Intent intent = new Intent(this, CommentDetailActivity.class);
        intent.putExtra(CommentDetailActivity.KEY_COMMENT_DETAIL_IS_REMOTE, true);
        intent.putExtra(CommentDetailActivity.KEY_COMMENT_DETAIL_NOTE_ID, note.getId());
        startActivity(intent);
    }

    public void showBlogPreviewActivity(long siteId) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderBlogPreview(this, siteId);
    }

    public void showPostActivity(long siteId, long postId) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderPostDetail(this, siteId, postId);
    }

    public void showStatsActivityForSite(int localTableSiteId) {
        if (isFinishing()) return;

        Intent intent = new Intent(this, StatsActivity.class);
        intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableSiteId);
        intent.putExtra(StatsActivity.ARG_NO_MENU_DRAWER, true);
        startActivity(intent);
    }

    public void showWebViewActivityForUrl(String url) {
        if (isFinishing() || url == null) return;

        WPWebViewActivity.openURL(this, url);
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
