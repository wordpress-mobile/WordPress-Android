/**
 * Behaves much list a ListFragment
 */
package org.wordpress.android.ui.notifications;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
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
import org.wordpress.android.util.JSONUtil;

public class NoteCommentLikeFragment extends ListFragment implements NotificationFragment {
    public static final String TAG="NoteDetail";

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

        // No note? No service.
        if (getNote() == null)
            return;

        // set the header
        LayoutInflater inflater = getActivity().getLayoutInflater();
        DetailHeader noteHeader = (DetailHeader) inflater.inflate(R.layout.notifications_detail_header, null);
        noteHeader.setText(getHeaderText());
        noteHeader.setBackgroundColor(getResources().getColor(R.color.light_gray));
        noteHeader.getTextView().setGravity(Gravity.CENTER_HORIZONTAL);
        noteHeader.setClickable(false);
        list.addHeaderView(noteHeader);
        
        // set the footer
        DetailHeader noteFooter = (DetailHeader) inflater.inflate(R.layout.notifications_detail_header, null);        
        JSONObject bodyObject =  getNote().queryJSON("body", new JSONObject());
        String footerText = getFooterText();
        if (!footerText.equals("")) {
            noteFooter.setText(footerText);
            String footerUrl = JSONUtil.getString(bodyObject, "header_link");
            if (!footerUrl.equals("")) {
                noteFooter.setUrl(footerUrl);
            }
            list.addFooterView(noteFooter);
        }
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
    
    private String getHeaderText() {
        JSONArray mItems = getNote().queryJSON("body.items", new JSONArray());
        JSONObject noteItem = JSONUtil.queryJSON(mItems, String.format("[%d]", 0), new JSONObject());
        return JSONUtil.getStringDecoded(noteItem, "header_text");
    }
    
    private String getFooterText() {
        JSONArray mItems = getNote().queryJSON("body.items", new JSONArray());
        JSONObject noteItem = JSONUtil.queryJSON(mItems, String.format("[%d]", 0), new JSONObject());
        return JSONUtil.getStringDecoded(noteItem, "html");
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
            JSONObject noteItem = getItem(position+1); //This is because element at position 0 of body.items must be discarded.
            JSONObject followAction = JSONUtil.queryJSON(noteItem, "action", new JSONObject());
            FollowRow row = (FollowRow) v;
            row.setListener(new FollowListener(getActivity().getApplicationContext()));
            row.setAction(followAction);
            row.setText(JSONUtil.queryJSON(noteItem, "header_text", ""));
            row.getImageView().setImageUrl(JSONUtil.queryJSON(noteItem, "icon", ""), WordPress.imageLoader);
            
            return v;
        }
        
        public long getItemId(int position){
            return (long) position;
        }
        
        public JSONObject getItem(int position){
            return JSONUtil.queryJSON(mItems, String.format("[%d]", position), new JSONObject());
        }
        
        public int getCount(){
            return mItems.length()-1; //Element at position 0 of body.items must be discarded.
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