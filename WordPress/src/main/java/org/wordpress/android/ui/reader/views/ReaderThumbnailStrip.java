package org.wordpress.android.ui.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption;
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

    private View mView;
    private LinearLayout mContainer;
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
        mView = inflate(context, R.layout.reader_thumbnail_strip, this);
        mContainer = (LinearLayout) mView.findViewById(R.id.thumbnail_strip_container);
        mThumbnailHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_thumbnail_strip_image_height);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int margins = context.getResources().getDimensionPixelSize(R.dimen.reader_card_content_padding) * 2;
        mThumbnailWidth = (displayWidth - margins) / IMAGE_COUNT;
    }

    public void loadThumbnails(long blogId, long postId, final boolean isPrivate) {
        // get rid of any views already added
        mContainer.removeAllViews();

        // get this post's content and scan it for images suitable in a gallery
        final String content = ReaderPostTable.getPostText(blogId, postId);
        final ReaderImageList imageList =
                new ReaderImageScanner(content, isPrivate).getGalleryImageList();
        if (imageList.size() < IMAGE_COUNT) {
            mView.setVisibility(View.GONE);
            return;
        }

        // add a separate imageView for each image up to the max
        int numAdded = 0;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String imageUrl: imageList) {
            View view = inflater.inflate(R.layout.reader_thumbnail_strip_image, mContainer, false);
            WPNetworkImageView imageView = (WPNetworkImageView) view.findViewById(R.id.thumbnail_strip_image);
            mContainer.addView(view);

            String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, mThumbnailWidth, mThumbnailHeight);
            imageView.setImageUrl(photonUrl, WPNetworkImageView.ImageType.PHOTO);

            numAdded++;
            if (numAdded >= IMAGE_COUNT) {
                break;
            }
        }

        // tapping anywhere opens the first image
        mView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                EnumSet<PhotoViewerOption> options = EnumSet.of(PhotoViewerOption.IS_GALLERY_IMAGE);
                if (isPrivate) {
                    options.add(PhotoViewerOption.IS_PRIVATE_IMAGE);
                }
                ReaderActivityLauncher.showReaderPhotoViewer(
                        view.getContext(),
                        imageList.get(0),
                        content,
                        view,
                        options,
                        0,
                        0);
            }
        });

        if (mView.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(mView, AniUtils.Duration.SHORT);
        }
    }
}
