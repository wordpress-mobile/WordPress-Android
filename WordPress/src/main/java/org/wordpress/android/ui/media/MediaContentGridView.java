package org.wordpress.android.ui.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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
import org.wordpress.android.ui.media.content.DeviceImageMediaContent;
import org.wordpress.android.ui.media.content.MediaContent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to hold media content and display it as a grid.
 */

public class MediaContentGridView extends GridView {
    private MediaContentAdapter mAdapter;

    public MediaContentGridView(Context context) {
        super(context);

        mAdapter = new MediaContentAdapter();
        setAdapter(mAdapter);
    }

    public MediaContentGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaContentGridView(Context context, AttributeSet attrs, int defStyle) {
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
        public int getViewTypeCount() {
            return MediaContent.MEDIA_TYPE.COUNT.ordinal();
        }

        @Override
        public int getItemViewType(int position) {
            if (position < mContent.size()) {
                return mContent.get(position).getType().ordinal();
            }

            return 0;
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
            MediaContent content = mContent.get(position);

            switch (content.getType()) {
                case INVALID:
                    break;
                case CAPTURE:
                    convertView = createCaptureContentView(convertView, (CaptureMediaContent) content, parent);
                    break;
                case DEVICE_IMAGE:
                    convertView = createDeviceImageContentView(convertView, (DeviceImageMediaContent) content, parent);
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

        private View createDeviceImageContentView(View convertView, DeviceImageMediaContent content, ViewGroup root) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.device_image_media_content, root, false);
            }

            if (convertView != null) {
                final ImageView contentImage = (ImageView) convertView.findViewById(R.id.deviceImageContentImage);
                TextView contentTitle = (TextView) convertView.findViewById(R.id.deviceImageContentTitle);

                if (contentTitle != null) {
                    contentTitle.setText(content.getName());
                }

                if (contentImage != null) {
                    BackgroundDownload bgDownload = new BackgroundDownload(contentImage);
                    bgDownload.execute(content.getThumbUri());
                }
            }

            return convertView;
        }

        /** Helper method to create the view for content capture */
        private View createCaptureContentView(View convertView, CaptureMediaContent content, ViewGroup root) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.capture_media_content, root, false);
            }

            if (convertView != null) {
                int captureType = content.getCaptureType();

                ImageView imageView = (ImageView) convertView.findViewById(R.id.contentImage);
                if (imageView != null) {
                    if (captureType == CaptureMediaContent.CAPTURE_TYPE_IMAGE) {
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.dashicon_camera_black));
                    }else if (captureType == CaptureMediaContent.CAPTURE_TYPE_VIDEO) {
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.dashicon_video_alt2_black));
                    }
                }

                TextView textView = (TextView) convertView.findViewById(R.id.contentTitle);
                if (textView != null) {
                    if (captureType == CaptureMediaContent.CAPTURE_TYPE_IMAGE) {
                        textView.setText("Take a picture");
                    } else if(captureType == CaptureMediaContent.CAPTURE_TYPE_VIDEO) {
                        textView.setText("Capture video");
                    }
                }
            }

            return convertView;
        }

        private class BackgroundDownload extends AsyncTask<String, String, Bitmap> {
            WeakReference<ImageView> mResultStore;

            public BackgroundDownload(ImageView resultStore) {
                mResultStore = new WeakReference<ImageView>(resultStore);
            }

            @Override
            protected Bitmap doInBackground(String... params) {
                return BitmapFactory.decodeFile(params[0]);
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                ImageView imageView = mResultStore.get();

                if (imageView != null) {
                    imageView.setImageBitmap(result);
                }
            }
        }
    }
}
