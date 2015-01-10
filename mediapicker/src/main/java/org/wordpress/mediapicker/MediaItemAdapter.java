package org.wordpress.mediapicker;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.mediapicker.source.MediaSourceCaptureImage;
import org.wordpress.mediapicker.source.MediaSourceCaptureVideo;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link android.widget.Adapter} to convert {@link org.wordpress.mediapicker.MediaItem}'s to
 * {@link android.view.View}'s.
 *
 * Customizing the MediaItem view:
 *
 * There are two ways to modify the layout of MediaItem views, either by overriding the
 * media_item_view layout resource or by overriding various convenience resources.
 *
 * The following resource IDs are recognized in the media_item_view layout:
 * <ul>
 *     <li>media_item_frame</li>
 *     <li>media_item_image</li>
 *     <li>media_item_overlay</li>
 *     <li>media_item_title</li>
 * </ul>
 *
 * The following resources are provided for convenience to modify the default layout:
 * <ul>
 *     <li>@drawable/camera                    : overlay image for image capture sources</li>
 *     <li>@drawable/video                     : overlay image for video capture sources</li>
 *     <li>@drawable/media_item_frame_selector : defines frame background based on check state</li>
 *     <li>@string/cd_media_item_image         : content description for media image</li>
 *     <li>@string/cd_media_item_overlay       : content description for overlay image</li>
 *     <li>@dimen/media_item_height            : height of the entire MediaItem view</li>
 *     <li>@dimen/media_item_frame_margin_*    : frame margins; * = left/top/right/bottom</li>
 *     <li>@dimen/media_item_frame_padding_*   : frame padding; * = left/top/right/bottom</li>
 *     <li>@dimen/media_item_title_margin_*    : title margins; * = left/top/right/bottom</li>
 *     <li>@dimen/media_item_title_padding_*   : title padding; * = left/top/right/bottom</li>
 * </ul>
 */

public class MediaItemAdapter extends BaseAdapter {
    private final LayoutInflater  mLayoutInflater;
    private final List<MediaItem> mContent;
    private final int mMediaItemViewHeight;

    public MediaItemAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
        mContent = new ArrayList<>();

        mMediaItemViewHeight = (int) context.getResources().getDimension(R.dimen.media_item_height);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getCount() {
        return mContent.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @Override
    public Object getItem(int position) {
        return mContent.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.media_item_view, parent, false);
        }

        MediaItem mediaItem = mContent.get(position);

        if (mediaItem != null) {
            layoutTitleView(convertView, mediaItem.getTitle());
            layoutImageView(convertView, mediaItem.getPreviewSource());
            layoutOverlayView(convertView, mediaItem.getTag());
        }

        return convertView;
    }

    /**
     * Adds a {@link org.wordpress.mediapicker.MediaItem} to the end of the content list.
     *
     * @param mediaItem
     *  the item to add
     */
    public void addMedia(MediaItem mediaItem) {
        addMedia(mContent.size(), mediaItem);
    }

    /**
     * Adds a {@link org.wordpress.mediapicker.MediaItem} at a specified position.
     *
     * @param position
     *  the index to add the item
     * @param mediaItem
     *  the item to add
     */
    public void addMedia(int position, MediaItem mediaItem) {
        if (mediaItem != null) {
            mContent.add(position, mediaItem);
        }
    }

    /**
     * Helper method; conditionally hides title text view based on the provided text.
     *
     * @param host
     *  parent view of the title {@link android.widget.TextView}
     * @param text
     *  the string to present in the MediaItem view; if null or empty the view will not be visible
     */
    private void layoutTitleView(View host, String text) {
        final TextView titleView = (TextView) host.findViewById(R.id.media_item_title);

        if (titleView != null) {
            int visibility = (text == null || text.equals("")) ? View.INVISIBLE : View.VISIBLE;

            titleView.setVisibility(visibility);
            titleView.setText(text);
        }
    }

    /**
     * Helper method; begins a background thread to load an image from a given URI.
     *
     * @param host
     *  parent view of the MediaItem preview {@link android.widget.ImageView}
     * @param imageSource
     *  URI of the image to present in the MediaItem view
     */
    private void layoutImageView(View host, Uri imageSource) {
        if (imageSource != null) {
            ImageView previewView = (ImageView) host.findViewById(R.id.media_item_image);

            if (previewView != null) {
                MediaUtils.BackgroundFetchThumbnail bgDownload =
                        new MediaUtils.BackgroundFetchThumbnail(previewView,
                                MediaUtils.BackgroundFetchThumbnail.THUMB_TYPE.IMAGE,
                                mMediaItemViewHeight,
                                mMediaItemViewHeight);
                previewView.setTag(bgDownload);
                bgDownload.execute(imageSource);
            }
        }
    }

    /**
     * Helper method; adds an overlay image on capture media sources.
     *
     * @param host
     *  parent view of the MediaItem overlay {@link android.widget.ImageView}
     * @param itemTag
     *  tag of the MediaItem, if it matches capture source tags an overlay will be drawn
     */
    private void layoutOverlayView(View host, String itemTag) {
        // TODO
        if (itemTag != null) {
            ImageView overlayView = (ImageView) host.findViewById(R.id.media_item_overlay);

            if (overlayView != null) {
                if (itemTag.equals(MediaSourceCaptureImage.TAG_CAPTURE_IMAGE)) {
                    overlayView.setImageResource(R.drawable.camera);
                    overlayView.setVisibility(View.VISIBLE);
                } else if (itemTag.equals(MediaSourceCaptureVideo.TAG_CAPTURE_VIDEO)) {
                    overlayView.setImageResource(R.drawable.video);
                    overlayView.setVisibility(View.VISIBLE);
                } else {
                    overlayView.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
}
