package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.AuthorModel;
import org.wordpress.android.ui.stats.models.AuthorsModel;
import org.wordpress.android.ui.stats.models.FollowDataModel;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

public class StatsAuthorsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsAuthorsFragment.class.getSimpleName();
    private OnAuthorsSectionChangeListener mListener;

    // Container Activity must implement this interface
    public interface OnAuthorsSectionChangeListener {
        public void onAuthorsVisibilityChange(boolean isEmpty);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAuthorsSectionChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAuthorsSectionChangeListener");
        }
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (isErrorResponse()) {
            showErrorUI();
            return;
        }

        if (isDataEmpty()) {
            showHideNoResultsUI(true);
            mListener.onAuthorsVisibilityChange(true); // Hide the authors section if completely empty
            return;
        }

        List<AuthorModel> authors = ((AuthorsModel) mDatamodels[0]).getAuthors();
        // Do not show the authors section if there is one author only
        if (authors == null || authors.size() <= 1) {
            showHideNoResultsUI(true);
            mListener.onAuthorsVisibilityChange(true);
            return;
        }

        BaseExpandableListAdapter adapter = new MyExpandableListAdapter(getActivity(), authors);
        StatsUIHelper.reloadGroupViews(getActivity(), adapter, mGroupIdToExpandedMap, mList, getMaxNumberOfItemsToShowInList());
        showHideNoResultsUI(false);
        mListener.onAuthorsVisibilityChange(false);
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return (!isDataEmpty(0)
                && ((AuthorsModel) mDatamodels[0]).getAuthors() != null
                && ((AuthorsModel) mDatamodels[0]).getAuthors().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST);
    }

    @Override
    protected boolean isExpandableList() {
        return true;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.AUTHORS
        };
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_authors;
    }
    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }
    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_top_authors;
    }
    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_top_authors_desc;
    }

    private class MyExpandableListAdapter extends BaseExpandableListAdapter {
        public LayoutInflater inflater;
        public Activity activity;
        private List<AuthorModel> authors;

        public MyExpandableListAdapter(Activity act, List<AuthorModel> authors) {
            this.activity = act;
            this.authors = authors;
            this.inflater = act.getLayoutInflater();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            AuthorModel currentGroup = authors.get(groupPosition);
            List<PostModel> posts = currentGroup.getPosts();
            return posts.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            final PostModel children = (PostModel) getChild(groupPosition, childPosition);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(convertView);
                convertView.setTag(viewHolder);
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            // The link icon
            holder.showLinkIcon();

            // name, url
            holder.setEntryTextOpenDetailsPage(children);

            // Setup the more button
            holder.setMoreButtonOpenInReader(children);

            // totals
            int total = children.getTotals();
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon
            holder.networkImageView.setVisibility(View.GONE);

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            AuthorModel currentGroup = authors.get(groupPosition);
            List<PostModel> posts = currentGroup.getPosts();
            if (posts == null) {
                return 0;
            } else {
                return posts.size();
            }
        }

        @Override
        public Object getGroup(int groupPosition) {
            return authors.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return authors.size();
        }


        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                convertView.setTag(new StatsViewHolder(convertView));
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            AuthorModel group = (AuthorModel) getGroup(groupPosition);

            String name = group.getName();
            int total = group.getViews();
            String icon = group.getAvatar();
            int children = getChildrenCount(groupPosition);

            holder.setEntryText(name, getResources().getColor(R.color.stats_link_text_color));

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // icon
            //holder.showNetworkImage(icon);
            holder.networkImageView.setImageUrl(GravatarUtils.fixGravatarUrl(icon, mResourceVars.headerAvatarSizePx), WPNetworkImageView.ImageType.AVATAR);
            holder.networkImageView.setVisibility(View.VISIBLE);

            final FollowDataModel followData = group.getFollowData();
            if (followData == null) {
                holder.imgMore.setVisibility(View.GONE);
                holder.imgMore.setClickable(false);
            } else {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FollowHelper fh = new FollowHelper(activity);
                        fh.showPopup(holder.imgMore, followData);
                    }
                });
            }

            if (children == 0) {
                holder.showLinkIcon();
            } else {
                holder.showChevronIcon();
            }

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_authors);
    }
}
