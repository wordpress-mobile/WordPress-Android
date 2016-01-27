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
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;


public class StatsCommentsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsCommentsFragment.class.getSimpleName();
    static final String ARG_REST_RESPONSE_FOLLOWERS = "ARG_REST_RESPONSE_FOLLOWERS";

    private CommentsModel mCommentsModel;
    private CommentFollowersModel mCommentFollowersModel;

    @Override
    protected boolean hasDataAvailable() {
        return mCommentsModel != null && mCommentFollowersModel != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (mCommentsModel != null) {
            outState.putSerializable(ARG_REST_RESPONSE, mCommentsModel);
        }
        if (mCommentFollowersModel != null) {
            outState.putSerializable(ARG_REST_RESPONSE_FOLLOWERS, mCommentFollowersModel);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mCommentsModel = (CommentsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE_FOLLOWERS)) {
            mCommentFollowersModel = (CommentFollowersModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE_FOLLOWERS);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.CommentsUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mCommentsModel = event.mComments;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.CommentFollowersUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mCommentFollowersModel = event.mCommentFollowers;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mCommentsModel = null;
        mCommentFollowersModel = null;
        showErrorUI(event.mError);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Resources res = container.getContext().getResources();
        String[] titles = {
                res.getString(R.string.stats_comments_by_authors),
                res.getString(R.string.stats_comments_by_posts_and_pages),
        };

        setupTopModulePager(inflater, container, view, titles);

        return view;
    }

    @Override
    protected void updateUI() {
        // This module is a kind of exception to the normal way we build page interface.
        // In this module only the first rest endpoint StatsService.StatsEndpointsEnum.COMMENTS
        // is used to populate 99% of the UI even if there is a tab on the top.
        // Switching to a different tab on the UI doesn't switch the underlying datamodel index as in all other modules.

        if (!isAdded()) {
            return;
        }

        if (mCommentsModel == null && mCommentFollowersModel == null) {
            showHideNoResultsUI(true);
            mTotalsLabel.setVisibility(View.GONE);
            mTopPagerContainer.setVisibility(View.GONE);
            return;
        }

        mTopPagerContainer.setVisibility(View.VISIBLE);

        if (mCommentFollowersModel != null) { // check if comment-followers is already here
            mTotalsLabel.setVisibility(View.VISIBLE);
            int totalNumberOfFollowers = mCommentFollowersModel.getTotal();
            String totalCommentsFollowers = getString(R.string.stats_comments_total_comments_followers);
            mTotalsLabel.setText(
                    String.format(totalCommentsFollowers, FormatUtils.formatDecimal(totalNumberOfFollowers))
            );
        }

        ArrayAdapter adapter = null;

        if (mTopPagerSelectedButtonIndex == 0 && hasAuthors()) {
            adapter = new AuthorsAdapter(getActivity(), getAuthors());
        } else if (mTopPagerSelectedButtonIndex == 1 && hasPosts()) {
            adapter = new PostsAndPagesAdapter(getActivity(), getPosts());
        }

        if (adapter != null) {
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasAuthors() {
        return mCommentsModel != null
                && mCommentsModel.getAuthors() != null
                && mCommentsModel.getAuthors().size() > 0;
    }

    private List<AuthorModel> getAuthors() {
        if (!hasAuthors()) {
            return new ArrayList<AuthorModel>(0);
        }
        return mCommentsModel.getAuthors();
    }

    private boolean hasPosts() {
        return  mCommentsModel != null
                && mCommentsModel.getPosts() != null
                && mCommentsModel.getPosts().size() > 0;
    }

    private List<PostModel> getPosts() {
        if (!hasPosts()) {
            return new ArrayList<PostModel>(0);
        }
        return mCommentsModel.getPosts();
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
                rowView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final AuthorModel currentRowData = list.get(position);
            final StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

            // entries
            holder.setEntryText(currentRowData.getName(), getResources().getColor(R.color.stats_text_color));

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getViews()));

            // avatar
            holder.networkImageView.setImageUrl(GravatarUtils.fixGravatarUrl(currentRowData.getAvatar(), mResourceVars.headerAvatarSizePx), WPNetworkImageView.ImageType.AVATAR);
            holder.networkImageView.setVisibility(View.VISIBLE);

            final FollowDataModel followData = currentRowData.getFollowData();
            if (followData == null) {
                holder.imgMore.setVisibility(View.GONE);
                holder.imgMore.setClickable(false);
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
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.COMMENTS, StatsService.StatsEndpointsEnum.COMMENT_FOLLOWERS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_comments);
    }
}
