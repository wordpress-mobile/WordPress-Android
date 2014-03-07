/**
 * Behaves much list a ListFragment
 */
package org.wordpress.android.ui.notifications;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Note;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;

public class NoteSingleLineListFragment extends ListFragment implements NotificationFragment {
    private Note mNote;
    
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
    }
    
    @Override
    public void onActivityCreated(Bundle bundle){
        super.onActivityCreated(bundle);
        
        ListView list = getListView();
        list.setDivider(getResources().getDrawable(R.drawable.list_divider));
        list.setDividerHeight(1);
        list.setHeaderDividersEnabled(false);

        Note currentNote = getNote();
        
        // No note? No service.
        if (currentNote == null)
            return;
        
        // set the header
        LayoutInflater inflater = getActivity().getLayoutInflater();
        DetailHeader noteHeader = (DetailHeader) inflater.inflate(R.layout.notifications_detail_header, null);
        noteHeader.setText(JSONUtil.getStringDecoded(currentNote.queryJSON("subject", new JSONObject()), "text"));
        noteHeader.setBackgroundColor(getResources().getColor(R.color.light_gray));
        noteHeader.getTextView().setGravity(Gravity.CENTER_HORIZONTAL);
        String footerUrl = currentNote.queryJSON("body.header_link", "");
        noteHeader.setNote(getNote(), footerUrl);

        if (getActivity() instanceof OnPostClickListener) {
            noteHeader.setOnPostClickListener(((OnPostClickListener)getActivity()));
        }
        if (getActivity() instanceof OnCommentClickListener) {
            noteHeader.setOnCommentClickListener(((OnCommentClickListener)getActivity()));
        }

        list.addHeaderView(noteHeader);
        
        // set the adapter
        setListAdapter(new NoteAdapter());
    }
    
    @Override
    public void setListAdapter(ListAdapter adapter) {
        super.setListAdapter(adapter);
    }
        
    @Override
    public void setNote(Note note){
        mNote = note;
    }
    
    @Override
    public Note getNote(){
        return mNote;
    }
    
    class NoteAdapter extends BaseAdapter {
        private final JSONArray mItems;
        NoteAdapter(){
            mItems = getNote().queryJSON("body.items", new JSONArray());
        }
        
        public View getView(int position, View cachedView, ViewGroup parent){
            View v;
            if (cachedView == null) {
                v = getActivity().getLayoutInflater().inflate(R.layout.notifications_follow_row, null);
            } else {
                v = cachedView;
            }
            JSONObject noteItem = getItem(position);
            JSONObject followAction = JSONUtil.queryJSON(noteItem, "action", new JSONObject());

            FollowRow row = (FollowRow) v;
            row.setFollowListener(new FollowListener());
            row.setAction(followAction);
            String headerText = JSONUtil.queryJSON(noteItem, "header_text", "");
            if (TextUtils.isEmpty(headerText)) {
                // reblog notifications don't have "header_text" but they do have "header" which
                // contains the user's name wrapped in a link, so strip the html to get the name
                headerText = HtmlUtils.fastStripHtml(JSONUtil.queryJSON(noteItem, "header", ""));
            }
            row.setNameText(headerText);
            row.getImageView().setImageUrl(JSONUtil.queryJSON(noteItem, "icon", ""), WordPress.imageLoader);
            
            return v;
        }
        // {"action":{"type":"follow","params":{"following-text":"Following","is_following":false,"following-hover-text":"Unfollow","blog_id":63708455,"blog_url":"http:\/\/madamesir.wordpress.com","blog_title":"Madame Sir","site_id":63708455,"stat-source":"note_reblog_post","follow-text":"Follow","blog_domain":"madamesir.wordpress.com"}},"icon_width":32,"icon_height":32,"icon":"https:\/\/1.gravatar.com\/avatar\/474a194639684ac63da9314aa816f520?s=256&d=https%3A%2F%2F1.gravatar.com%2Favatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D256&r=G","header":"<a href=\"http:\/\/madamesir.wordpress.com\" class=\"wpn-user-blog-link\" target=\"_blank\" notes-data-click=\"reblog_note_rebloggers_blog\">madamesir<\/a>"}
        public long getItemId(int position){
            return (long) position;
        }
        
        public JSONObject getItem(int position){
            return JSONUtil.queryJSON(mItems, String.format("[%d]", position), new JSONObject());
        }
        
        public int getCount(){
            return mItems.length();
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