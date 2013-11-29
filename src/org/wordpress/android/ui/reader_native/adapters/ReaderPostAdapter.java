package org.wordpress.android.ui.reader_native.adapters;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.ui.reader_native.actions.ReaderPostActions;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * Created by nbradbury on 6/27/13.
 * adapter for list of posts in a specific tag
 */
public class ReaderPostAdapter extends BaseAdapter {
    private String mCurrentTag;

    private int mPhotonWidth;
    private int mPhotonHeight;
    private int mAvatarSz;

    private final float mRowAnimationFromYDelta;
    private final int mRowAnimationDuration;
    private boolean mCanRequestMorePosts = false;
    private boolean mAnimateRows = false;

    private final LayoutInflater mInflater;
    private ReaderPostList mPosts = new ReaderPostList();

    private final int mColorFollow;
    private final int mColorFollowing;

    private ReaderActions.RequestReblogListener mReblogListener;
    private ReaderActions.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;

    public ReaderPostAdapter(Context context,
                             boolean isGridView,
                             ReaderActions.RequestReblogListener reblogListener,
                             ReaderActions.DataLoadedListener dataLoadedListener,
                             ReaderActions.DataRequestedListener dataRequestedListener) {
        super();

        mInflater = LayoutInflater.from(context);

        mReblogListener = reblogListener;
        mDataLoadedListener = dataLoadedListener;
        mDataRequestedListener = dataRequestedListener;

        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.reader_avatar_sz_medium);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int displayHeight = DisplayUtils.getDisplayPixelHeight(context);

