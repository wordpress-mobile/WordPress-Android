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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.support.v4.app.ListFragment;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

import static org.wordpress.android.WordPress.*;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

class NotificationDetailFragment extends ListFragment implements NotificationFragment {
    public static final String TAG="NoteDetail";
    public static final String NOTE_ID_ARGUMENT="note_id";
    public static final String NOTE_JSON_ARGUMENT="note_json";
    
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
        LinearLayout noteHeader = (LinearLayout) inflater.inflate(R.layout.notifications_detail_header, null);
        // LinearLayout noteFooter = (LinearLayout) inflater.inflate(R.layout.notifications_detail_footer, null);
        ListView list = getListView();
        list.addHeaderView(noteHeader);
        // list.addFooterView(noteFooter);
        // set the adapter
        setListAdapter(new NoteAdapter());
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
            return v;
        }
        
        public long getItemId(int position){
            return (long) 0;
        }
        
        public JSONObject getItem(int position){
            return new JSONObject();
        }
        
        public int getCount(){
            return mItems.length();
        }
    }
}