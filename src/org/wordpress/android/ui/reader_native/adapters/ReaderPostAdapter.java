package org.wordpress.android.ui.reader_native.adapters;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.ui.reader_native.ReaderActivityLauncher;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.ui.reader_native.actions.ReaderPostActions;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * Created by nbradbury on 6/27/13.
 * adapter for list of posts in a specific topic
 */
public class ReaderPostAdapter extends BaseAdapter {
    private String mCurrentTopic;

    private int mPhotonWidth;
    private int mPhotonHeight;

    private int mNegativeOffset;
    private int mDefaultOffset;
    private int mAvatarSz;

    private boolean mEnableRowAnimation = true;
    private final float mRowAnimationFromYDelta;
    private final int mRowAnimationDuration;
    private int mPreviousGetViewPosition = -1;
    private boolean mCanRequestMorePosts = false;
    private boolean mIsGridView = false;

    private Context mContext;
    private final LayoutInflater mInflater;
    private ReaderPostList mPosts = new ReaderPostList();

    private ReaderActions.RequestReblogListener mReblogListener;
    private ReaderActions.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;

    public ReaderPostAdapter(Context context,
                             boolean isGridView,
                             ReaderActions.RequestReblogListener reblogListener,
                             ReaderActions.DataLoadedListener dataLoadedListener,
                             ReaderActions.DataRequestedListener dataRequestedListener) {
        super();

        mContext = context.getApplicationContext();
        mInflater = LayoutInflater.from(context);

        mReblogListener = reblogListener;
        mDataLoadedListener = dataLoadedListener;
        mDataRequestedListener = dataRequestedListener;

        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.reader_avatar_sz_medium);
        mIsGridView = isGridView;

        // negative top offset for avatar when both an avatar and featured image exist
        mNegativeOffset = -(mAvatarSz / 2);

        // offset when no featured image
        mDefaultOffset = context.getResources().getDimensionPixelOffset(R.dimen.reader_margin_medium);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int displayHeight = DisplayUtils.getDisplayPixelHeight(context);

