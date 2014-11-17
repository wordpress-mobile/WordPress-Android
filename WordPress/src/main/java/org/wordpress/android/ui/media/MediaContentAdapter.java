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
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays {@link MediaContent}.
 */

public class MediaContentAdapter extends BaseAdapter {
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

            ImageView imageView = (ImageView) convertView.findViewById(R.id.mediaContentBackgroundImage);
            if (imageView != null) {
                int imageResource = isVideo ? R.drawable.media_image_placeholder : R.drawable.media_image_placeholder;
                imageView.setImageDrawable(mResources.getDrawable(imageResource));
            }

            ImageView overlayView = (ImageView) convertView.findViewById(R.id.mediaContentOverlayImage);
            if (overlayView != null) {
                overlayView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                         ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                         Gravity.CENTER));
                overlayView.setAlpha(0.8f);
                overlayView.setVisibility(View.VISIBLE);
                int overlayResource = isVideo ? R.drawable.dashicon_video_alt2_black : R.drawable.dashicon_camera_black;
                overlayView.setImageDrawable(mResources.getDrawable(overlayResource));
            }

            WPTextView titleView = (WPTextView) convertView.findViewById(R.id.mediaContentTitle);
            if (titleView != null) {
                titleView.setText(isVideo ? "Capture video" : "Capture image");
            }
        }

        return convertView;
    }

    /** Helper method to create the view for device images. */
    private View createDeviceImageContentView(View convertView, MediaContent content) {
        if (convertView != null) {
            ImageView contentImage = (ImageView) convertView.findViewById(R.id.mediaContentBackgroundImage);
            TextView contentTitle = (TextView) convertView.findViewById(R.id.mediaContentTitle);
            ImageView overlayView = (ImageView) convertView.findViewById(R.id.mediaContentOverlayImage);

            if (contentTitle != null) {
                contentTitle.setText(content.getContentTitle());
            }

            if (contentImage != null) {
                if (content.getContentPreviewUri() == null || content.getContentPreviewUri().getPath().equals("")) {
                    contentImage.setImageResource(R.drawable.media_image_placeholder);
                } else {
                    contentImage.setImageResource(R.drawable.media_image_placeholder);
                    MediaUtils.BackgroundFetchDeviceImage bgDownload = new MediaUtils.BackgroundFetchDeviceImage(contentImage);
                    bgDownload.execute(content.getContentPreviewUri());
                }
            }

            if (overlayView != null) {
                overlayView.setVisibility(View.INVISIBLE);
            }
        }

        return convertView;
    }

    /** Helper method to create the view for device images. */
    private View createDeviceVideoContentView(View convertView, MediaContent content) {
        if (convertView != null) {
            ImageView contentImage = (ImageView) convertView.findViewById(R.id.mediaContentBackgroundImage);
            TextView contentTitle = (TextView) convertView.findViewById(R.id.mediaContentTitle);
            ImageView overlayView = (ImageView) convertView.findViewById(R.id.mediaContentOverlayImage);

            if (contentTitle != null) {
                contentTitle.setText(content.getContentTitle());
            }

            if (contentImage != null) {
                if (content.getContentPreviewUri() == null || content.getContentPreviewUri().getPath().equals("")) {
                    contentImage.setImageResource(R.drawable.media_image_placeholder);
                } else {
                    contentImage.setImageResource(R.drawable.media_image_placeholder);
                    MediaUtils.BackgroundFetchVideoThumbnail bgDownload = new MediaUtils.BackgroundFetchVideoThumbnail(contentImage);
                    bgDownload.execute(content.getContentPreviewUri());
                }
            }

            if (overlayView != null) {
                overlayView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                         ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                         Gravity.CENTER));
                overlayView.setAlpha(0.7f);
                overlayView.setImageResource(R.drawable.ic_media_play);
                overlayView.setVisibility(View.VISIBLE);
            }
        }

        return convertView;
    }

    private View createWebImageContentView(View convertView, MediaContent content) {
        if (convertView != null) {
            ImageView contentImage = (ImageView) convertView.findViewById(R.id.mediaContentBackgroundImage);
            TextView contentTitle = (TextView) convertView.findViewById(R.id.mediaContentTitle);
            ImageView overlayView = (ImageView) convertView.findViewById(R.id.mediaContentOverlayImage);

            if (contentTitle != null) {
                contentTitle.setText(content.getContentTitle());
            }

            if (contentImage != null) {
                if (content.getContentPreviewUri() == null || content.getContentPreviewUri().getPath().equals("")) {
                    contentImage.setImageResource(R.drawable.ic_action_web_site);
                } else {
                    contentImage.setImageResource(R.drawable.media_image_placeholder);
                    MediaUtils.BackgroundDownloadWebImage bgDownload = new MediaUtils.BackgroundDownloadWebImage(contentImage);
                    bgDownload.execute(content.getContentPreviewUri());
                }
            }

            if (overlayView != null) {
                overlayView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                         ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                         Gravity.TOP | Gravity.RIGHT));
                overlayView.setAlpha(0.65f);
                overlayView.setImageResource(R.drawable.dashicon_wordpress_alt);
                overlayView.setVisibility(View.VISIBLE);
            }
        }

        return convertView;
    }
}
