package org.wordpress.android.ui.notifications;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.util.Log;

import org.wordpress.android.R;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

public class NotificationsListFragment extends ListFragment {
    private static String TAG="WPNotifications";
    private static final int[] IMAGE_IDS={ R.id.note_icon };
    private static final int LOAD_MORE_WITHIN_X_ROWS=5;
    private NoteProvider mNoteProvider;
    
    public interface NoteProvider {
        public void onRequestMoreNotifications(ListView listView, ListAdapter adapter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        ListView listView = getListView();
        listView.setOnScrollListener(new ListScrollListener());
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
    public void setNoteProvider(NoteProvider provider){
        mNoteProvider = provider;
    }
    protected void requestMoreNotifications(){
        if (mNoteProvider != null) {
            mNoteProvider.onRequestMoreNotifications(getListView(), getListAdapter());
        }
    }
    
    private class ListScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount){
            // ask for more if we're close to the end of the list
            Log.d(TAG, String.format("First: %d Count: %d Total: %d", firstVisibleItem, visibleItemCount, totalItemCount));
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