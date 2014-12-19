package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.adapters.PostsAndPagesAdapter;
import org.wordpress.android.ui.stats.models.AuthorModel;
import org.wordpress.android.ui.stats.models.CommentFollowersModel;
import org.wordpress.android.ui.stats.models.CommentsModel;
import org.wordpress.android.ui.stats.models.FollowDataModel;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;


public class StatsCommentsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsCommentsFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Resources res = container.getContext().getResources();
        String[] titles = {
                res.getString(R.string.stats_comments_by_authors),
                res.getString(R.string.stats_comments_by_posts_and_pages),
        };

        setupTopModulePager(inflater, view, titles);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.STATS, this.getTag() + " > restoring instance state");
            if (savedInstanceState.containsKey(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX)) {
                mTopPagerSelectedButtonIndex = savedInstanceState.getInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX);
            }
        } else {
            // first time it's created
            mTopPagerSelectedButtonIndex = getArguments().getInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //AppLog.d(AppLog.T.STATS, this.getTag() + " > saving instance state");
        outState.putInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, mTopPagerSelectedButtonIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        mTopPagerContainer.setVisibility(View.VISIBLE);

        if (mDatamodels == null) {
            showHideNoResultsUI(true);
            mTotalsLabel.setVisibility(View.GONE);
            return;
        }

        if (isErrorResponse()) {
            showErrorUI();
            return;
        }

        if (mDatamodels[1] != null) { // check if comment-followers is already here
            mTotalsLabel.setVisibility(View.VISIBLE);
            int totalNumberOfFollowers = ((CommentFollowersModel) mDatamodels[1]).getTotal();
            mTotalsLabel.setText(
                    getString(
                            R.string.stats_comments_total_comments_followers,
                            FormatUtils.formatDecimal(totalNumberOfFollowers)
                    )
            );
        } else {
            mTotalsLabel.setVisibility(View.GONE);
        }

        ArrayAdapter adapter = null;

        if (mTopPagerSelectedButtonIndex == 0 && hasAuthors()) {
            adapter = new AuthorsAdapter(getActivity(), getAuthors());
        } else if (mTopPagerSelectedButtonIndex == 1 && hasPosts()) {
            adapter = new PostsAndPagesAdapter(getActivity(), getLocalTableBlogID(), getPosts());
        }

        if (adapter != null) {
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasAuthors() {
        return mDatamodels != null && mDatamodels[0] != null
                && ((CommentsModel) mDatamodels[0]).getAuthors() != null
                && ((CommentsModel) mDatamodels[0]).getAuthors().size() > 0;
    }

    private List<AuthorModel> getAuthors() {
        if (!hasAuthors()) {
            return null;
        }
        return ((CommentsModel) mDatamodels[0]).getAuthors();
    }

    private boolean hasPosts() {
        return mDatamodels != null && mDatamodels[0] != null
                && ((CommentsModel) mDatamodels[0]).getPosts() != null
                && ((CommentsModel) mDatamodels[0]).getPosts().size() > 0;
    }

    private List<SingleItemModel> getPosts() {
        if (!hasPosts()) {
            return null;
        }
        return ((CommentsModel) mDatamodels[0]).getPosts();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        if (mTopPagerSelectedButtonIndex == 0 && hasAuthors() && getAuthors().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST) {
            return true;
        } else if (mTopPagerSelectedButtonIndex == 1 && hasPosts() && getPosts().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class AuthorsAdapter extends ArrayAdapter<AuthorModel> {

        private final List<AuthorModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public AuthorsAdapter(Activity context, List<AuthorModel> list) {
            super(context, R.layout.stats_list_cell, list);
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                rowView = inflater.inflate(R.layout.stats_list_cell, null);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final AuthorModel currentRowData = list.get(position);
            final StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

            // entries
            holder.entryTextView.setText(currentRowData.getName());
            holder.entryTextView.setTextColor(context.getResources().getColor(R.color.stats_text_color));
            holder.entryTextView.setOnClickListener(null);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getViews()));

            // avatar
            holder.networkImageView.setImageUrl(PhotonUtils.fixAvatar(currentRowData.getAvatar(), mResourceVars.headerAvatarSizePx), WPNetworkImageView.ImageType.AVATAR);
            holder.networkImageView.setVisibility(View.VISIBLE);

            final FollowDataModel followData = currentRowData.getFollowData();
            if (followData == null) {
                holder.imgMore.setVisibility(View.GONE);
                holder.imgMore.setOnClickListener(null);
            } else {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FollowHelper fh = new FollowHelper(context);
                        fh.showPopup(holder.imgMore, followData);
                    }
                });
            }

            return rowView;
        }
    }

    @Override
    protected int getEntryLabelResId() {
        if (mTopPagerSelectedButtonIndex == 0) {
            return R.string.stats_entry_top_commenter;
        } else {
            return R.string.stats_entry_posts_and_pages;
        }
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_comments;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_comments;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_comments_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.COMMENTS, StatsService.StatsEndpointsEnum.COMMENT_FOLLOWERS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_comments);
    }
}