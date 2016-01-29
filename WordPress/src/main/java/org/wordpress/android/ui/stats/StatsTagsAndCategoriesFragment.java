package org.wordpress.android.ui.stats;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.TagModel;
import org.wordpress.android.ui.stats.models.TagsContainerModel;
import org.wordpress.android.ui.stats.models.TagsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class StatsTagsAndCategoriesFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsTagsAndCategoriesFragment.class.getSimpleName();

    private TagsContainerModel mTagsContainer;

    @Override
    protected boolean hasDataAvailable() {
        return mTagsContainer != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (mTagsContainer != null) {
            outState.putSerializable(ARG_REST_RESPONSE, mTagsContainer);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mTagsContainer = (TagsContainerModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.TagsUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mTagsContainer = event.mTagsContainer;
        mGroupIdToExpandedMap.clear();
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mTagsContainer = null;
        mGroupIdToExpandedMap.clear();
        showErrorUI(event.mError);
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (hasTags()) {
            BaseExpandableListAdapter adapter = new MyExpandableListAdapter(getActivity(), getTags());
            StatsUIHelper.reloadGroupViews(getActivity(), adapter, mGroupIdToExpandedMap, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasTags() {
        return mTagsContainer != null
                && mTagsContainer.getTags() != null
                && mTagsContainer.getTags().size() > 0;
    }

    private List<TagsModel> getTags() {
        if (!hasTags()) {
            return new ArrayList<TagsModel>(0);
        }
        return mTagsContainer.getTags();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return hasTags() && getTags().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST;
    }

    @Override
    protected boolean isExpandableList() {
        return true;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.TAGS_AND_CATEGORIES
        };
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_tags_and_categories;
    }
    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }
    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_tags_and_categories;
    }
    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_tags_and_categories_desc;
    }

    private class MyExpandableListAdapter extends BaseExpandableListAdapter {
        public final LayoutInflater inflater;
        private final List<TagsModel> groups;

        public MyExpandableListAdapter(Context context, List<TagsModel> groups) {
            this.groups = groups;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            TagsModel currentGroup = groups.get(groupPosition);
            List<TagModel> results = currentGroup.getTags();
            return results.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            final TagModel children = (TagModel) getChild(groupPosition, childPosition);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(convertView);

                //Make the picture smaller (same size of the chevron) only for tag
                ViewGroup.LayoutParams params = viewHolder.networkImageView.getLayoutParams();
                params.width = DisplayUtils.dpToPx(convertView.getContext(), 12);
                params.height = params.width;
                viewHolder.networkImageView.setLayoutParams(params);

                convertView.setTag(viewHolder);
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            // name, url
            holder.setEntryTextOrLink(children.getLink(), children.getName());

            // totals
            holder.totalsTextView.setText("");

            // icon.
            holder.networkImageView.setVisibility(View.VISIBLE);
            holder.networkImageView.setImageDrawable(getResources().getDrawable(R.drawable.stats_icon_tags));

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            TagsModel currentGroup = groups.get(groupPosition);
            List<TagModel> tags = currentGroup.getTags();
            if (tags == null || tags.size() == 1 ) {
                return 0;
            } else {
                return tags.size();
            }
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groups.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return groups.size();
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
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(convertView);
                convertView.setTag(viewHolder);

                //Make the picture smaller (same size of the chevron) only for tag
                ViewGroup.LayoutParams params = viewHolder.networkImageView.getLayoutParams();
                params.width = DisplayUtils.dpToPx(convertView.getContext(), 12);
                params.height = params.width;
                viewHolder.networkImageView.setLayoutParams(params);
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            TagsModel group = (TagsModel) getGroup(groupPosition);
            StringBuilder groupName = new StringBuilder();
            List<TagModel> tags = group.getTags();
            for (int i = 0; i < tags.size(); i++) {
                TagModel currentTag = tags.get(i);
                groupName.append(currentTag.getName());
                if ( i < (tags.size() - 1)) {
                    groupName.append(" | ");
                }
            }
            int total = group.getViews();
            int children = getChildrenCount(groupPosition);

            if (children > 0) {
                holder.setEntryText(groupName.toString(), getResources().getColor(R.color.stats_link_text_color));
            } else {
                holder.setEntryTextOrLink(tags.get(0).getLink(), groupName.toString());
            }

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(children > 0 ? View.VISIBLE : View.GONE);


            // icon
            if ( children == 0 ) {
                holder.networkImageView.setVisibility(View.VISIBLE);
                int drawableResource = groupName.toString().equalsIgnoreCase("uncategorized") ? R.drawable.stats_icon_categories
                        : R.drawable.stats_icon_tags;
                holder.networkImageView.setImageDrawable(getResources().getDrawable(drawableResource));
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
        return getString(R.string.stats_view_tags_and_categories);
    }
}
