package org.wordpress.android.ui.notifications.blocks;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.NotificationUtils;
import org.wordpress.android.ui.reader.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.TypefaceCache;
import org.wordpress.android.widgets.WPTextView;

/**
 * A block that displays a parent comment to a comment reply notification
 * Very similar to a NoteBlock, but with different styling in the layout xml
 */
public class CommentReplyNoteBlock extends NoteBlock {

    public CommentReplyNoteBlock(JSONObject noteObject, OnNoteBlockTextClickListener onNoteBlockTextClickListener) {
        super(noteObject, onNoteBlockTextClickListener);
    }

    @Override
    public int getLayoutResourceId() {
        return R.layout.note_block_comment_reply;
    }


}
