package org.wordpress.android.ui.media;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.media.content.CaptureMediaContent;
import org.wordpress.android.ui.media.content.MediaContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to hold media content and display it as a grid.
 */

public class MediaSelectGridView extends GridView {
    private MediaContentAdapter mAdapter;

    public MediaSelectGridView(Context context) {
        super(context);

        mAdapter = new MediaContentAdapter();
        setAdapter(mAdapter);
    }

    public MediaSelectGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaSelectGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void addContent(MediaContent content) {
        mAdapter.addContent(content);
    }

    private class MediaContentAdapter extends BaseAdapter {
        private List<MediaContent> mContent;

        public MediaContentAdapter() {
            mContent = new ArrayList<MediaContent>();
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
                MediaContent content = mContent.get(position);

                switch (content.getType()) {
                    case INVALID:
                        break;
                    case CAPTURE:
                        convertView = createCaptureContentView((CaptureMediaContent) content, parent);
                }
            }

            return convertView;
        }

        public void addContent(MediaContent newContent) {
            addContent(newContent, false);
        }

        public void addContent(MediaContent newContent, boolean sort) {
            if (newContent != null) mContent.add(newContent);

            if (sort) {
                // TODO
            }

            notifyDataSetChanged();
        }

        /** Helper method to create the view for content capture */
        private View createCaptureContentView(CaptureMediaContent content, ViewGroup root) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.capture_media_content, root, false);

            if (view != null) {
                int captureType = content.getCaptureType();

                ImageView imageView = (ImageView) view.findViewById(R.id.contentImage);
                if (imageView != null) {
                    if (captureType == CaptureMediaContent.CAPTURE_TYPE_IMAGE) {
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.dashicon_camera_black));
                    }else if (captureType == CaptureMediaContent.CAPTURE_TYPE_VIDEO) {
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.dashicon_video_alt2_black));
                    }
                }

                TextView textView = (TextView) view.findViewById(R.id.contentTitle);
                if (textView != null) {
                    if (captureType == CaptureMediaContent.CAPTURE_TYPE_IMAGE) {
                        textView.setText("Take a picture");
                    } else if(captureType == CaptureMediaContent.CAPTURE_TYPE_VIDEO) {
                        textView.setText("Capture video");
                    }
                }
            }

            view.refreshDrawableState();
            return view;
        }
    }
}
