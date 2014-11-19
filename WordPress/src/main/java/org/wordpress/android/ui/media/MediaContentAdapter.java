package org.wordpress.android.ui.media;

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays {@link MediaContent}.
 */

public class MediaContentAdapter extends BaseAdapter {
    private static final FrameLayout.LayoutParams CAPTURE_CONTENT_OVERLAY_LAYOUT_PARAMES =
            new FrameLayout.LayoutParams(128, 128, Gravity.CENTER);
    private static final FrameLayout.LayoutParams DEVICE_VIDEO_CONTENT_OVERLAY_LAYOUT_PARAMS =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                         ViewGroup.LayoutParams.WRAP_CONTENT,
                                         Gravity.CENTER);
    private static final FrameLayout.LayoutParams WEB_IMAGE_CONTENT_OVERLAY_LAYOUT_PARAMS =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                         ViewGroup.LayoutParams.WRAP_CONTENT,
                                         Gravity.TOP | Gravity.END);

    private final Resources      mResources;
    private final LayoutInflater mLayoutInflator;

    private List<MediaContent> mContent;

    public MediaContentAdapter(Context context) {
        mResources = context.getResources();
        mLayoutInflator = LayoutInflater.from(context);

        mContent = new ArrayList<MediaContent>();
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return mContent.size();
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
            convertView = mLayoutInflator.inflate(R.layout.media_content_grid_item, parent, false);
        }

        MediaContent content = mContent.get(position);
        switch (content.getType()) {
        case INVALID:
            // TODO
            break;
        case CAPTURE:
            convertView = createCaptureContentView(convertView, content);
            break;
        case DEVICE_IMAGE:
            convertView = createDeviceImageContentView(convertView, content);
            break;
        case DEVICE_VIDEO:
            convertView = createDeviceVideoContentView(convertView, content);
            break;
        case WEB_IMAGE:
            convertView = createWebImageContentView(convertView, content);
            break;
        }

        return convertView;
    }

    public void addContent(MediaContent newContent) {
        addContent(newContent, false);
    }

    public void addContent(MediaContent newContent, boolean sort) {
        if (newContent != null) {
            mContent.add(newContent);

            if (sort) {
                // TODO
            }
        }
    }

    /** Helper method to create the view for content capture. */
    private View createCaptureContentView(View convertView, MediaContent content) {
        if (convertView != null) {
            String tag = content.getTag();
            boolean isVideo = tag != null && tag.equals("CaptureVideo");

            ImageView contentImageView = (ImageView) convertView.findViewById(R.id.mediaContentBackgroundImage);
            if (contentImageView != null) {
                contentImageView.setImageBitmap(null);
                contentImageView.setBackgroundColor(mResources.getColor(R.color.grey_medium_light));
            }

            layoutTitleView(convertView, isVideo ? "Capture video" : "Capture image", View.VISIBLE);
            layoutOverlay(convertView, CAPTURE_CONTENT_OVERLAY_LAYOUT_PARAMES, 0.8f, isVideo ? R.drawable.video : R.drawable.camera, View.VISIBLE);
        }

        return convertView;
    }

    /** Helper method to create the view for device images. */
    private View createDeviceImageContentView(View convertView, MediaContent content) {
        if (convertView != null) {
            ImageView contentImageView = (ImageView) convertView.findViewById(R.id.mediaContentBackgroundImage);

            if (contentImageView != null) {
                if (content.getContentPreviewUri() == null || content.getContentPreviewUri().getPath().equals("")) {
                    contentImageView.setImageResource(R.drawable.media_image_placeholder);
                } else {
                    contentImageView.setImageResource(R.drawable.media_image_placeholder);
                    MediaUtils.BackgroundFetchDeviceImage bgDownload = new MediaUtils.BackgroundFetchDeviceImage(contentImageView);
                    bgDownload.execute(content.getContentPreviewUri());
                }
            }

            layoutTitleView(convertView, "", View.INVISIBLE);
            layoutOverlay(convertView, null, 0.0f, -1, View.INVISIBLE);
        }

        return convertView;
    }

    /** Helper method to create the view for device images. */
    private View createDeviceVideoContentView(View convertView, MediaContent content) {
        if (convertView != null) {
            ImageView contentImage = (ImageView) convertView.findViewById(R.id.mediaContentBackgroundImage);

            if (contentImage != null) {
                if (content.getContentPreviewUri() == null || content.getContentPreviewUri().getPath().equals("")) {
                    contentImage.setImageResource(R.drawable.media_image_placeholder);
                } else {
                    contentImage.setImageResource(R.drawable.media_image_placeholder);
                    MediaUtils.BackgroundFetchVideoThumbnail bgDownload = new MediaUtils.BackgroundFetchVideoThumbnail(contentImage);
                    bgDownload.execute(content.getContentPreviewUri());
                }
            }

            layoutTitleView(convertView, "", View.INVISIBLE);
            layoutOverlay(convertView, DEVICE_VIDEO_CONTENT_OVERLAY_LAYOUT_PARAMS, 0.7f, R.drawable.ic_media_play, View.VISIBLE);
        }

        return convertView;
    }

    private View createWebImageContentView(View convertView, MediaContent content) {
        if (convertView != null) {
            ImageView contentImageView = (ImageView) convertView.findViewById(R.id.mediaContentBackgroundImage);

            if (contentImageView != null) {
                if (content.getContentPreviewUri() == null || content.getContentPreviewUri().getPath().equals("")) {
                    contentImageView.setImageResource(R.drawable.ic_action_web_site);
                } else {
                    contentImageView.setImageResource(R.drawable.media_image_placeholder);
                    MediaUtils.BackgroundDownloadWebImage bgDownload = new MediaUtils.BackgroundDownloadWebImage(contentImageView);
                    bgDownload.execute(content.getContentPreviewUri());
                }
            }

            layoutTitleView(convertView, "", View.INVISIBLE);
            layoutOverlay(convertView, WEB_IMAGE_CONTENT_OVERLAY_LAYOUT_PARAMS, 0.65f, R.drawable.dashicon_wordpress_alt, View.VISIBLE);
        }

        return convertView;
    }

    private void layoutTitleView(View host, String text, int visibility) {
        final TextView contentTitleView = (TextView) host.findViewById(R.id.mediaContentTitle);

        if (contentTitleView != null) {
            contentTitleView.setText(text);
            contentTitleView.setVisibility(visibility);
        }
    }

    private void layoutOverlay(View host, FrameLayout.LayoutParams layoutParams, float alpha, int resource, int visibility) {
        ImageView overlayView = (ImageView) host.findViewById(R.id.mediaContentOverlayImage);

        if (overlayView != null) {
            if (layoutParams != null) {
                overlayView.setLayoutParams(layoutParams);
            }
            if (resource > -1) {
                overlayView.setImageResource(resource);
            }
            overlayView.setAlpha(alpha);
            overlayView.setVisibility(visibility);
        }
    }
}
