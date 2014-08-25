package org.wordpress.android.ui.notifications;

import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.blocks.NoteBlock;
import org.wordpress.android.ui.notifications.blocks.UserNoteBlock;
import org.wordpress.android.util.JSONUtil;

// Note header, displayed at top of detail view
public class HeaderUserNoteBlock extends NoteBlock {

    private JSONArray mHeaderArray;

    private UserNoteBlock.OnGravatarClickedListener mGravatarClickedListener;

    public HeaderUserNoteBlock(JSONArray headerArray, UserNoteBlock.OnGravatarClickedListener onGravatarClickedListener) {
        super(new JSONObject(), null);

        mHeaderArray = headerArray;
        mGravatarClickedListener = onGravatarClickedListener;
    }

    public int getLayoutResourceId() {
        return R.layout.note_block_user_header;
    }

    @Override
    public View configureView(View view) {
        final NoteHeaderBlockHolder noteBlockHolder = (NoteHeaderBlockHolder)view.getTag();

        noteBlockHolder.nameTextView.setText(getUserName());
        noteBlockHolder.avatarImageView.setImageUrl(getAvatarUrl(), WordPress.imageLoader);
        if (!TextUtils.isEmpty(getUserUrl())) {
            noteBlockHolder.avatarImageView.setOnTouchListener(mOnGravatarTouchListener);
        } else {
            noteBlockHolder.avatarImageView.setOnTouchListener(null);
        }

        noteBlockHolder.snippetTextView.setText(getSnippet());

        return view;
    }

    private String getUserName() {
        return JSONUtil.queryJSON(mHeaderArray, "[0].text", "");
    }

    private String getAvatarUrl() {
        return JSONUtil.queryJSON(mHeaderArray, "[0].media[0].url", "");
    }

    private String getUserUrl() {
        return JSONUtil.queryJSON(mHeaderArray, "[0].ids[0].url", "");
    }

    private String getSnippet() {
        return JSONUtil.queryJSON(mHeaderArray, "[1].text", "");
    }

    @Override
    public Object getViewHolder(View view) {
        return new NoteHeaderBlockHolder(view);
    }

    private class NoteHeaderBlockHolder {
        private TextView nameTextView;
        private TextView snippetTextView;
        private NetworkImageView avatarImageView;

        public NoteHeaderBlockHolder(View view) {
            nameTextView = (TextView)view.findViewById(R.id.header_user);
            snippetTextView = (TextView)view.findViewById(R.id.header_snippet);
            avatarImageView = (NetworkImageView)view.findViewById(R.id.header_avatar);
        }
    }

    private View.OnTouchListener mOnGravatarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int animationDuration = 150;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .alpha(0.5f)
                        .setDuration(animationDuration)
                        .setInterpolator(new DecelerateInterpolator());
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(animationDuration)
                        .setInterpolator(new DecelerateInterpolator());

                if (event.getActionMasked() == MotionEvent.ACTION_UP && mGravatarClickedListener != null) {
                    // Fire the listener, which will load the site preview for the user's site
                    // In the future we can use this to load a 'profile view' (currently in R&D)
                    long siteId = Long.valueOf(JSONUtil.queryJSON(mHeaderArray, "[0].ids[0].site_id", 0));
                    long userId = Long.valueOf(JSONUtil.queryJSON(mHeaderArray, "[0].ids[0].id", 0));
                    if (mGravatarClickedListener != null && siteId > 0 && userId > 0) {
                        mGravatarClickedListener.onGravatarClicked(siteId, userId);
                    }
                }
            }

            return true;
        }
    };

}