        // determine size to use when requesting images via photon - full width unless we're using
        // a grid, in which case half-width since the grid shows two columns
        mPhotonWidth = (isGridView ? displayWidth / 2 : displayWidth);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);

        // when animating rows in, start from this y-position near the bottom using medium animation duration
        mRowAnimationFromYDelta = displayHeight - (displayHeight / 6);
        mRowAnimationDuration = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);

        // colors for follow text
        mColorFollow = context.getResources().getColor(R.color.reader_hyperlink);
        mColorFollowing = context.getResources().getColor(R.color.orange_medium);
    }

    public void setPosts(ReaderPostList posts) {
        if (posts==null) {
            mPosts = new ReaderPostList();
        } else {
            mPosts = (ReaderPostList) posts.clone();
        }
        notifyDataSetChanged();
    }

    public void setTag(String tagName) {
        mCurrentTag = tagName;
        reload();
    }

    private void clear() {
        if (!mPosts.isEmpty()) {
            mPosts.clear();
            notifyDataSetChanged();
        }
    }

    public void refresh() {
        //clear(); <-- don't do this, causes LoadPostsTask to always think all posts are new
        loadPosts();
    }

    public void reload() {
        // briefly animate the appearance of new rows when reloading - happens when the tag is
        // changed or the user taps to view new posts
        mAnimateRows = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAnimateRows = false;
            }
        }, 750);

        clear();
        loadPosts();
    }

    /*
     * reload a single post
     */
    public void reloadPost(ReaderPost post) {
        int index = mPosts.indexOfPost(post);
        if (index == -1)
            return;

        final ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId);
        if (updatedPost==null)
            return;

        mPosts.set(index, updatedPost);
        notifyDataSetChanged();
    }

    @SuppressLint("NewApi")
    private void loadPosts() {
        if (mIsTaskRunning)
            ReaderLog.w("reader posts task already running");

        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadPostsTask().execute();
        }
    }

    @Override
    public int getCount() {
        return mPosts.size();
    }

    @Override
    public Object getItem(int position) {
        return mPosts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReaderPost post = (ReaderPost) getItem(position);
        final PostViewHolder holder;

        if (convertView==null) {
            convertView = mInflater.inflate(R.layout.reader_listitem_post_excerpt, parent, false);
            holder = new PostViewHolder();

            holder.txtTitle = (TextView) convertView.findViewById(R.id.text_title);
            holder.txtText = (TextView) convertView.findViewById(R.id.text_excerpt);
            holder.txtBlogName = (TextView) convertView.findViewById(R.id.text_blog_name);
            holder.txtDate = (TextView) convertView.findViewById(R.id.text_date);
            holder.txtFollow = (TextView) convertView.findViewById(R.id.text_follow);

            holder.txtCommentCount = (TextView) convertView.findViewById(R.id.text_comment_count);
            holder.txtLikeCount = (TextView) convertView.findViewById(R.id.text_like_count);

            holder.imgFeatured = (WPNetworkImageView) convertView.findViewById(R.id.image_featured);
            holder.imgAvatar = (WPNetworkImageView) convertView.findViewById(R.id.image_avatar);

            holder.imgBtnLike = (ImageView) convertView.findViewById(R.id.image_like_btn);
            holder.imgBtnComment = (ImageView) convertView.findViewById(R.id.image_comment_btn);
            holder.imgBtnReblog = (ImageView) convertView.findViewById(R.id.image_reblog_btn);

            convertView.setTag(holder);
        } else {
            holder = (PostViewHolder) convertView.getTag();
        }

        if (post.hasTitle()) {
            holder.txtTitle.setText(post.getTitle());
        } else {
            holder.txtTitle.setText(R.string.reader_untitled_post);
        }

        if (post.hasExcerpt()) {
            holder.txtText.setVisibility(View.VISIBLE);
            holder.txtText.setText(post.getExcerpt());
        } else {
            holder.txtText.setVisibility(View.GONE);
        }

        holder.txtBlogName.setText(post.getBlogName());
        holder.txtDate.setText(DateTimeUtils.javaDateToTimeSpan(post.getDatePublished()));

        // featured image or video
        if (post.hasFeaturedImage()) {
            final String imageUrl = post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight);
            holder.imgFeatured.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);
            holder.imgFeatured.setVisibility(View.VISIBLE);
        } else if (post.hasFeaturedVideo()) {
            holder.imgFeatured.setVideoUrl(post.postId, post.getFeaturedVideo());
            holder.imgFeatured.setVisibility(View.VISIBLE);
        } else {
            holder.imgFeatured.setVisibility(View.GONE);
        }

        if (post.hasPostAvatar()) {
            holder.imgAvatar.setImageUrl(post.getPostAvatarForDisplay(mAvatarSz), WPNetworkImageView.ImageType.AVATAR);
        } else {
            holder.imgAvatar.showDefaultImage(WPNetworkImageView.ImageType.AVATAR);
        }

        // likes, comments & reblogging
        if (post.isWP()) {
            final int pos = position;

            showFollowStatus(holder.txtFollow, post.isFollowedByCurrentUser);
            holder.txtFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollow(holder, pos, post);
                }
            });

            showLikeStatus(holder.imgBtnLike, post.isLikedByCurrentUser);
            holder.imgBtnLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleLike(holder, pos, post);
                }
            });

            showReblogStatus(holder.imgBtnReblog, post.isRebloggedByCurrentUser);
            if (!post.isRebloggedByCurrentUser && post.isWP()) {
                holder.imgBtnReblog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderAniUtils.zoomAction(holder.imgBtnReblog);
                        if (mReblogListener!=null)
                            mReblogListener.onRequestReblog(post);
                    }
                });
            }

            holder.txtFollow.setVisibility(View.VISIBLE);
            holder.imgBtnLike.setVisibility(View.VISIBLE);
            holder.imgBtnComment.setVisibility(View.VISIBLE);
            holder.imgBtnReblog.setVisibility(View.VISIBLE);
        } else {
            holder.txtFollow.setVisibility(View.GONE);
            holder.imgBtnLike.setVisibility(View.INVISIBLE);
            holder.imgBtnComment.setVisibility(View.INVISIBLE);
            holder.imgBtnReblog.setVisibility(View.INVISIBLE);
        }

        showCounts(holder, post);

        // animate the appearance of this row while new posts are being loaded
        if (mAnimateRows)
            animateRow(convertView);

        // if we're nearing the end of the posts, fire request to load more
        if (mCanRequestMorePosts && mDataRequestedListener!=null && (position >= getCount()-1))
            mDataRequestedListener.onRequestData(ReaderActions.RequestDataAction.LOAD_OLDER);

        return convertView;
    }

    /*
     * shows like & comment count
     */
    private void showCounts(final PostViewHolder holder, final ReaderPost post) {
        if (post.numLikes > 0) {
            holder.txtLikeCount.setText(Integer.toString(post.numLikes));
            holder.txtLikeCount.setVisibility(View.VISIBLE);
        } else {
            holder.txtLikeCount.setVisibility(View.GONE);
        }

        if (post.numReplies > 0) {
            holder.txtCommentCount.setText(Integer.toString(post.numReplies));
            holder.txtCommentCount.setVisibility(View.VISIBLE);
        } else {
            holder.txtCommentCount.setVisibility(View.GONE);
        }
    }

    /*
     * animate in the passed view - uses faster property animation on ICS and above, falls back to
     * animation resource for older devices
     */
    private DecelerateInterpolator mRowInterpolator = new DecelerateInterpolator();
    @SuppressLint("NewApi")
    private void animateRow(View view) {
        if (SysUtils.isGteAndroid4()) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, mRowAnimationFromYDelta, 0f);
            animator.setDuration(mRowAnimationDuration);
            animator.setInterpolator(mRowInterpolator);
            animator.start();
        } else {
            ReaderAniUtils.startAnimation(view, R.anim.reader_listview_row);
        }
    }

    private static class PostViewHolder {
        TextView txtTitle;
        TextView txtText;
        TextView txtBlogName;
        TextView txtDate;
        TextView txtFollow;

        TextView txtLikeCount;
        TextView txtCommentCount;

        ImageView imgBtnLike;
        ImageView imgBtnComment;
        ImageView imgBtnReblog;

        WPNetworkImageView imgFeatured;
        WPNetworkImageView imgAvatar;
    }

    /*
     * triggered when user taps the like button (textView)
     */
    public void toggleLike(PostViewHolder holder, int position, ReaderPost post) {
        // start animation immediately so user knows they did something
        ReaderAniUtils.zoomAction(holder.imgBtnLike);

        if (!ReaderPostActions.performPostAction(holder.imgBtnLike.getContext(), ReaderPostActions.PostAction.TOGGLE_LIKE, post, null))
            return;

        // update post in array and on screen
        ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId);
        mPosts.set(position, updatedPost);
        showLikeStatus(holder.imgBtnLike, updatedPost.isLikedByCurrentUser);
        showCounts(holder, post);
    }

    private void showLikeStatus(ImageView imgBtnLike, boolean isLikedByCurrentUser) {
        if (isLikedByCurrentUser != imgBtnLike.isSelected())
            imgBtnLike.setSelected(isLikedByCurrentUser);
    }

    private void showReblogStatus(ImageView imgBtnReblog, boolean isRebloggedByCurrentUser) {
        if (isRebloggedByCurrentUser != imgBtnReblog.isSelected())
            imgBtnReblog.setSelected(isRebloggedByCurrentUser);
        if (isRebloggedByCurrentUser)
            imgBtnReblog.setOnClickListener(null);
    }

    private void toggleFollow(PostViewHolder holder, int position, ReaderPost post) {
        ReaderAniUtils.zoomAction(holder.txtFollow);

        if (!ReaderPostActions.performPostAction(holder.imgBtnLike.getContext(), ReaderPostActions.PostAction.TOGGLE_FOLLOW, post, null))
            return;

        ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId);
        mPosts.set(position, updatedPost);
        showFollowStatus(holder.txtFollow, updatedPost.isFollowedByCurrentUser);
    }

    private void showFollowStatus(TextView txtFollow, boolean isFollowedByCurrentUser) {
        txtFollow.setText(isFollowedByCurrentUser ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
        txtFollow.setTextColor(isFollowedByCurrentUser ? mColorFollowing : mColorFollow);
    }

    /*
     * AsyncTask to load desired posts
     */
    private boolean mIsTaskRunning = false;
    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderPostList tmpPosts;
        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            tmpPosts = ReaderPostTable.getPostsWithTag(mCurrentTag, Constants.READER_MAX_POSTS_TO_DISPLAY);
            if (mPosts.isSameList(tmpPosts))
                return false;

            // if we're not already displaying the max # posts, enable requesting more when
            // the user scrolls to the end of the list
            mCanRequestMorePosts = (ReaderPostTable.getNumPostsWithTag(mCurrentTag) < Constants.READER_MAX_POSTS_TO_DISPLAY);

            // pre-load avatars, featured images and pubDates in each post - these values are all
            // cached by the post after the first time they're computed, so calling these getters
            // here ensures the values are immediately available when called from getView()
            for (ReaderPost post: tmpPosts) {
                post.getPostAvatarForDisplay(mAvatarSz);
                post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight);
                // not used directly by getView(), but is used by post.getSource() which getView() uses
                post.getDatePublished();
            }

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mPosts = (ReaderPostList)(tmpPosts.clone());
                notifyDataSetChanged();
            }

            if (mDataLoadedListener!=null)
                mDataLoadedListener.onDataLoaded(isEmpty());

            mIsTaskRunning = false;
        }
    }

    /**
     * pre-loads featured images so they're cached before posts are displayed
     */
    /*private class ImagePreloader {
        private int currentIndex = -1;
        private ArrayList<String> imageUrls = new ArrayList<String>();

        ImagePreloader(final ReaderPostList posts) {
            if (posts != null) {
                for (ReaderPost post: posts) {
                    if (post.hasFeaturedImage())
                        imageUrls.add(post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight));
                }
            }
        }

        void start() {
            preloadNext();
        }

        void preloadNext() {
            currentIndex++;
            if (currentIndex >= imageUrls.size())
                return;
            WordPress.imageLoader.get(imageUrls.get(currentIndex), imagePreloadListener);
        }

        ImageLoader.ImageListener imagePreloadListener = new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean isImmediate) {
                if (imageContainer != null)
                    ReaderLog.i("preloaded (isImmediate=" + isImmediate + ") - " + imageContainer.getRequestUrl());
                preloadNext();
            }
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.e(volleyError);
                preloadNext();
            }
        };
    }*/
}
