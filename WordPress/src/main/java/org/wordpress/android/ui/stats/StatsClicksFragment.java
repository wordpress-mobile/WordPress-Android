package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import org.apache.commons.lang.StringUtils;
import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.ClickGroupModel;
import org.wordpress.android.ui.stats.model.ClicksModel;
import org.wordpress.android.ui.stats.model.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.List;

public class StatsClicksFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsClicksFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (isErrorResponse(0)) {
            showErrorUI(mDatamodels[0]);
            return;
        }

        if (mDatamodels != null && mDatamodels[0] != null
                && ((ClicksModel) mDatamodels[0]).getClickGroups().size() > 0) {
            BaseExpandableListAdapter adapter = new MyExpandableListAdapter(getActivity(), ((ClicksModel) mDatamodels[0]).getClickGroups());
            StatsUIHelper.reloadGroupViews(getActivity(), adapter, mGroupIdToExpandedMap, mList, getMaxNumberOfItemsToShowInList());
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return (mDatamodels != null && mDatamodels[0] != null
                && ((ClicksModel) mDatamodels[0]).getClickGroups() != null
                && ((ClicksModel) mDatamodels[0]).getClickGroups().size() > 10);
    }

    @Override
    protected boolean isExpandableList() {
        return true;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.CLICKS
        };
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_clicks_link;
    }
    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_clicks;
    }
    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_clicks_title;
    }
    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_clicks_desc;
    }

    private class MyExpandableListAdapter extends BaseExpandableListAdapter {
        public LayoutInflater inflater;
        public Activity activity;
        private List<ClickGroupModel> clickGroups;

        public MyExpandableListAdapter(Activity act, List<ClickGroupModel> clickGroups) {
            this.activity = act;
            this.clickGroups = clickGroups;
            this.inflater = act.getLayoutInflater();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            ClickGroupModel currentGroup = clickGroups.get(groupPosition);
            List<SingleItemModel> results = currentGroup.getClicks();
            return results.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            final SingleItemModel children = (SingleItemModel) getChild(groupPosition, childPosition);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(convertView);
                convertView.setTag(viewHolder);
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            // name, url
            holder.setEntryTextOrLink(children.getUrl(), children.getTitle());

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(
                    children.getTotals()
            ));

            // no icon, make it invisible so children are indented
            holder.networkImageView.setVisibility(View.INVISIBLE);

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            ClickGroupModel currentGroup = clickGroups.get(groupPosition);
            List<SingleItemModel> clicks = currentGroup.getClicks();
            if (clicks == null) {
                return 0;
            } else {
                return clicks.size();
            }
        }

        @Override
        public Object getGroup(int groupPosition) {
            return clickGroups.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return clickGroups.size();
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

            ClickGroupModel group = (ClickGroupModel) getGroup(groupPosition);

            String name = group.getName();
            int total = group.getViews();
            String url = group.getUrl();
            String icon = group.getIcon();
            int children = getChildrenCount(groupPosition);

            holder.setEntryTextOrLink(url, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // icon
            if (StringUtils.isNotBlank(icon)) {
                holder.showNetworkImage(icon);
            } else {
                holder.networkImageView.setVisibility(View.GONE);
            }

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(children > 1 ? View.VISIBLE : View.GONE);
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
        return getString(R.string.stats_view_clicks);
    }
}
