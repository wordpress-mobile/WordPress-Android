/**
 * Behaves much list a ListFragment
 */
package org.wordpress.android.ui.notifications;

import android.util.Log;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.support.v4.app.ListFragment;
import android.net.Uri;
import android.content.Intent;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

import static org.wordpress.android.WordPress.*;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

class SingleLineListFragment extends ListFragment implements NotificationFragment {
    public static final String TAG="NoteDetail";
    public static final String NOTE_ID_ARGUMENT="note_id";
    public static final String NOTE_JSON_ARGUMENT="note_json";
    private static final int[] IMAGE_IDS={ R.id.avatar };
    
    protected Note mNote;
    
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
    }
    
    @Override
    public void onActivityCreated(Bundle bundle){
        super.onActivityCreated(bundle);
        // set the header
        LayoutInflater inflater = getActivity().getLayoutInflater();
        DetailHeader noteHeader = (DetailHeader) inflater.inflate(R.layout.notifications_detail_header, null);
        noteHeader.setText(getNote().getSubject());
        final String url = getNote().queryJSON("body.header_link", "");
        noteHeader.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v){
               Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
               startActivity(intent);
           }
        });
        // LinearLayout noteFooter = (LinearLayout) inflater.inflate(R.layout.notifications_detail_footer, null);
        ListView list = getListView();
        list.addHeaderView(noteHeader);
        // list.addFooterView(noteFooter);
        // set the adapter
        setListAdapter(new NoteAdapter());
    }
    
    @Override
    public void setListAdapter(ListAdapter adapter) {
        // Wrap the adapter in the thumbnail adapter
        ThumbnailBus bus = new ThumbnailBus();
        SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> cache;
        cache = new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(null, null, 101, bus);
        ThumbnailAdapter thumbAdapter = new ThumbnailAdapter( getActivity(), adapter, cache, IMAGE_IDS);
        super.setListAdapter(thumbAdapter);
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
        private JSONArray mItems;
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
            JSONObject followAction = Note.queryJSON(noteItem, "action", new JSONObject());
            FollowRow row = (FollowRow) v;
            row.setAction(followAction);
            row.setText(Note.queryJSON(noteItem, "header_text", ""));
            row.getImageView().setTag(Note.queryJSON(noteItem, "icon", ""));
            return v;
        }
        
        public long getItemId(int position){
            return (long) position;
        }
        
        public JSONObject getItem(int position){
            return Note.queryJSON(mItems, String.format("[%d]", position), new JSONObject());
        }
        
        public int getCount(){
            return mItems.length();
        }
    }
}