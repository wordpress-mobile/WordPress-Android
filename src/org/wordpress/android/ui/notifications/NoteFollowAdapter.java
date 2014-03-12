package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.NotificationUtils.NoteUpdatedListener;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

import java.lang.ref.WeakReference;

/**
 * Created by nbradbury on 3/12/14.
 */
public class NoteFollowAdapter extends BaseAdapter implements NoteUpdatedListener {
    private JSONArray mItems;
    private Note mNote;
    private final int mAvatarSz;
    private final WeakReference<Context> mWeakContext;
    private final LayoutInflater mInflater;

    NoteFollowAdapter(Context context, Note note) {
        mWeakContext = new WeakReference<Context>(context);
        mInflater = LayoutInflater.from(context);
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        setNote(note);
        // get the latest version of this note to ensure follow statuses are correct
        NotificationUtils.updateNotification(getNoteId(), this);
    }

    /*
     * fired by NotificationUtils.updateNotification() when this note has been updated
     */
    @Override
    public void onNoteUpdated(int noteId) {
        if (!hasContext())
            return;
        setNote(WordPress.wpDB.getNoteById(noteId));
        notifyDataSetChanged();
    }

    private boolean hasContext() {
        return (mWeakContext.get() != null);
    }

    private void setNote(Note note) {
        mNote = note;
        if (mNote != null) {
            mItems = mNote.queryJSON("body.items", new JSONArray());
        } else {
            mItems = new JSONArray();
        }
    }

    public View getView(int position, View cachedView, ViewGroup parent){
        View view;
        if (cachedView == null) {
            view = mInflater.inflate(R.layout.notifications_follow_row, null);
        } else {
            view = cachedView;
        }

        JSONObject noteItem = getItem(position);
        JSONObject followAction = JSONUtil.queryJSON(noteItem, "action", new JSONObject());

        FollowRow row = (FollowRow) view;
        row.setFollowListener(new FollowListener());
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
