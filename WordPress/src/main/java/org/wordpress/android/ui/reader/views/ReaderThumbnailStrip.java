package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

/**
 *
 */
public class ReaderThumbnailStrip extends LinearLayout {

    private static final int MIN_IMAGE_WIDTH = 320;

    private View mView;
    private ViewGroup mContainer;
    private int mThumbnailSize;

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

    public ReaderThumbnailStrip(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    public void initView(Context context) {
        mView = inflate(context, R.layout.reader_thumbnail_strip, this);
        mContainer = (ViewGroup) mView.findViewById(R.id.thumbnail_strip_container);
        mThumbnailSize = context.getResources().getDimensionPixelSize(R.dimen.reader_thumbnail_strip_image_size);
    }

    private void showView() {
        if (mView.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(mView, AniUtils.Duration.SHORT);
        }
    }

    private void hideView() {
        if (mView.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(mView, AniUtils.Duration.SHORT);
        }
    }

    public void loadThumbnails(@NonNull ReaderPost post) {
        mContainer.removeAllViews();

        ReaderImageScanner scanner = new ReaderImageScanner(post.getText(), post.isPrivate);
        List<String> images = new ReaderImageScanner(post.getText(), post.isPrivate).getImageList(MIN_IMAGE_WIDTH);
        if (images.isEmpty()) {
            hideView();
            return;
        }

        for (String imageUrl: images) {
            View view = inflate(getContext(), R.layout.reader_thumbnail_strip_image, mContainer);
            WPNetworkImageView imageView = (WPNetworkImageView) view.findViewById(R.id.thumbnail_strip_image);
            String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, mThumbnailSize, mThumbnailSize);
            imageView.setImageUrl(photonUrl, WPNetworkImageView.ImageType.PHOTO);
        }

        showView();
    }
}
