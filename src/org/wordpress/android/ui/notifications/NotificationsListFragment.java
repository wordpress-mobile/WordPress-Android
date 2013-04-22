package org.wordpress.android.ui.notifications;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.util.Log;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

public class NotificationsListFragment extends ListFragment {
    private static String TAG="WPNotifications";
    private static final int[] IMAGE_IDS={ R.id.note_icon };
    private static final int LOAD_MORE_WITHIN_X_ROWS=5;
    private NoteProvider mNoteProvider;
    private NotesAdapter mNotesAdapter;
    private OnNoteClickListener mNoteClickListener;
    /**
     * For responding to tapping of notes
     */
    public interface OnNoteClickListener {
        public void onClickNote(Note note);
    }
    /**
     * For providing more notes data when getting to the end of the list
     */
    public interface NoteProvider {
        public void onRequestMoreNotifications(ListView listView, ListAdapter adapter);
    }
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        // setup the initial notes adapter
        mNotesAdapter = new NotesAdapter();
    }
    @Override
    public void onActivityCreated(Bundle bundle){
        super.onActivityCreated(bundle);
        ListView listView = getListView();
        listView.setOnScrollListener(new ListScrollListener());
        View progress = View.inflate(getActivity(), R.layout.list_footer_progress, null);
        listView.addFooterView(progress, null, false);
        setListAdapter(mNotesAdapter);
    }
    @Override
    public void onListItemClick (ListView l, View v, int position, long id){
        Note note = mNotesAdapter.getItem(position);
        if (note != null && mNoteClickListener != null) {
            mNoteClickListener.onClickNote(note);
        }
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
    public void setNotesAdapter(NotesAdapter adapter){
        mNotesAdapter = adapter;
        this.setListAdapter(adapter);
    }
    public NotesAdapter getNotesAdapter(){
        return mNotesAdapter;
    }
    public void setNoteProvider(NoteProvider provider){
        mNoteProvider = provider;
    }
    public void setOnNoteClickListener(OnNoteClickListener listener){
        mNoteClickListener = listener;
    }
    protected void requestMoreNotifications(){
        if (mNoteProvider != null) {
            mNoteProvider.onRequestMoreNotifications(getListView(), getListAdapter());
        }
    }

    class NotesAdapter extends ArrayAdapter<Note> {
        NotesAdapter(){
            this(getActivity());
        }
        NotesAdapter(Context context){
            this(context, new ArrayList<Note>());
        }
        NotesAdapter(Context context, List<Note> notes){
            super(context, R.layout.note_list_item, R.id.note_label, notes);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View view = super.getView(position, convertView, parent);
            final Note note = getItem(position);
            TextView detailText = (TextView) view.findViewById(R.id.note_detail);
            if (note.isCommentType()) {
                detailText.setText(note.getCommentPreview());
                detailText.setVisibility(View.VISIBLE);
            } else {
                detailText.setVisibility(View.GONE);
            }
            final ImageView iconView = (ImageView) view.findViewById(R.id.note_icon);
            iconView.setImageResource(R.drawable.placeholder);
            iconView.setTag(note.getIconURL());
            return view;
        }
        public Note getLastNote(){
            return getItem(getCount()-1);
        }
        public void addAll(List<Note> notes){
            Iterator<Note> noteIterator = notes.iterator();
            while(noteIterator.hasNext()){
                add(noteIterator.next());
            }
        }
    }

    private class ListScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount){
            // if we're within 5 from the last item we should ask for more items
            if (firstVisibleItem + visibleItemCount >= totalItemCount - LOAD_MORE_WITHIN_X_ROWS) {
                requestMoreNotifications();
            }
        }
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState){
        }
    }
    
}