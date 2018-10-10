package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener;
import org.wordpress.android.ui.reader.models.ReaderSimplePost;
import org.wordpress.android.ui.reader.models.ReaderSimplePostList;
import org.wordpress.android.util.analytics.AnalyticsUtils;

/**
 * used by the detail view to display related posts, which can be either local (related posts
 * from the same site as the source post) or global (related posts from across wp.com)
 */
public class ReaderSimplePostContainerView extends LinearLayout {
    private OnFollowListener mFollowListener;

    private final ReaderSimplePostList mSimplePostList = new ReaderSimplePostList();

    public ReaderSimplePostContainerView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderSimplePostContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderSimplePostContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public ReaderSimplePostContainerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.reader_simple_posts_container_view, this);
    }

    public void showPosts(ReaderSimplePostList posts,
                          String siteName,
                          boolean isGlobal,
                          ReaderSimplePostView.OnSimplePostClickListener listener) {
        mSimplePostList.clear();
        mSimplePostList.addAll(posts);

        ViewGroup container = (ViewGroup) findViewById(R.id.container_related_posts);
        container.removeAllViews();

        // nothing more to do if passed list is empty
        if (mSimplePostList.size() == 0) {
            return;
        }

        // add a view for each post
        for (int index = 0; index < mSimplePostList.size(); index++) {
            ReaderSimplePost relatedPost = mSimplePostList.get(index);
            ReaderSimplePostView postView = new ReaderSimplePostView(getContext());
            postView.setOnFollowListener(mFollowListener);
            postView.showPost(relatedPost, container, isGlobal, listener);
        }

        // make sure the label for these posts has the correct caption
        TextView label = findViewById(R.id.text_related_posts_label);
        if (isGlobal) {
            label.setText(getContext().getString(R.string.reader_label_global_related_posts));
        } else {
            label.setText(String.format(getContext().getString(R.string.reader_label_local_related_posts), siteName));
        }
    }

    public void setOnFollowListener(OnFollowListener listener) {
        mFollowListener = listener;
    }

    /*
     * called by reader detail when scrolled into view, tracks railcar events for each post
     */
    public void trackRailcarRender() {
        for (ReaderSimplePost post : mSimplePostList) {
            AnalyticsUtils.trackRailcarRender(post.getRailcarJson());
        }
    }
}
