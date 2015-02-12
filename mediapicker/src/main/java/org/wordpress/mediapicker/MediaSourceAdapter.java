package org.wordpress.mediapicker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.mediapicker.source.MediaSource;

import java.util.List;

/**
 * An {@link android.widget.Adapter} that creates {@link android.view.View}'s for
 * {@link org.wordpress.mediapicker.MediaItem}'s provided by
 * {@link org.wordpress.mediapicker.source.MediaSource}'s.
 *
 * The following resources are provided for convenience to modify the default layout:
 * <ul>
 *     <li>@drawable/camera                    : overlay image for image capture sources</li>
 *     <li>@drawable/video                     : overlay image for video capture sources</li>
 *     <li>@drawable/media_item_frame_selector : defines frame background based on check state</li>
 *     <li>@dimen/media_item_height            : height of the entire MediaItem view</li>
 *     <li>@dimen/media_item_frame_margin_*    : frame margins; * = left/top/right/bottom</li>
 *     <li>@dimen/media_item_frame_padding_*   : frame padding; * = left/top/right/bottom</li>
 * </ul>
 */

public class MediaSourceAdapter extends BaseAdapter implements MediaSource.OnMediaChange {
    private final LayoutInflater  mLayoutInflater;
    private final ImageLoader.ImageCache mImageCache;
    private final List<MediaSource> mMediaSources;

    public MediaSourceAdapter(Context context, List<MediaSource> sources, ImageLoader.ImageCache imageCache) {
        mMediaSources = sources;
        mLayoutInflater = LayoutInflater.from(context);
        mImageCache = imageCache;

        for (MediaSource source : mMediaSources) {
            if (source != null) {
                source.setListener(this);
            }
        }
    }

    @Override
    public int getViewTypeCount() {
        return mMediaSources.size() == 0 ? 1 : mMediaSources.size();
    }

    @Override
    public int getCount() {
        return totalItems();
    }

    @Override
    public int getItemViewType(int position) {
        return mMediaSources.indexOf(sourceAtPosition(position));
    }

    @Override
    public MediaItem getItem(int position) {
        MediaSource sourceAtPosition = sourceAtPosition(position);

        return sourceAtPosition != null ? sourceAtPosition.getMedia(position - offsetAtPosition(position)) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MediaSource itemSource = sourceAtPosition(position);

        if (itemSource != null) {
            return itemSource.getView(position - offsetAtPosition(position), convertView, parent, mLayoutInflater, mImageCache);
        }

        return null;
    }

    /**
     * Helper method; determines the total number of items in all MediaSources
     *
     * @return
     *  the total number of MediaItems contained in the MediaSource
     */
    private int totalItems() {
        int count = 0;
        for (int i = 0; i < mMediaSources.size(); ++i) {
            count += mMediaSources.get(i).getCount();
        }

        return count;
    }

    /**
     * Determines the MediaSource that contains the MediaItem at the specified position.
     *
     * @param position
     *  the absolute position in the list of all MediaItems
     * @return
     *  the parent MediaSource for the MediaItem at the specified position
     */
    public MediaSource sourceAtPosition(int position) {
        int count = 0;
        for (int i = 0; i < mMediaSources.size(); ++i) {
            count += mMediaSources.get(i).getCount();

            if (position < count) {
                return mMediaSources.get(i);
            }
        }

        return null;
    }

    /**
     * Helper method; determines offset into individual MediaSource
     *
     * @param position
     *  position of item within all MediaSources
     * @return
     *  position of item within its parent MediaSource
     */
    private int offsetAtPosition(int position) {
        int offset = 0;
        for (int i = 0; i < mMediaSources.size(); ++i) {
            if (position < (offset + mMediaSources.get(i).getCount())) {
                return offset;
            }

            offset += mMediaSources.get(i).getCount();
        }

        return offset;
    }

    @Override
    public void onMediaAdded(MediaSource source, List<MediaItem> addedItems) {
        notifyDataSetChanged();
    }

    @Override
    public void onMediaRemoved(MediaSource source, List<MediaItem> removedItems) {
        notifyDataSetChanged();
    }

    @Override
    public void onMediaChanged(MediaSource source, List<MediaItem> changedItems) {
        notifyDataSetChanged();
    }
}
