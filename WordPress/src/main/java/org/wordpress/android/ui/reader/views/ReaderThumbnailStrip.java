package org.wordpress.android.ui.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderPhotoViewerActivity;
import org.wordpress.android.ui.reader.models.ReaderImageList;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * displays a row of image thumbnails from a reader post - only shows when two or more images
 * of a minimum size are found
 */
public class ReaderThumbnailStrip extends LinearLayout {

    private static final int MIN_IMAGE_COUNT = 2;
    private static final int MAX_IMAGE_COUNT = 4;

    private View mView;
    private LinearLayout mContainer;
    private int mThumbnailSize;
    private String mCountStr;

    public ReaderThumbnailStrip(Context context) {
        super(context);
        initView(context);
    }

    public ReaderThumbnailStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderThumbnailStrip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReaderThumbnailStrip(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        mView = inflate(context, R.layout.reader_thumbnail_strip, this);
        mContainer = (LinearLayout) mView.findViewById(R.id.thumbnail_strip_container);
        mThumbnailSize = context.getResources().getDimensionPixelSize(R.dimen.reader_thumbnail_strip_image_size);
        mCountStr = context.getResources().getString(R.string.reader_label_image_count);
    }

    private void fadeIn() {
        if (mView.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(mView, AniUtils.Duration.SHORT);
        }
    }

    private void fadeOut() {
        if (mView.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(mView, AniUtils.Duration.SHORT);
        }
    }

    public void loadThumbnails(@NonNull final ReaderPost post) {
        // get rid of any views already added
        mContainer.removeAllViews();

        // TODO: it would be more efficient to rely on the attachments
        final ReaderImageList imageList = new ReaderImageScanner(post.getText(), post.isPrivate).getImageList(ReaderPhotoViewerActivity.MIN_IMAGE_WIDTH);
        if (imageList.size() < MIN_IMAGE_COUNT) {
            fadeOut();
            return;
        }

        // add a separate imageView for each image up to the max
        int numAdded = 0;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (final String imageUrl: imageList) {
            View view = inflater.inflate(R.layout.reader_thumbnail_strip_image, mContainer, false);
            WPNetworkImageView imageView = (WPNetworkImageView) view.findViewById(R.id.thumbnail_strip_image);
            mContainer.addView(view);

            String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, mThumbnailSize, mThumbnailSize);
            imageView.setImageUrl(photonUrl, WPNetworkImageView.ImageType.PHOTO);

            numAdded++;
            if (numAdded >= MAX_IMAGE_COUNT) {
                break;
            }
        }

        // add the labels which include the image count
        View labelView = inflater.inflate(R.layout.reader_thumbnail_strip_labels, mContainer, false);
        TextView txtCount = (TextView) labelView.findViewById(R.id.text_gallery_count);
        txtCount.setText(String.format(mCountStr, imageList.size()));
        mContainer.addView(labelView);

        // tapping anywhere opens the first image
        mView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ReaderActivityLauncher.showReaderPhotoViewer(
                        view.getContext(),
                        imageList.get(0),
                        post.getText(),
                        view,
                        post.isPrivate,
                        0,
                        0);
            }
        });

        fadeIn();
    }
}
