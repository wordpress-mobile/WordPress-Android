package org.wordpress.android.ui.posts.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.ui.posts.services.PostMediaService;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.PostListButton;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adapter for Posts/Pages list
 */
public class PostsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnPostButtonClickListener {
        void onPostButtonClicked(int buttonId, PostsListPost post);
    }

    private OnLoadMoreListener mOnLoadMoreListener;
    private OnPostsLoadedListener mOnPostsLoadedListener;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnPostButtonClickListener mOnPostButtonClickListener;

    private final int mLocalTableBlogId;
    private final int mPhotonWidth;
    private final int mPhotonHeight;
    private final int mEndlistIndicatorHeight;

    private final boolean mIsPage;
    private final boolean mIsPrivateBlog;
    private final boolean mIsStatsSupported;
    private final boolean mAlwaysShowAllButtons;

    private final PostsListPostList mPosts = new PostsListPostList();
    private final LayoutInflater mLayoutInflater;

    private final List<PostsListPost> mHiddenPosts = new ArrayList<>();

    private static final long ROW_ANIM_DURATION = 150;

    private static final int VIEW_TYPE_POST_OR_PAGE = 0;
    private static final int VIEW_TYPE_ENDLIST_INDICATOR = 1;

    public PostsListAdapter(Context context, @NonNull Blog blog, boolean isPage) {
        mIsPage = isPage;
        mLayoutInflater = LayoutInflater.from(context);

        mLocalTableBlogId = blog.getLocalTableBlogId();
        mIsPrivateBlog = blog.isPrivate();
        mIsStatsSupported = blog.isDotcomFlag() || blog.isJetpackPowered();

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int contentSpacing = context.getResources().getDimensionPixelSize(R.dimen.content_margin);
        mPhotonWidth = displayWidth - (contentSpacing * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);

        // endlist indicator height is hard-coded here so that its horz line is in the middle of the fab
        mEndlistIndicatorHeight = DisplayUtils.dpToPx(context, mIsPage ? 82 : 74);

        // on larger displays we can always show all buttons
        mAlwaysShowAllButtons = (displayWidth >= 1080);
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }

    public void setOnPostsLoadedListener(OnPostsLoadedListener listener) {
        mOnPostsLoadedListener = listener;
    }

    public void setOnPostSelectedListener(OnPostSelectedListener listener) {
        mOnPostSelectedListener = listener;
    }

    public void setOnPostButtonClickListener(OnPostButtonClickListener listener) {
        mOnPostButtonClickListener = listener;
    }

    private PostsListPost getItem(int position) {
        if (isValidPostPosition(position)) {
            return mPosts.get(position);
        }
        return null;
    }

    private boolean isValidPostPosition(int position) {
        return (position >= 0 && position < mPosts.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mPosts.size()) {
            return VIEW_TYPE_ENDLIST_INDICATOR;
        }
        return VIEW_TYPE_POST_OR_PAGE;
    }

    @Override
    public int getItemCount() {
        if (mPosts.size() == 0) {
            return 0;
        } else {
            return mPosts.size() + 1; // +1 for the endlist indicator
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ENDLIST_INDICATOR) {
            View view = mLayoutInflater.inflate(R.layout.endlist_indicator, parent, false);
            view.getLayoutParams().height = mEndlistIndicatorHeight;
            return new EndListViewHolder(view);
        } else if (mIsPage) {
            View view = mLayoutInflater.inflate(R.layout.page_item, parent, false);
            return new PageViewHolder(view);
        } else {
            View view = mLayoutInflater.inflate(R.layout.post_cardview, parent, false);
            return new PostViewHolder(view);
        }
    }

    private boolean canShowStatsForPost(PostsListPost post) {
        return mIsStatsSupported
                && post.getStatusEnum() == PostStatus.PUBLISHED
                && !post.isLocalDraft();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        // nothing to do if this is the static endlist indicator
        if (getItemViewType(position) == VIEW_TYPE_ENDLIST_INDICATOR) {
            return;
        }

        final PostsListPost post = mPosts.get(position);
        Context context = holder.itemView.getContext();

        if (holder instanceof PostViewHolder) {
            PostViewHolder postHolder = (PostViewHolder) holder;

            if (post.hasTitle()) {
                postHolder.txtTitle.setText(post.getTitle());
            } else {
                postHolder.txtTitle.setText("(" + context.getResources().getText(R.string.untitled) + ")");
            }

            if (post.hasExcerpt()) {
                postHolder.txtExcerpt.setVisibility(View.VISIBLE);
                postHolder.txtExcerpt.setText(post.getExcerpt());
            } else {
                postHolder.txtExcerpt.setVisibility(View.GONE);
            }

            if (post.hasFeaturedImageId() || post.hasFeaturedImageUrl()) {
                postHolder.imgFeatured.setVisibility(View.VISIBLE);
                postHolder.imgFeatured.setImageUrl(post.getFeaturedImageUrl(), WPNetworkImageView.ImageType.PHOTO);
            } else {
                postHolder.imgFeatured.setVisibility(View.GONE);
            }

            // local drafts say "delete" instead of "trash"
            if (post.isLocalDraft()) {
                postHolder.txtDate.setVisibility(View.GONE);
                postHolder.btnTrash.setButtonType(PostListButton.BUTTON_DELETE);
            } else {
                postHolder.txtDate.setText(post.getFormattedDate());
                postHolder.txtDate.setVisibility(View.VISIBLE);
                postHolder.btnTrash.setButtonType(PostListButton.BUTTON_TRASH);
            }

            updateStatusText(postHolder.txtStatus, post);
            configurePostButtons(postHolder, post);
        } else if (holder instanceof PageViewHolder) {
            PageViewHolder pageHolder = (PageViewHolder) holder;
            if (post.hasTitle()) {
                pageHolder.txtTitle.setText(post.getTitle());
            } else {
                pageHolder.txtTitle.setText("(" + context.getResources().getText(R.string.untitled) + ")");
            }

            String dateStr = getPageDateHeaderText(context, post);
            pageHolder.txtDate.setText(dateStr);

            updateStatusText(pageHolder.txtStatus, post);

            // don't show date header if same as previous
            boolean showDate;
            if (position > 0) {
                String prevDateStr = getPageDateHeaderText(context, mPosts.get(position - 1));
                showDate = !prevDateStr.equals(dateStr);
            } else {
                showDate = true;
            }
            pageHolder.dateHeader.setVisibility(showDate ? View.VISIBLE : View.GONE);

            // no "..." more button when uploading
            pageHolder.btnMore.setVisibility(post.isUploading() ? View.GONE : View.VISIBLE);
            pageHolder.btnMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPagePopupMenu(v, post);
                }
            });

            // only show the top divider for the first item
            pageHolder.dividerTop.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        }

        // load more posts when we near the end
        if (mOnLoadMoreListener != null && position >= mPosts.size() - 1
                && position >= PostsListFragment.POSTS_REQUEST_COUNT - 1) {
            mOnLoadMoreListener.onLoadMore();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostsListPost selectedPost = getItem(position);
                if (mOnPostSelectedListener != null && selectedPost != null) {
                    mOnPostSelectedListener.onPostSelected(selectedPost);
                }
            }
        });
    }

    /*
     * returns the caption to show in the date header for the passed page - pages with the same
     * caption will be grouped together
     *  - if page is local draft, returns "Local draft"
     *  - if page is scheduled, returns formatted date w/o time
     *  - if created today or yesterday, returns "Today" or "Yesterday"
     *  - if created this month, returns the number of days ago
     *  - if created this year, returns the month name
     *  - if created before this year, returns the month name with year
     */
    private static String getPageDateHeaderText(Context context, PostsListPost page) {
        if (page.isLocalDraft()) {
            return context.getString(R.string.local_draft);
        } else if (page.getStatusEnum() == PostStatus.SCHEDULED) {
            return DateUtils.formatDateTime(context, page.getDateCreatedGmt(), DateUtils.FORMAT_ABBREV_ALL);
        } else {
            Date dtCreated = new Date(page.getDateCreatedGmt());
            Date dtNow = DateTimeUtils.nowUTC();
            int daysBetween = DateTimeUtils.daysBetween(dtCreated, dtNow);
            if (daysBetween == 0) {
                return context.getString(R.string.today);
            } else if (daysBetween == 1) {
                return context.getString(R.string.yesterday);
            } else if (DateTimeUtils.isSameMonthAndYear(dtCreated, dtNow)) {
                return String.format(context.getString(R.string.days_ago), daysBetween);
            } else if (DateTimeUtils.isSameYear(dtCreated, dtNow)) {
                return new SimpleDateFormat("MMMM").format(dtCreated);
            } else {
                return new SimpleDateFormat("MMMM yyyy").format(dtCreated);
            }
        }
    }

    /*
     * user tapped "..." next to a page, show a popup menu of choices
     */
    private void showPagePopupMenu(View view, final PostsListPost page) {
        Context context = view.getContext();
        final ListPopupWindow listPopup = new ListPopupWindow(context);
        listPopup.setAnchorView(view);

        // width is a multiple of 56dp - https://www.google.com/design/spec/components/menus.html#menus-simple-menus
        listPopup.setWidth(DisplayUtils.dpToPx(context, 168));
        listPopup.setModal(true);
        listPopup.setAdapter(new PageMenuAdapter(context, page));
        listPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listPopup.dismiss();
                if (mOnPostButtonClickListener != null) {
                    int buttonId = (int) id;
                    mOnPostButtonClickListener.onPostButtonClicked(buttonId, page);
                }
            }
        });
        listPopup.show();
    }

    private void updateStatusText(TextView txtStatus, PostsListPost post) {
        if ((post.getStatusEnum() == PostStatus.PUBLISHED) && !post.isLocalDraft() && !post.hasLocalChanges()) {
            txtStatus.setVisibility(View.GONE);
        } else {
            int statusTextResId = 0;
            int statusIconResId = 0;
            int statusColorResId = R.color.grey_darken_10;

            if (post.isUploading()) {
                statusTextResId = R.string.post_uploading;
                statusColorResId = R.color.alert_yellow;
            } else if (post.isLocalDraft()) {
                statusTextResId = R.string.local_draft;
                statusIconResId = R.drawable.noticon_scheduled;
                statusColorResId = R.color.alert_yellow;
            } else if (post.hasLocalChanges()) {
                statusTextResId = R.string.local_changes;
                statusIconResId = R.drawable.noticon_scheduled;
                statusColorResId = R.color.alert_yellow;
            } else {
                switch (post.getStatusEnum()) {
                    case DRAFT:
                        statusTextResId = R.string.draft;
                        statusIconResId = R.drawable.noticon_scheduled;
                        statusColorResId = R.color.alert_yellow;
                        break;
                    case PRIVATE:
                        statusTextResId = R.string.post_private;
                        break;
                    case PENDING:
                        statusTextResId = R.string.pending_review;
                        statusIconResId = R.drawable.noticon_scheduled;
                        statusColorResId = R.color.alert_yellow;
                        break;
                    case SCHEDULED:
                        statusTextResId = R.string.scheduled;
                        statusIconResId = R.drawable.noticon_scheduled;
                        statusColorResId = R.color.alert_yellow;
                        break;
                    case TRASHED:
                        statusTextResId = R.string.trashed;
                        statusIconResId = R.drawable.noticon_trashed;
                        statusColorResId = R.color.alert_red;
                        break;
                }
            }

            Resources resources = txtStatus.getContext().getResources();
            txtStatus.setTextColor(resources.getColor(statusColorResId));
            txtStatus.setText(statusTextResId != 0 ? resources.getString(statusTextResId) : "");
            Drawable drawable = (statusIconResId != 0 ? resources.getDrawable(statusIconResId) : null);
            txtStatus.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            txtStatus.setVisibility(View.VISIBLE);
        }
    }

    private void configurePostButtons(final PostViewHolder holder,
                                      final PostsListPost post) {
        // posts with local changes have preview rather than view button
        if (post.isLocalDraft() || post.hasLocalChanges()) {
            holder.btnView.setButtonType(PostListButton.BUTTON_PREVIEW);
        } else {
            holder.btnView.setButtonType(PostListButton.BUTTON_VIEW);
        }

        boolean canShowStatsButton = canShowStatsForPost(post);
        int numVisibleButtons = (canShowStatsButton ? 4 : 3);

        // edit / view are always visible
        holder.btnEdit.setVisibility(View.VISIBLE);
        holder.btnView.setVisibility(View.VISIBLE);

        // if we have enough room to show all buttons, hide the back/more buttons and show stats/trash
        if (mAlwaysShowAllButtons || numVisibleButtons <= 3) {
            holder.btnMore.setVisibility(View.GONE);
            holder.btnBack.setVisibility(View.GONE);
            holder.btnTrash.setVisibility(View.VISIBLE);
            holder.btnStats.setVisibility(canShowStatsButton ? View.VISIBLE : View.GONE);
        } else {
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.btnBack.setVisibility(View.GONE);
            holder.btnTrash.setVisibility(View.GONE);
            holder.btnStats.setVisibility(View.GONE);
        }

        View.OnClickListener btnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // handle back/more here, pass other actions to activity/fragment
                int buttonType = ((PostListButton) view).getButtonType();
                switch (buttonType) {
                    case PostListButton.BUTTON_MORE:
                        animateButtonRows(holder, post, false);
                        break;
                    case PostListButton.BUTTON_BACK:
                        animateButtonRows(holder, post, true);
                        break;
                    default:
                        if (mOnPostButtonClickListener != null) {
                            mOnPostButtonClickListener.onPostButtonClicked(buttonType, post);
                        }
                        break;
                }
            }
        };
        holder.btnEdit.setOnClickListener(btnClickListener);
        holder.btnView.setOnClickListener(btnClickListener);
        holder.btnStats.setOnClickListener(btnClickListener);
        holder.btnTrash.setOnClickListener(btnClickListener);
        holder.btnMore.setOnClickListener(btnClickListener);
        holder.btnBack.setOnClickListener(btnClickListener);
    }

    /*
     * buttons may appear in two rows depending on display size and number of visible
     * buttons - these rows are toggled through the "more" and "back" buttons - this
     * routine is used to animate the new row in and the old row out
     */
    private void animateButtonRows(final PostViewHolder holder,
                                   final PostsListPost post,
                                   final boolean showRow1) {
        // first animate out the button row, then show/hide the appropriate buttons,
        // then animate the row layout back in
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f);
        ObjectAnimator animOut = ObjectAnimator.ofPropertyValuesHolder(holder.layoutButtons, scaleX, scaleY);
        animOut.setDuration(ROW_ANIM_DURATION);
        animOut.setInterpolator(new AccelerateInterpolator());

        animOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // row 1
                holder.btnEdit.setVisibility(showRow1 ? View.VISIBLE : View.GONE);
                holder.btnView.setVisibility(showRow1 ? View.VISIBLE : View.GONE);
                holder.btnMore.setVisibility(showRow1 ? View.VISIBLE : View.GONE);
                // row 2
                holder.btnStats.setVisibility(!showRow1 && canShowStatsForPost(post) ? View.VISIBLE : View.GONE);
                holder.btnTrash.setVisibility(!showRow1 ? View.VISIBLE : View.GONE);
                holder.btnBack.setVisibility(!showRow1 ? View.VISIBLE : View.GONE);

                PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f);
                PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f);
                ObjectAnimator animIn = ObjectAnimator.ofPropertyValuesHolder(holder.layoutButtons, scaleX, scaleY);
                animIn.setDuration(ROW_ANIM_DURATION);
                animIn.setInterpolator(new DecelerateInterpolator());
                animIn.start();
            }
        });

        animOut.start();
    }

    public void loadPosts() {
        new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /*
     * hides the post - used when the post is trashed by the user but the network request
     * to delete the post hasn't completed yet
     */
    public void hidePost(PostsListPost post) {
        mHiddenPosts.add(post);

        int position = mPosts.indexOfPost(post);
        if (position > -1) {
            mPosts.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void unhidePost(PostsListPost post) {
        if (mHiddenPosts.remove(post)) {
            loadPosts();
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public interface OnPostSelectedListener {
        void onPostSelected(PostsListPost post);
    }

    public interface OnPostsLoadedListener {
        void onPostsLoaded(int postCount);
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final TextView txtExcerpt;
        private final TextView txtDate;
        private final TextView txtStatus;

        private final PostListButton btnEdit;
        private final PostListButton btnView;
        private final PostListButton btnMore;

        private final PostListButton btnStats;
        private final PostListButton btnTrash;
        private final PostListButton btnBack;

        private final WPNetworkImageView imgFeatured;
        private final ViewGroup layoutButtons;

        public PostViewHolder(View view) {
            super(view);

            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtExcerpt = (TextView) view.findViewById(R.id.text_excerpt);
            txtDate = (TextView) view.findViewById(R.id.text_date);
            txtStatus = (TextView) view.findViewById(R.id.text_status);

            btnEdit = (PostListButton) view.findViewById(R.id.btn_edit);
            btnView = (PostListButton) view.findViewById(R.id.btn_view);
            btnMore = (PostListButton) view.findViewById(R.id.btn_more);

            btnStats = (PostListButton) view.findViewById(R.id.btn_stats);
            btnTrash = (PostListButton) view.findViewById(R.id.btn_trash);
            btnBack = (PostListButton) view.findViewById(R.id.btn_back);

            imgFeatured = (WPNetworkImageView) view.findViewById(R.id.image_featured);
            layoutButtons = (ViewGroup) view.findViewById(R.id.layout_buttons);
        }
    }

    class PageViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final TextView txtDate;
        private final TextView txtStatus;
        private final ViewGroup dateHeader;
        private final View btnMore;
        private final View dividerTop;

        public PageViewHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtStatus = (TextView) view.findViewById(R.id.text_status);
            btnMore = view.findViewById(R.id.btn_more);
            dividerTop = view.findViewById(R.id.divider_top);
            dateHeader = (ViewGroup) view.findViewById(R.id.header_date);
            txtDate = (TextView) dateHeader.findViewById(R.id.text_date);
        }
    }

    class EndListViewHolder extends RecyclerView.ViewHolder {
        public EndListViewHolder(View view) {
            super(view);
        }
    }

    /*
     * called after the media (featured image) for a post has been downloaded - locate the post
     * and set its featured image url to the passed url
     */
    public void mediaUpdated(long mediaId, String mediaUrl) {
        int position = mPosts.indexOfFeaturedMediaId(mediaId);
        if (isValidPostPosition(position)) {
            mPosts.get(position).setFeaturedImageUrl(mediaUrl);
            notifyItemChanged(position);
        }
    }

    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean> {
        private PostsListPostList tmpPosts;
        private final ArrayList<Long> mediaIdsToUpdate = new ArrayList<>();

        @Override
        protected Boolean doInBackground(Void... nada) {
            tmpPosts = WordPress.wpDB.getPostsListPosts(mLocalTableBlogId, mIsPage);

            // make sure we don't return any hidden posts
            for (PostsListPost hiddenPost : mHiddenPosts) {
                tmpPosts.remove(hiddenPost);
            }

            // go no further if existing post list is the same
            if (mPosts.isSameList(tmpPosts)) {
                return false;
            }

            // generate the featured image url for each post
            String imageUrl;
            for (PostsListPost post : tmpPosts) {
                if (post.isLocalDraft()) {
                    imageUrl = null;
                } else if (post.getFeaturedImageId() != 0) {
                    imageUrl = WordPress.wpDB.getMediaThumbnailUrl(mLocalTableBlogId, post.getFeaturedImageId());
                    // if the imageUrl isn't found it means the featured image info hasn't been added to
                    // the local media library yet, so add to the list of media IDs to request info for
                    if (TextUtils.isEmpty(imageUrl)) {
                        mediaIdsToUpdate.add(post.getFeaturedImageId());
                    }
                } else if (post.hasDescription()) {
                    ReaderImageScanner scanner = new ReaderImageScanner(post.getDescription(), mIsPrivateBlog);
                    imageUrl = scanner.getLargestImage();
                } else {
                    imageUrl = null;
                }

                if (!TextUtils.isEmpty(imageUrl)) {
                    post.setFeaturedImageUrl(
                            ReaderUtils.getResizedImageUrl(
                                    imageUrl,
                                    mPhotonWidth,
                                    mPhotonHeight,
                                    mIsPrivateBlog));
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mPosts.clear();
                mPosts.addAll(tmpPosts);
                notifyDataSetChanged();

                if (mediaIdsToUpdate.size() > 0) {
                    PostMediaService.startService(WordPress.getContext(), mLocalTableBlogId, mediaIdsToUpdate);
                }
            }

            if (mOnPostsLoadedListener != null) {
                mOnPostsLoadedListener.onPostsLoaded(mPosts.size());
            }
        }
    }
}
