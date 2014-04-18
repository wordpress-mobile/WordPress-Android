package org.wordpress.android.ui.reader;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderBlogDetailActivity extends SherlockFragmentActivity
                                      implements View.OnTouchListener {

    private WPNetworkImageView mImageMshot;
    private int mImageHeight;
    private int mImageWidth;
    private float mScaleFactor = 1.0f;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_blog_detail);

        mImageMshot = (WPNetworkImageView) findViewById(R.id.image_mshot);
        mImageHeight = getResources().getDimensionPixelSize(R.dimen.reader_blog_header_image_height);
        mImageWidth = DisplayUtils.getDisplayPixelWidth(this);

        mImageMshot.setScaleType(ImageView.ScaleType.MATRIX);
        scaleImage(mScaleFactor);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        long blogId = getIntent().getLongExtra(ReaderActivity.ARG_BLOG_ID, 0);
        showListFragmentForBlog(blogId);
        showMShot(blogId);
    }

    private void showMShot(long blogId) {
        final ReaderBlogInfo blogInfo = ReaderBlogTable.getBlogInfo(blogId);
        if (blogInfo == null) {
            return;
        }

        int width = DisplayUtils.getDisplayPixelWidth(this);
        mImageMshot.setImageUrl(getMshotsUrl(blogInfo.getUrl(), width), WPNetworkImageView.ImageType.PHOTO);

        mImageMshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderActivityLauncher.openUrl(ReaderBlogDetailActivity.this, blogInfo.getUrl());
            }
        });
    }

    private static String getMshotsUrl(String blogUrl, int width) {
        if (TextUtils.isEmpty(blogUrl)) {
            return "";
        }

        return "http://s.wordpress.com/mshots/v1/"
                + UrlUtils.urlEncode(blogUrl)
                + "?w=" + Integer.toString(width);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showProgress() {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_loading);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_loading);
        progressBar.setVisibility(View.GONE);
    }

    /*
     * show fragment containing list of latest posts in a specific blog
     */
    private void showListFragmentForBlog(long blogId) {
        ReaderPostListFragment fragment = ReaderPostListFragment.newInstance(blogId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    private ReaderPostListFragment getListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_reader_post_list));
        if (fragment == null)
            return null;
        return ((ReaderPostListFragment) fragment);
    }

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }

    private boolean mIsMoving;
    private float mLastMotionY;
    private static final int MOVE_MIN_DIFF = 1;

    private void scaleImage(float scale) {
        mScaleFactor += scale;
        if (mScaleFactor <= 0) {
            mScaleFactor = 0;
            mImageMshot.setVisibility(View.INVISIBLE);
            return;
        } else if (mScaleFactor >= 1.0f) {
            mScaleFactor = 1.0f;
            return;
        }
        mImageMshot.setVisibility(View.VISIBLE);
        Matrix matrix = mImageMshot.getImageMatrix();
        RectF drawableRect = new RectF(0, 0, mImageWidth, mImageHeight);
        RectF viewRect = new RectF(0, 0, mImageMshot.getWidth(), mImageMshot.getHeight());
        matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
        matrix.postScale(1.0f, mScaleFactor);
        mImageMshot.setImageMatrix(matrix);
        mImageMshot.invalidate();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        final float y = event.getY();
        final int yDiff = (int) (y - mLastMotionY);
        mLastMotionY = y;

        switch (action) {
            case MotionEvent.ACTION_MOVE :
                if (mIsMoving) {
                    float scaleStep = yDiff * 0.0015f;
                    if (yDiff < -MOVE_MIN_DIFF) {
                        // user is scrolling down
                        scaleImage(scaleStep);
                    } else if (yDiff > MOVE_MIN_DIFF) {
                        // user is scrolling up
                        scaleImage(scaleStep);
                    }
                } else {
                    mIsMoving = true;
                }
                break;

            default :
                mIsMoving = false;
                break;
        }

        return false;
    }
}
