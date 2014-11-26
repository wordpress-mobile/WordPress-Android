package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.TagModel;
import org.wordpress.android.ui.stats.model.TagsContainerModel;
import org.wordpress.android.ui.stats.model.TagsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.List;

public class StatsTagsAndCategoriesFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsTagsAndCategoriesFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (isErrorResponse(0)) {
            showErrorUI(mDatamodels[0]);
            return;
        }
        if (hasTags()) {
            BaseExpandableListAdapter adapter = new MyExpandableListAdapter(getActivity(), getTags());
            StatsUIHelper.reloadGroupViews(getActivity(), adapter, mGroupIdToExpandedMap, mList, getMaxNumberOfItemsToShowInList());
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    private boolean hasTags() {
        return mDatamodels != null && mDatamodels[0] != null
                && ((TagsContainerModel) mDatamodels[0]).getTags() != null
                && (((TagsContainerModel) mDatamodels[0]).getTags()).size() > 0;
    }

    private List<TagsModel> getTags() {
        if (!hasTags()) {
            return null;
        }
        return ((TagsContainerModel) mDatamodels[0]).getTags();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return hasTags() && getTags().size() > 10;
    }

    @Override
    protected boolean isExpandableList() {
        return true;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionToUpdate() {
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
        public LayoutInflater inflater;
        public Activity activity;
        private List<TagsModel> groups;

        public MyExpandableListAdapter(Activity act, List<TagsModel> groups) {
            this.activity = act;
            this.groups = groups;
            this.inflater = act.getLayoutInflater();
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
                convertView.setTag(viewHolder);
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            String name = children.getName();

            // name, url
            holder.entryTextView.setText(name);

            // totals
            holder.totalsTextView.setText("");

            // icon
            holder.networkImageView.setVisibility(View.INVISIBLE);

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            TagsModel currentGroup = groups.get(groupPosition);
            List<TagModel> referrals = currentGroup.getTags();
            if (referrals == null || referrals.size() == 1 ) {
                return 0;
            } else {
                return referrals.size();
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
                convertView.setTag(new StatsViewHolder(convertView));
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

            holder.entryTextView.setText(groupName);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(children > 0 ? View.VISIBLE : View.GONE);
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
