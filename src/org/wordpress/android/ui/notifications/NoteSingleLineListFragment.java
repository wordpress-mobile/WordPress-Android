/**
 * Behaves much list a ListFragment
 */
package org.wordpress.android.ui.notifications;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

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

public class NoteSingleLineListFragment extends ListFragment implements NotificationFragment, NoteUpdatedListener {
    private Note mNote;
    private int mAvatarSz;
    
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        mAvatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notifications_follow_list, container, false);
    }
    
    @Override
    public void onActivityCreated(Bundle bundle){
        super.onActivityCreated(bundle);
        
        ListView list = getListView();
        list.setDivider(getResources().getDrawable(R.drawable.list_divider));
        list.setDividerHeight(1);
        list.setHeaderDividersEnabled(false);

        // No note? No service.
        if (getNote() == null)
            return;
        
        // set the header
        final DetailHeader noteHeader = (DetailHeader) getView().findViewById(R.id.header);
        noteHeader.setText(JSONUtil.getStringDecoded(getNote().queryJSON("subject", new JSONObject()), "text"));
        String footerUrl = getNote().queryJSON("body.header_link", "");
        noteHeader.setNote(getNote(), footerUrl);

        if (getActivity() instanceof OnPostClickListener) {
            noteHeader.setOnPostClickListener(((OnPostClickListener)getActivity()));
        }
        if (getActivity() instanceof OnCommentClickListener) {
            noteHeader.setOnCommentClickListener(((OnCommentClickListener)getActivity()));
        }

        // set the adapter
        setListAdapter(new NoteAdapter(getActivity()));

        // get the latest version of this note to ensure follow statuses are correct
        NotificationUtils.updateNotification(getNoteId(), this);
    }

    /*
     * fired by NotificationUtils.updateNotification() when this note has been updated
     */
    @Override
    public void onNoteUpdated(int noteId) {
        if (getActivity() == null)
            return;
        setNote(WordPress.wpDB.getNoteById(noteId));
        ((NoteAdapter)getListAdapter()).refresh();
    }

    @Override
    public void setNote(Note note){
        mNote = note;
    }
    
    @Override
    public Note getNote(){
        return mNote;
    }

    private int getNoteId() {
        if (mNote == null)
            return 0;
        return StringUtils.stringToInt(mNote.getId());
    }

    class NoteAdapter extends BaseAdapter {
        private JSONArray mItems;
        private final LayoutInflater mInflater;

        NoteAdapter(Context context){
            mItems = getNote().queryJSON("body.items", new JSONArray());
            mInflater = LayoutInflater.from(context);
        }

        private void refresh() {
            mItems = getNote().queryJSON("body.items", new JSONArray());
            notifyDataSetChanged();
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
            return (long) position;
        }
        
        public JSONObject getItem(int position){
            return JSONUtil.queryJSON(mItems, String.format("[%d]", position), new JSONObject());
        }
        
        public int getCount(){
            return (mItems != null ? mItems.length() : 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }
}