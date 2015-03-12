package org.wordpress.mediapicker.source;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.mediapicker.MediaItem;

import java.util.List;

/**
 * MediaSource's are used to gather {@link org.wordpress.mediapicker.MediaItem}'s and create
 * {@link android.view.View}'s for displaying them.
 */

public interface MediaSource extends Parcelable {
    /**
     * Interface offered for any class to implement a listener for data set changes.
     */
    public interface OnMediaChange {
        /**
         * To be called if Media is being loaded in the background. Listener may choose to display a
         * progress indicator or alert the user in some fashion.
         *
         * @param complete
         *  true if loading is finished, otherwise false
         */
        public void onMediaLoading(MediaSource source, boolean complete);

        /**
         * To be called when new MediaItems have been added to the source.
         *
         * @param source
         *  the host source
         * @param addedItems
         *  the newly added {@link org.wordpress.mediapicker.MediaItem}'s
         */
        public void onMediaAdded(MediaSource source, List<MediaItem> addedItems);

        /**
         * To be called when existing {@link org.wordpress.mediapicker.MediaItem}'s are removed.
         *
         * @param source
         *  the host source
         * @param removedItems
         *  the removed {@link org.wordpress.mediapicker.MediaItem}'s
         */
        public void onMediaRemoved(MediaSource source, List<MediaItem> removedItems);

        /**
         * To be called when existing {@link org.wordpress.mediapicker.MediaItem}'s are modified.
         *
         * @param source
         *  the host source
         * @param changedItems
         *  the changed {@link org.wordpress.mediapicker.MediaItem}'s
         */
        public void onMediaChanged(MediaSource source, List<MediaItem> changedItems);
    }

    // Load MediaItem data
    public void gather(final OnMediaLoaded callback);
    // Destroy MediaItem data
    public void cleanup();
    // Can be ignored if no listener is needed
    public void setListener(final OnMediaChange listener);
    // Get the number of MediaItems accessible through the source
    public int getCount();
    // Get the MediaItem at the specified position
    public MediaItem getMedia(int position);
    // Get the View to display the MediaItem at the specified position TODO: remove cache from here
    public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, ImageLoader.ImageCache cache);
    // Callback when MediaItem is selected; return true if handled internally, false to propagate
    public boolean onMediaItemSelected(final MediaItem mediaItem, boolean selected);
}
