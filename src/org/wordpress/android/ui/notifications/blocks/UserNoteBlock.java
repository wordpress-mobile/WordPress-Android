package org.wordpress.android.ui.notifications.blocks;

import android.view.View;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPTextView;

/**
 * A block that displays information about a User (such as a user that liked a post)
 * Will display an action button if available (e.g. follow button)
 */
public class UserNoteBlock extends NoteBlock {

    public UserNoteBlock(JSONObject noteObject) {
        super(noteObject, null);
    }

    @Override
    public BlockType getBlockType() {
        return BlockType.USER;
    }

    @Override
    public int getLayoutResourceId() {
        return R.layout.note_block_user;
    }

    @Override
    public View configureView(View view) {
        UserActionNoteBlockHolder noteBlockHolder = (UserActionNoteBlockHolder)view.getTag();
        noteBlockHolder.mNameTextView.setText(getNoteText());
        noteBlockHolder.mUrlTextView.setText(StringUtils.notNullStr(getUserUrl()));
        if (hasImageMediaItem()) {
            noteBlockHolder.mAvatarImageView.setImageUrl(getNoteMediaItem().optString("url", ""), WordPress.imageLoader);
        }

        if (hasAction()) {
            configureActionButton(noteBlockHolder.mActionButton);
            noteBlockHolder.mActionButton.setVisibility(View.VISIBLE);
        } else {
            noteBlockHolder.mActionButton.setVisibility(View.GONE);
        }

        return view;
    }

    private void configureActionButton(WPTextView actionButton) {
        // For now we will support the follow action
        try {
            boolean isFollowing = getNoteData().getJSONObject("actions").getBoolean("follow");
            ReaderUtils.showFollowStatus(actionButton, isFollowing);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.NOTIFS, "Unexpected action button value: " + e.getMessage());
        }
    }

    @Override
    public Object getViewHolder(View view) {
        return new UserActionNoteBlockHolder(view);
    }

    private class UserActionNoteBlockHolder {
        private WPTextView mNameTextView;
        private WPTextView mUrlTextView;
        private WPTextView mActionButton;
        private NetworkImageView mAvatarImageView;

        public UserActionNoteBlockHolder(View view) {
            mNameTextView = (WPTextView) view.findViewById(R.id.name);
            mUrlTextView = (WPTextView) view.findViewById(R.id.url);
            mActionButton = (WPTextView) view.findViewById(R.id.action_button);
            mActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext(), "Sweet", Toast.LENGTH_SHORT).show();
                }
            });
            mAvatarImageView = (NetworkImageView) view.findViewById(R.id.avatar);
        }
    }

    public String getUserUrl() {
        if (getNoteData() == null) return null;

        JSONArray idsArray = getNoteData().optJSONArray("ids");
        if (idsArray != null) {
            for (int i=0; i < idsArray.length(); i++) {
                try {
                    JSONObject idObject = idsArray.getJSONObject(i);
                    if (idObject.has("url")) {
                        return UrlUtils.removeUrlScheme(idObject.getString("url"));
                    }
                } catch (JSONException e) {
                    AppLog.i(AppLog.T.NOTIFS, "Unexpected object in notifications ids array.");
                }
            }
        }

        return null;
    }

    // Show or hide action button
    private boolean hasAction() {
        if (getNoteData() == null) {
            return false;
        }

        return getNoteData().has("actions");
    }
}
