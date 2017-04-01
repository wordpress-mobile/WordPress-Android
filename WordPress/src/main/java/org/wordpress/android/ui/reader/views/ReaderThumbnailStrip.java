package org.wordpress.android.ui.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.models.ReaderImageList;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.EnumSet;

/**
 * displays a row of image thumbnails from a reader post - only shows when two or more images
 * of a minimum size are found
 */
public class ReaderThumbnailStrip extends LinearLayout {

    public static final int IMAGE_COUNT = 4;

    private ViewGroup mView;
    private int mThumbnailHeight;
    private int mThumbnailWidth;

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
        mView = (ViewGroup) inflate(context, R.layout.reader_thumbnail_strip, this);
        mThumbnailHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_thumbnail_strip_image_height);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int margins = context.getResources().getDimensionPixelSize(R.dimen.reader_card_content_padding) * 2;
        mThumbnailWidth = (displayWidth - margins) / IMAGE_COUNT;
    }

    public void loadThumbnails(long blogId, long postId, boolean privateEh) {
        // get rid of any views already added
        mView.removeAllViews();

        // get this post's content and scan it for images suitable in a gallery
        final String content = ReaderPostTable.getPostText(blogId, postId);
        final ReaderImageList imageList =
                new ReaderImageScanner(content, privateEh).getImageList(IMAGE_COUNT, ReaderConstants.MIN_GALLERY_IMAGE_WIDTH);
        if (imageList.size() < IMAGE_COUNT) {
            mView.setVisibility(View.GONE);
            return;
        }

        final EnumSet<PhotoViewerOption> photoViewerOptions = EnumSet.of(PhotoViewerOption.IS_GALLERY_IMAGE);
        if (privateEh) {
            photoViewerOptions.add(PhotoViewerOption.IS_PRIVATE_IMAGE);
        }

        // add a separate imageView for each image up to the max
        int numAdded = 0;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (final String imageUrl: imageList) {
            View view = inflater.inflate(R.layout.reader_thumbnail_strip_image, mView, false);
            WPNetworkImageView imageView = (WPNetworkImageView) view.findViewById(R.id.thumbnail_strip_image);
            mView.addView(view);

            String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, mThumbnailWidth, mThumbnailHeight);
            imageView.setImageUrl(photonUrl, WPNetworkImageView.ImageType.PHOTO);

            // tapping a thumbnail opens the photo viewer
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.showReaderPhotoViewer(
                            view.getContext(),
                            imageUrl,
                            content,
                            view,
                            photoViewerOptions,
                            0,
                            0);
                }
            });

            numAdded++;
            if (numAdded >= IMAGE_COUNT) {
                break;
            }
        }

        if (mView.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(mView, AniUtils.Duration.SHORT);
        }
    }
}
