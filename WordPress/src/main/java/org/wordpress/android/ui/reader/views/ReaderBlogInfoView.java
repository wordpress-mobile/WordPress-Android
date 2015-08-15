package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.AniUtils;

public class ReaderBlogInfoView extends LinearLayout {

    public interface OnBlogInfoLoadedListener {
        public void onBlogInfoLoaded(ReaderBlog blogInfo);
    }

    private OnBlogInfoLoadedListener mBlogInfoListener;

    public ReaderBlogInfoView(Context context) {
        super(context);
        initView(context, null);
    }

    public ReaderBlogInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ReaderBlogInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    public ReaderBlogInfoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        inflate(context, R.layout.reader_blog_info_view, this);
    }

    public void setOnBlogInfoLoadedListener(OnBlogInfoLoadedListener listener) {
        mBlogInfoListener = listener;
    }

    public void loadBlogInfo(long blogId, long feedId) {
        // get info from local db first
        ReaderBlog blogInfo;
        if (feedId != 0) {
            blogInfo = ReaderBlogTable.getFeedInfo(feedId);
        } else {
            blogInfo = ReaderBlogTable.getBlogInfo(blogId);
        }
        if (blogInfo != null) {
            showBlogInfo(blogInfo);
        }

        // now get from server
        ReaderActions.UpdateBlogInfoListener listener = new ReaderActions.UpdateBlogInfoListener() {
            @Override
            public void onResult(ReaderBlog blogInfo) {
                showBlogInfo(blogInfo);
            }
        };
        if (feedId != 0) {
            ReaderBlogActions.updateFeedInfo(feedId, null, listener);
        } else {
            ReaderBlogActions.updateBlogInfo(blogId, null, listener);
        }
    }

    private void showBlogInfo(ReaderBlog blogInfo) {
        if (blogInfo == null) return;

        ViewGroup layoutInfo = (ViewGroup) findViewById(R.id.layout_blog_info);
        TextView txtDescription = (TextView) layoutInfo.findViewById(R.id.text_blog_description);
        TextView txtFollowCount = (TextView) layoutInfo.findViewById(R.id.text_blog_follow_count);

        if (blogInfo.hasDescription()) {
            txtDescription.setVisibility(View.VISIBLE);
            txtDescription.setText(blogInfo.getDescription());
        } else {
            txtDescription.setVisibility(View.GONE);
        }

        if (blogInfo.numSubscribers > 0) {
            txtFollowCount.setText(
                    String.format(getContext().getString(R.string.reader_label_follow_count), blogInfo.numSubscribers));
            txtFollowCount.setVisibility(View.VISIBLE);
        } else {
            txtFollowCount.setVisibility(View.GONE);
        }

        if (layoutInfo.getVisibility() != View.VISIBLE) {
            AniUtils.scaleIn(layoutInfo, AniUtils.Duration.MEDIUM);
        }

        if (mBlogInfoListener != null) {
            mBlogInfoListener.onBlogInfoLoaded(blogInfo);
        }
    }
}
