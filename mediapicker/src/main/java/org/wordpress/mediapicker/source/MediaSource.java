package org.wordpress.mediapicker.source;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.mediapicker.MediaItem;

/**
 * MediaSource's are used to gather {@link org.wordpress.mediapicker.MediaItem}'s and create
 * {@link android.view.View}'s for displaying them.
 */

public interface MediaSource extends Parcelable {
    // Get the number of MediaItems accessible through the source
    public int getCount();
    // Get the MediaItem at the specified position
    public MediaItem getMedia(int position);
    // Get the View to display the MediaItem at the specified position TODO: remove cache from here
    public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, ImageLoader.ImageCache cache);
    // Callback when MediaItem is selected; return true if handled internally, false to propagate
    public boolean onMediaItemSelected(final MediaItem mediaItem, boolean selected);
}
