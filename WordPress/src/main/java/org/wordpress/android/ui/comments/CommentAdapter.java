package org.wordpress.android.ui.comments;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

public class CommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface OnDataLoadedListener {
        void onDataLoaded(boolean isEmpty);
    }

    interface OnLoadMoreListener {
        void onLoadMore();
    }

    interface OnSelectedItemsChangeListener {
        void onSelectedItemsChanged();
    }

    interface OnCommentPressedListener {
        void onCommentPressed(int position, View view);

        void onCommentLongPressed(int position, View view);
    }

    private final LayoutInflater mInflater;
    private final Context mContext;

    private final CommentList mComments = new CommentList();
    private final HashSet<Integer> mSelectedPositions = new HashSet<>();

    private final int mStatusColorSpam;
    private final int mStatusColorUnapproved;

    private int mSelectedItemBackground;

    private final int mAvatarSz;
    private final String mStatusTextSpam;
    private final String mStatusTextUnapproved;

    private OnDataLoadedListener mOnDataLoadedListener;
    private OnCommentPressedListener mOnCommentPressedListener;
    private OnLoadMoreListener mOnLoadMoreListener;
    private OnSelectedItemsChangeListener mOnSelectedChangeListener;

    private boolean mEnableSelection;

    private SiteModel mSite;

    @Inject CommentStore mCommentStore;
    @Inject ImageManager mImageManager;

    class CommentHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final TextView mTxtTitle;
        private final TextView mTxtComment;
        private final TextView mTxtStatus;
        private final TextView mTxtDate;
        private final ImageView mImgAvatar;
        private final ImageView mImgCheckmark;
        private final ViewGroup mContainerView;

        CommentHolder(View view) {
            super(view);
            mTxtTitle = view.findViewById(R.id.title);
            mTxtComment = view.findViewById(R.id.comment);
            mTxtStatus = view.findViewById(R.id.status);
            mTxtDate = view.findViewById(R.id.text_date);
            mImgCheckmark = view.findViewById(R.id.image_checkmark);
            mImgAvatar = view.findViewById(R.id.avatar);
            mContainerView = view.findViewById(R.id.layout_container);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            ViewUtilsKt.redirectContextClickToLongPressListener(itemView);
        }

        @Override
        public void onClick(View v) {
            if (mOnCommentPressedListener != null) {
                mOnCommentPressedListener.onCommentPressed(getAdapterPosition(), v);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mOnCommentPressedListener != null) {
                mOnCommentPressedListener.onCommentLongPressed(getAdapterPosition(), v);
            }
            return true;
        }
    }

    CommentAdapter(Context context, SiteModel site) {
        ((WordPress) context.getApplicationContext()).component().inject(this);

        mInflater = LayoutInflater.from(context);
        mContext = context;

        mSite = site;

        mStatusColorSpam = ContextExtensionsKt.getColorFromAttribute(context, R.attr.wpColorError);
        mStatusColorUnapproved = ContextExtensionsKt.getColorFromAttribute(context, R.attr.wpColorWarningDark);


        mStatusTextSpam = context.getResources().getString(R.string.comment_status_spam);
        mStatusTextUnapproved = context.getResources().getString(R.string.comment_status_unapproved);

        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);

        mSelectedItemBackground = ColorUtils
                .setAlphaComponent(ContextExtensionsKt.getColorFromAttribute(context, R.attr.colorOnSurface),
                        context.getResources().getInteger(R.integer.selected_list_item_opacity));

        setHasStableIds(true);
    }

    void setOnDataLoadedListener(OnDataLoadedListener listener) {
        mOnDataLoadedListener = listener;
    }

    void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }

    void setOnCommentPressedListener(OnCommentPressedListener listener) {
        mOnCommentPressedListener = listener;
    }

    void setOnSelectedItemsChangeListener(OnSelectedItemsChangeListener listener) {
        mOnSelectedChangeListener = listener;
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        //noinspection InflateParams
        View view = mInflater.inflate(R.layout.comment_listitem, null);
        CommentHolder holder = new CommentHolder(view);
        view.setTag(holder);
        return holder;
    }

    private String getFormattedTitle(CommentModel comment) {
        String formattedTitle;
        Context context = WordPress.getContext();

        String author = context.getString(R.string.anonymous);
        if (!TextUtils.isEmpty(comment.getAuthorName())) {
            author = comment.getAuthorName().trim();
        }

        if (!TextUtils.isEmpty(comment.getPostTitle())) {
            formattedTitle = context.getString(R.string.comment_title, author, comment.getPostTitle().trim());
        } else {
            formattedTitle = author;
        }
        return formattedTitle;
    }

    private String getAvatarForDisplay(CommentModel comment, int avatarSize) {
        String avatarForDisplay = "";
        if (!TextUtils.isEmpty(comment.getAuthorProfileImageUrl())) {
            avatarForDisplay = GravatarUtils.fixGravatarUrl(comment.getAuthorProfileImageUrl(), avatarSize);
        } else if (!TextUtils.isEmpty(comment.getAuthorEmail())) {
            avatarForDisplay = GravatarUtils.gravatarFromEmail(comment.getAuthorEmail(), avatarSize);
        }
        return avatarForDisplay;
    }

    private Spanned getSpannedContent(CommentModel comment) {
        String content = StringUtils.notNullStr(comment.getContent());
        return WPHtml.fromHtml(content, null, null, mContext, null, 0);
    }

    private String getFormattedDate(CommentModel comment, Context context) {
        if (comment.getDatePublished() != null) {
            return DateTimeUtils.javaDateToTimeSpan(DateTimeUtils.dateFromIso8601(comment.getDatePublished()), context);
        }
        return "";
    }

    @Override
    public void onBindViewHolder(@NotNull RecyclerView.ViewHolder viewHolder, int position) {
        CommentModel comment = mComments.get(position);
        CommentHolder holder = (CommentHolder) viewHolder;

        // Note: following operation can take some time, we could maybe cache the calculated objects (title, spanned
        // content) to make the list scroll smoother.
        holder.mTxtTitle.setText(Html.fromHtml(getFormattedTitle(comment)));
        holder.mTxtComment.setText(getSpannedContent(comment));
        holder.mTxtDate.setText(getFormattedDate(comment, mContext));

        // status is only shown for comments that haven't been approved
        final boolean showStatus;
        CommentStatus commentStatus = CommentStatus.fromString(comment.getStatus());
        switch (commentStatus) {
            case SPAM:
                showStatus = true;
                holder.mTxtStatus.setText(mStatusTextSpam);
                holder.mTxtStatus.setTextColor(mStatusColorSpam);
                break;
            case UNAPPROVED:
                showStatus = true;
                holder.mTxtStatus.setText(mStatusTextUnapproved);
                holder.mTxtStatus.setTextColor(mStatusColorUnapproved);
                break;
            default:
                showStatus = false;
                break;
        }
        holder.mTxtStatus.setVisibility(showStatus ? View.VISIBLE : View.GONE);

        int checkmarkVisibility;
        if (mEnableSelection && isItemSelected(position)) {
            checkmarkVisibility = View.VISIBLE;
            mImageManager.cancelRequestAndClearImageView(holder.mImgAvatar);
            holder.mContainerView.setBackgroundColor(mSelectedItemBackground);
        } else {
            checkmarkVisibility = View.GONE;
            mImageManager.loadIntoCircle(holder.mImgAvatar, ImageType.AVATAR_WITH_BACKGROUND,
                    getAvatarForDisplay(comment, mAvatarSz));
            holder.mContainerView.setBackground(null);
        }

        if (holder.mImgCheckmark.getVisibility() != checkmarkVisibility) {
            holder.mImgCheckmark.setVisibility(checkmarkVisibility);
        }

        // comment text needs to be to the left of date/status when the title is a single line and
        // the status is displayed or else the status may overlap the comment text - note that
        // getLineCount() will return 0 if the view hasn't been rendered yet, which is why we
        // check getLineCount() <= 1
        boolean adjustComment = (showStatus && holder.mTxtTitle.getLineCount() <= 1);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.mTxtComment.getLayoutParams();
        if (adjustComment) {
            params.addRule(RelativeLayout.LEFT_OF, R.id.layout_date_status);
        } else {
            params.addRule(RelativeLayout.LEFT_OF, 0);
        }

        // request to load more comments when we near the end
        if (mOnLoadMoreListener != null && position >= getItemCount() - 1
            && position >= CommentsListFragment.COMMENTS_PER_PAGE - 1) {
            mOnLoadMoreListener.onLoadMore();
        }
    }

    public CommentModel getItem(int position) {
        if (isPositionValid(position)) {
            return mComments.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return mComments.get(position).getRemoteCommentId();
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    private boolean isEmpty() {
        return getItemCount() == 0;
    }

    void setEnableSelection(boolean enable) {
        if (enable == mEnableSelection) {
            return;
        }

        mEnableSelection = enable;
        if (mEnableSelection) {
            notifyDataSetChanged();
        } else {
            clearSelectedComments();
        }
    }

    void clearSelectedComments() {
        if (mSelectedPositions.size() > 0) {
            mSelectedPositions.clear();
            notifyDataSetChanged();
            if (mOnSelectedChangeListener != null) {
                mOnSelectedChangeListener.onSelectedItemsChanged();
            }
        }
    }

    int getSelectedCommentCount() {
        return mSelectedPositions.size();
    }

    CommentList getSelectedComments() {
        CommentList comments = new CommentList();
        if (!mEnableSelection) {
            return comments;
        }

        for (Integer position : mSelectedPositions) {
            if (isPositionValid(position)) {
                comments.add(mComments.get(position));
            }
        }

        return comments;
    }

    private boolean isItemSelected(int position) {
        return mSelectedPositions.contains(position);
    }

    void setItemSelected(int position, boolean isSelected, View view) {
        if (isItemSelected(position) == isSelected) {
            return;
        }

        if (isSelected) {
            mSelectedPositions.add(position);
        } else {
            mSelectedPositions.remove(position);
        }

        notifyItemChanged(position);

        if (view != null && view.getTag() instanceof CommentHolder) {
            CommentHolder holder = (CommentHolder) view.getTag();
            // animate the selection change
            AniUtils.startAnimation(holder.mImgCheckmark, isSelected ? R.anim.cab_select : R.anim.cab_deselect);
            holder.mImgCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }

        if (mOnSelectedChangeListener != null) {
            mOnSelectedChangeListener.onSelectedItemsChanged();
        }
    }

    void toggleItemSelected(int position, View view) {
        setItemSelected(position, !isItemSelected(position), view);
    }

    private int indexOfCommentId(long commentId) {
        return mComments.indexOfCommentId(commentId);
    }

    private boolean isPositionValid(int position) {
        return (position >= 0 && position < mComments.size());
    }

    public void removeComment(CommentModel comment) {
        int position = indexOfCommentId(comment.getRemoteCommentId());
        if (position >= 0) {
            mComments.remove(position);
            notifyItemRemoved(position);
        }
    }

    /*
     * load comments using an AsyncTask
     */
    void loadComments(CommentStatus statusFilter) {
        if (mIsLoadTaskRunning) {
            AppLog.w(AppLog.T.COMMENTS, "load comments task already active");
        } else {
            new LoadCommentsTask(statusFilter).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
     * AsyncTask to load comments from SQLite
     */
    private boolean mIsLoadTaskRunning = false;

    private class LoadCommentsTask extends AsyncTask<Void, Void, Boolean> {
        private CommentList mTmpComments;
        final CommentStatus mStatusFilter;

        LoadCommentsTask(CommentStatus statusFilter) {
            mStatusFilter = statusFilter;
        }

        @Override
        protected void onPreExecute() {
            mIsLoadTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            mIsLoadTaskRunning = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            List<CommentModel> comments;
            if (mStatusFilter == null || mStatusFilter == CommentStatus.ALL) {
                // The "all" filter actually means "approved" + "unapproved" (but not "spam", "trash" or "deleted")
                comments = mCommentStore.getCommentsForSite(mSite, false,
                        CommentStatus.APPROVED, CommentStatus.UNAPPROVED);
            } else {
                comments = mCommentStore.getCommentsForSite(mSite, false, mStatusFilter);
            }

            mTmpComments = new CommentList();
            mTmpComments.addAll(comments);

            return !mComments.isSameList(mTmpComments);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mComments.clear();
                mComments.addAll(mTmpComments);
                // Sort by date
                Collections.sort(mComments, (commentModel, t1) -> {
                    Date d0 = DateTimeUtils.dateFromIso8601(commentModel.getDatePublished());
                    Date d1 = DateTimeUtils.dateFromIso8601(t1.getDatePublished());
                    if (d0 == null || d1 == null) {
                        return 0;
                    }
                    return d1.compareTo(d0);
                });
                notifyDataSetChanged();
            }

            if (mOnDataLoadedListener != null) {
                mOnDataLoadedListener.onDataLoaded(isEmpty());
            }

            mIsLoadTaskRunning = false;
        }
    }
}
