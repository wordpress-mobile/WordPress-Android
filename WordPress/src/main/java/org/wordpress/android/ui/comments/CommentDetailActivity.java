package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

// simple wrapper activity for CommentDetailFragment
public class CommentDetailActivity extends Activity {

    public static final String KEY_COMMENT_DETAIL_LOCAL_TABLE_BLOG_ID = "local_table_blog_id";
    public static final String KEY_COMMENT_DETAIL_COMMENT_ID = "comment_detail_comment_id";
    public static final String KEY_COMMENT_DETAIL_NOTE_ID = "comment_detail_note_id";
    public static final String KEY_COMMENT_DETAIL_IS_REMOTE = "comment_detail_is_remote";

    private static final String TAG_COMMENT_DETAIL_FRAGMENT = "tag_comment_detail_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.comment_activity_detail);

        setTitle(R.string.comment);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            CommentDetailFragment commentDetailFragment = null;
            if (intent.getStringExtra(KEY_COMMENT_DETAIL_NOTE_ID) != null && SimperiumUtils.getNotesBucket() != null) {
                try {
                    Note note = SimperiumUtils.getNotesBucket().get(
                            intent.getStringExtra(KEY_COMMENT_DETAIL_NOTE_ID)
                    );

                    if (intent.hasExtra(KEY_COMMENT_DETAIL_IS_REMOTE)) {
                        commentDetailFragment = CommentDetailFragment.newInstanceForRemoteNoteComment(note);
                    } else {
                        commentDetailFragment = CommentDetailFragment.newInstance(note);
                    }
                } catch (BucketObjectMissingException e) {
                    AppLog.e(AppLog.T.NOTIFS, "CommentDetailActivity was passed an invalid note id.");
                }
            } else if (intent.getIntExtra(KEY_COMMENT_DETAIL_LOCAL_TABLE_BLOG_ID, 0) > 0
                    && intent.getLongExtra(KEY_COMMENT_DETAIL_COMMENT_ID, 0) > 0) {
                commentDetailFragment = CommentDetailFragment.newInstance(
                        intent.getIntExtra(KEY_COMMENT_DETAIL_LOCAL_TABLE_BLOG_ID, 0),
                        intent.getLongExtra(KEY_COMMENT_DETAIL_COMMENT_ID, 0)
                );
            }

            if (commentDetailFragment != null) {
                commentDetailFragment.setRetainInstance(true);
                getFragmentManager().beginTransaction()
                        .add(R.id.comment_detail_container, commentDetailFragment, TAG_COMMENT_DETAIL_FRAGMENT)
                        .commit();
            } else {
                ToastUtils.showToast(this, R.string.error_load_comment);
                finish();
            }
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
}
