package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.PopupMenu;

import org.apache.commons.lang.StringUtils;
import org.wordpress.android.R;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.stats.models.ReferrerGroupModel;
import org.wordpress.android.ui.stats.models.ReferrersModel;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.List;

public class StatsReferrersFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsReferrersFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (isErrorResponse(0)) {
            showErrorUI(mDatamodels[0]);
            return;
        }

        if (hasReferrers()) {
            BaseExpandableListAdapter adapter = new MyExpandableListAdapter(getActivity(), getReferrersGroups());
            StatsUIHelper.reloadGroupViews(getActivity(), adapter, mGroupIdToExpandedMap, mList, getMaxNumberOfItemsToShowInList());
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    private boolean hasReferrers() {
        return mDatamodels != null && mDatamodels[0] != null
                && ((ReferrersModel) mDatamodels[0]).getGroups() != null
                && ((ReferrersModel) mDatamodels[0]).getGroups().size() > 0;
    }

    private List<ReferrerGroupModel> getReferrersGroups() {
        if (!hasReferrers()) {
            return null;
        }
        return ((ReferrersModel) mDatamodels[0]).getGroups();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return hasReferrers() && getReferrersGroups().size() > 10;
    }

    @Override
    protected boolean isExpandableList() {
        return true;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[]getSectionToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.REFERRERS
        };
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_referrers;
    }
    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }
    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_referrers_title;
    }
    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_referrers_desc;
    }

    private class MyExpandableListAdapter extends BaseExpandableListAdapter {
        public LayoutInflater inflater;
        public Activity activity;
        private List<ReferrerGroupModel> groups;

        public MyExpandableListAdapter(Activity act, List<ReferrerGroupModel> groups) {
            this.activity = act;
            this.groups = groups;
            this.inflater = act.getLayoutInflater();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            ReferrerGroupModel currentGroup = groups.get(groupPosition);
            List<SingleItemModel> results = currentGroup.getResults();
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

            String name = children.getTitle();
            int total = children.getTotals();

            // name, url
            holder.setEntryTextOrLink(name, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon, make it invisible so children are indented
            holder.networkImageView.setVisibility(View.INVISIBLE);

            if (StringUtils.isNotBlank(children.getUrl())) {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PopupMenu popup = new PopupMenu(activity, view);
                        MenuItem menuItem = popup.getMenu().add(getString(R.string.view_in_browser));
                        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                WPWebViewActivity.openURL(activity, children.getUrl());
                                return true;
                            }
                        });
                        popup.show();
                    }
                });
            }
            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            ReferrerGroupModel currentGroup = groups.get(groupPosition);
            List<SingleItemModel> referrals = currentGroup.getResults();
            if (referrals == null) {
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

            ReferrerGroupModel group = (ReferrerGroupModel) getGroup(groupPosition);

            String name = group.getName();
            int total = group.getTotal();
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
        return getString(R.string.stats_view_referrers);
    }
}
