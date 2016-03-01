package org.wordpress.android.ui.stats;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.ClickGroupModel;
import org.wordpress.android.ui.stats.models.ClicksModel;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

public class StatsClicksFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsClicksFragment.class.getSimpleName();

    private ClicksModel mClicks;

    @Override
    protected boolean hasDataAvailable() {
        return mClicks != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mClicks);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mClicks = (ClicksModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.ClicksUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mGroupIdToExpandedMap.clear();
        mClicks = event.mClicks;

        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mClicks = null;
        mGroupIdToExpandedMap.clear();
        showErrorUI(event.mError);
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (hasClicks()) {
            BaseExpandableListAdapter adapter = new MyExpandableListAdapter(getActivity(), mClicks.getClickGroups());
            StatsUIHelper.reloadGroupViews(getActivity(), adapter, mGroupIdToExpandedMap, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasClicks() {
        return mClicks != null
                && mClicks.getClickGroups() != null
                && mClicks.getClickGroups().size() > 0;
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return (hasClicks() && mClicks.getClickGroups().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST);
    }

    @Override
    protected boolean isExpandableList() {
        return true;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
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
        public final LayoutInflater inflater;
        private final List<ClickGroupModel> clickGroups;

        public MyExpandableListAdapter(Context context, List<ClickGroupModel> clickGroups) {
            this.clickGroups = clickGroups;
            this.inflater = LayoutInflater.from(context);
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

            // The link icon
            holder.showLinkIcon();

            // name, url
            holder.setEntryTextOrLink(children.getUrl(), children.getTitle());

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(
                    children.getTotals()
            ));

            // no icon
            holder.networkImageView.setVisibility(View.GONE);

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

            final StatsViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                holder = new StatsViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (StatsViewHolder) convertView.getTag();
            }

            ClickGroupModel group = (ClickGroupModel) getGroup(groupPosition);

            String name = group.getName();
            int total = group.getViews();
            String url = group.getUrl();
            String icon = group.getIcon();
            int children = getChildrenCount(groupPosition);

            if (children > 0) {
                holder.setEntryText(name, getResources().getColor(R.color.stats_link_text_color));
            } else {
                holder.setEntryTextOrLink(url, name);
            }

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // Site icon
            holder.networkImageView.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(icon)) {
                holder.networkImageView.setImageUrl(
                        GravatarUtils.fixGravatarUrl(icon, mResourceVars.headerAvatarSizePx),
                        WPNetworkImageView.ImageType.GONE_UNTIL_AVAILABLE
                );
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
        return getString(R.string.stats_view_clicks);
    }
}