        // determine size to use when requesting images via photon - full width unless we're using
        // a grid, in which case half-width since the grid shows two columns
        mPhotonWidth = (mIsGridView ? displayWidth / 2 : displayWidth);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);

        // when animating rows in, start from this y-position near the bottom using medium animation duration
        mRowAnimationFromYDelta = displayHeight - (displayHeight / 6);
        mRowAnimationDuration = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
    }

    public void setPosts(ReaderPostList posts) {
        if (posts==null) {
            mPosts = new ReaderPostList();
        } else {
            mPosts = (ReaderPostList) posts.clone();
        }
        notifyDataSetChanged();
    }

    public void setTopic(String topicName) {
        mCurrentTopic = topicName;
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
        mEnableRowAnimation = isEmpty(); // only animate new rows when grid was previously empty
        loadPosts();
    }

    public void reload() {
        clear();
        mEnableRowAnimation = true;
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
        /*if (debugCnt==0) {
            Debug.startMethodTracing("WPReader");
        } else if (debugCnt==100) {
            Debug.stopMethodTracing();
        }
        debugCnt++;*/

        final ReaderPost post = (ReaderPost) getItem(position);

        //ReaderLog.d("requesting view for post " + Long.toString(post.postId) + ", position " + Integer.toString(position));

        final PostViewHolder holder;
        if (convertView==null) {
            convertView = mInflater.inflate(R.layout.listitem_reader_post_excerpt, parent, false);
            holder = new PostViewHolder();

            holder.txtTitle = (TextView) convertView.findViewById(R.id.text_title);
            holder.txtText = (TextView) convertView.findViewById(R.id.text_excerpt);
            holder.txtSource = (TextView) convertView.findViewById(R.id.text_source);
            holder.txtLikeButton = (TextView) convertView.findViewById(R.id.text_like_button);
            holder.txtReblogButton = (TextView) convertView.findViewById(R.id.text_reblog_button);
            holder.txtCounts = (TextView) convertView.findViewById(R.id.text_counts);
            holder.imgFeatured = (WPNetworkImageView) convertView.findViewById(R.id.image_featured);
            holder.imgAvatar = (WPNetworkImageView) convertView.findViewById(R.id.image_avatar);
            holder.layoutActions = (ViewGroup) convertView.findViewById(R.id.layout_actions);

            convertView.setTag(holder);
        } else {
            holder = (PostViewHolder) convertView.getTag();
        }

        if (post.hasTitle()) {
            holder.txtTitle.setText(post.getTitle());
        } else {
            holder.txtTitle.setText(R.string.reader_untitled_post);
        }

        // post text/excerpt
        if (post.hasExcerpt()) {
            holder.txtText.setVisibility(View.VISIBLE);
            holder.txtText.setText(post.getExcerpt());
        } else {
            holder.txtText.setVisibility(View.GONE);
        }

        // blog name / author name / date
        holder.txtSource.setText(post.getSource());

        // featured image (or video)
        final boolean isFeaturedImageVisible;
        if (post.hasFeaturedImage()) {
            holder.imgFeatured.setImageUrl(post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight), WPNetworkImageView.ImageType.PHOTO);
            holder.imgFeatured.setVisibility(View.VISIBLE);
            isFeaturedImageVisible = true;
        } else if (post.hasFeaturedVideo()) {
            holder.imgFeatured.setVideoUrl(post.postId, post.getFeaturedVideo());
            holder.imgFeatured.setVisibility(View.VISIBLE);
            isFeaturedImageVisible = true;
        } else {
            holder.imgFeatured.setVisibility(View.GONE);
            isFeaturedImageVisible = false;
        }

        // if there's a featured image and an avatar, we want the avatar to partially overlay the featured image
        int topMargin = (isFeaturedImageVisible && post.hasPostAvatar() ? mNegativeOffset : mDefaultOffset);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)holder.imgAvatar.getLayoutParams();
        if (layoutParams.topMargin!=topMargin)
            layoutParams.topMargin = topMargin;

        // avatar
        if (post.hasPostAvatar()) {
            holder.imgAvatar.setImageUrl(post.getPostAvatarForDisplay(mAvatarSz), WPNetworkImageView.ImageType.AVATAR);
            holder.imgAvatar.setVisibility(View.VISIBLE);
        } else {
            holder.imgAvatar.setVisibility(View.GONE);
        }

        // likes, comments & reblogging
        if (post.isWP()) {
            final int pos = position;
            showLikeStatus(holder.txtLikeButton, post.isLikedByCurrentUser);
            holder.txtLikeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleLike(holder, pos, post);
                }
            });

            showReblogStatus(holder.txtReblogButton, post.isRebloggedByCurrentUser);
            if (!post.isRebloggedByCurrentUser && post.isWP()) {
                holder.txtReblogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mReblogListener!=null)
                            mReblogListener.onRequestReblog(post);
                    }
                });
            }

            holder.layoutActions.setVisibility(View.VISIBLE);
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }

        showCounts(holder.txtCounts, post);

        // animate the appearance of this row if it's after the previous position shown by getView()
        // and row animation is enabled
        if (mEnableRowAnimation && position > mPreviousGetViewPosition)
            animateRow(convertView);

        mPreviousGetViewPosition = position;

        // if we're nearing the end of the posts, fire request to load more
        if (mCanRequestMorePosts && mDataRequestedListener!=null && (position >= getCount()-1))
            mDataRequestedListener.onRequestData(ReaderActions.RequestDataAction.LOAD_OLDER);

        return convertView;
    }

    /*
     * shows like & comment count
     */
    private void showCounts(final TextView txtCounts, final ReaderPost post) {
        if (post!=null && (post.numReplies > 0 || post.numLikes > 0)) {
            txtCounts.setText(getLikeAndCommentCounts(post));
            txtCounts.setVisibility(View.VISIBLE);
        } else {
            txtCounts.setVisibility(View.GONE);
        }
    }

    public String getLikeAndCommentCounts(ReaderPost post) {
        if (post==null || (post.numLikes==0 && post.numReplies==0))
            return "";

        String counts;
        if (post.numLikes==1) {
            counts = mContext.getString(R.string.reader_label_like_count_singular);
        } else if (post.numLikes > 1) {
            counts = mContext.getString(R.string.reader_label_like_count_plural, post.numLikes);
        } else {
            counts = "";
        }

        if (post.numReplies > 0) {
            if (post.numLikes > 0)
                counts += ", ";
            if (post.numReplies==1) {
                counts += mContext.getString(R.string.reader_label_comment_count_singular);
            } else {
                counts += mContext.getString(R.string.reader_label_comment_count_plural, post.numReplies);
            }
        }

        return counts;
    }

    public void enableRowAnimation(boolean enable) {
        mEnableRowAnimation = enable;
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
        private TextView txtTitle;
        private TextView txtText;
        private TextView txtSource;
        private TextView txtLikeButton;
        private TextView txtReblogButton;
        private TextView txtCounts;

        private WPNetworkImageView imgFeatured;
        private WPNetworkImageView imgAvatar;

        private ViewGroup layoutActions;
    }

    /*
     * triggered when user taps the like button (textView)
     */
    public void toggleLike(PostViewHolder holder, int position, ReaderPost post) {
        // start animation immediately so user knows they did something
        ReaderAniUtils.zoomAction(holder.txtLikeButton);

        if (!ReaderPostActions.performPostAction(holder.txtLikeButton.getContext(), ReaderPostActions.PostAction.TOGGLE_LIKE, post, null))
            return;

        // update post in array and on screen
        ReaderPost updatedPost = ReaderPostTable.getPost(post.blogId, post.postId);
        mPosts.set(position, updatedPost);
        showLikeStatus(holder.txtLikeButton, updatedPost.isLikedByCurrentUser);
        showCounts(holder.txtCounts, updatedPost);
    }

    private void showLikeStatus(TextView txtLikeButton, boolean isLikedByCurrentUser) {
        if (isLikedByCurrentUser!=txtLikeButton.isSelected()) {
            txtLikeButton.setText(isLikedByCurrentUser ? R.string.reader_btn_unlike : R.string.reader_btn_like);
            txtLikeButton.setSelected(isLikedByCurrentUser);
        }
    }

    private void showReblogStatus(TextView txtReblogButton, boolean isRebloggedByCurrentUser) {
        if (isRebloggedByCurrentUser!=txtReblogButton.isSelected()) {
            txtReblogButton.setSelected(isRebloggedByCurrentUser);
            txtReblogButton.setText(isRebloggedByCurrentUser ? R.string.reader_btn_reblogged : R.string.reader_btn_reblog);
        }
        if (isRebloggedByCurrentUser)
            txtReblogButton.setOnClickListener(null);
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
            tmpPosts = ReaderPostTable.getPostsInTopic(mCurrentTopic, Constants.READER_MAX_POSTS_TO_DISPLAY);
            if (mPosts.isSameList(tmpPosts))
                return false;

            // if we're not already displaying the max # posts, enable requesting more when
            // the user scrolls to the end of the list
            mCanRequestMorePosts = (ReaderPostTable.getNumPostsInTopic(mCurrentTopic) < Constants.READER_MAX_POSTS_PER_TOPIC);

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
                mPreviousGetViewPosition = -1;
                mPosts = (ReaderPostList)(tmpPosts.clone());
                notifyDataSetChanged();
            }

            if (mDataLoadedListener!=null)
                mDataLoadedListener.onDataLoaded(isEmpty());

            mIsTaskRunning = false;
        }
    }
}
