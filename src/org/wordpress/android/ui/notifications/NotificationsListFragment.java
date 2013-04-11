package org.wordpress.android.ui.notifications;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.widget.ListAdapter;

import org.wordpress.android.R;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

public class NotificationsListFragment extends ListFragment {
    private static final int[] IMAGE_IDS={ R.id.note_icon };
    @Override
    public void setListAdapter(ListAdapter adapter){
        // Wrap the adapter in the thumbnail adapter
        ThumbnailBus bus = new ThumbnailBus();
        SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> cache;
        cache = new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(null, null, 101, bus);
        ThumbnailAdapter thumbAdapter = new ThumbnailAdapter( getActivity(), adapter, cache, IMAGE_IDS);
        super.setListAdapter(thumbAdapter);

    }
    
}