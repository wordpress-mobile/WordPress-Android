package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

import java.lang.ref.WeakReference;

/**
 * Adapter used by NoteSingleLineListFragment and
 * NoteCommentLikeFragment to display list of liking/following users which enables
 * following/unfollowing each of them
 */
public class NoteFollowAdapter extends BaseAdapter {
    private JSONArray mItems;
    private Note mNote;
    private final boolean mDiscardFirstItem;
    private final int mAvatarSz;
    private final WeakReference<Context> mWeakContext;
    private final LayoutInflater mInflater;

    NoteFollowAdapter(Context context, Note note, boolean discardFirstItem) {
        mWeakContext = new WeakReference<Context>(context);
        mInflater = LayoutInflater.from(context);
        mDiscardFirstItem = discardFirstItem;
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);

        setNote(note);

        // request the latest version of this note to ensure follow statuses are correct
        //NotificationUtils.updateNotification(getNoteId(), this);
    }

    /*
     * fired by NotificationUtils.updateNotification() when this note has been updated
     */
    /*@Override
    public void onNoteUpdated(int noteId) {
        if (hasContext())
            setNote(WordPress.wpDB.getNoteById(noteId));
    }*/

    private boolean hasContext() {
        return (mWeakContext.get() != null);
    }

    private void setNote(Note note) {
        boolean hasItems = (mItems != null);

        mNote = note;

        final JSONArray items;
        if (mNote != null) {
            items = mNote.queryJSON("body.items", new JSONArray());
        } else {
            items = new JSONArray();
        }

        // the first body item in comment likes is the header ("This person liked your comment")
        // and should be discarded
        if (mDiscardFirstItem && items.length() > 0) {
            // can't use mItems.remove(0) since it requires API 19
            mItems = new JSONArray();
            for (int i = 1; i < items.length(); i++) {
                try {
                    mItems.put(items.get(i));
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.NOTIFS, e);
                }
            }
        } else {
            mItems = items;
        }

        // if the adapter had existing items, make sure the changes are reflected
        if (hasItems) {
            notifyDataSetChanged();
        }
    }

    public View getView(int position, View cachedView, ViewGroup parent){
        View view;
        if (cachedView == null) {
            view = mInflater.inflate(R.layout.note_block_user, null);
        } else {
            view = cachedView;
        }

        JSONObject noteItem = getItem(position);
        JSONObject followAction = JSONUtil.queryJSON(noteItem, "action", new JSONObject());

        FollowRow row = (FollowRow) view;
        row.setFollowListener(new FollowListener(getNoteId()));
        row.setAction(followAction);

        String headerText = JSONUtil.queryJSON(noteItem, "header_text", "");
        if (TextUtils.isEmpty(headerText)) {
            // reblog notifications don't have "header_text" but they do have "header" which
            // contains the user's name wrapped in a link, so strip the html to get the name
            headerText = HtmlUtils.fastStripHtml(JSONUtil.queryJSON(noteItem, "header", ""));
        }
        row.setNameText(headerText);

        String iconUrl = JSONUtil.queryJSON(noteItem, "icon", "");
        row.getImageView().setImageUrl(PhotonUtils.fixAvatar(iconUrl, mAvatarSz), WordPress.imageLoader);

        return view;
    }

    public long getItemId(int position){
        return position;
    }

    public JSONObject getItem(int position){
        return JSONUtil.queryJSON(mItems, String.format("[%d]", position), new JSONObject());
    }

    public int getCount(){
        return (mItems != null ? mItems.length() : 0);
    }

    private int getNoteId() {
        if (mNote == null)
            return 0;
        return StringUtils.stringToInt(mNote.getId());
    }
}
