package org.wordpress.android.ui.stats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.ReferrerGroupModel;
import org.wordpress.android.ui.stats.models.ReferrersModel;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.GravatarUtils.DefaultImage;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

public class StatsReferrersFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsReferrersFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (isErrorResponse()) {
            showErrorUI();
            return;
        }

        if (hasReferrers()) {
            BaseExpandableListAdapter adapter = new MyExpandableListAdapter(getActivity(), getReferrersGroups());
            StatsUIHelper.reloadGroupViews(getActivity(), adapter, mGroupIdToExpandedMap, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasReferrers() {
        return !isDataEmpty()
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
        return hasReferrers() && getReferrersGroups().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST;
    }

    @Override
    protected boolean isExpandableList() {
        return true;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
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
        public final LayoutInflater inflater;
        private final List<ReferrerGroupModel> groups;

        public MyExpandableListAdapter(Context context, List<ReferrerGroupModel> groups) {
            this.groups = groups;
            this.inflater = LayoutInflater.from(context);
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

            // The link icon
            holder.showLinkIcon();

            // name, url
            holder.setEntryTextOrLink(children.getUrl(), name);
            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            holder.networkImageView.setVisibility(View.GONE);
            // no more btm
            holder.imgMore.setVisibility(View.GONE);

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
        public View getGroupView(final int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            final StatsViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                holder = new StatsViewHolder(convertView);
                holder.networkImageView.setErrorImageResId(R.drawable.stats_icon_default_site_avatar);
                holder.networkImageView.setDefaultImageResId(R.drawable.stats_icon_default_site_avatar);
                convertView.setTag(holder);
            } else {
                holder = (StatsViewHolder) convertView.getTag();
            }

            ReferrerGroupModel group = (ReferrerGroupModel) getGroup(groupPosition);

            String name = group.getName();
            int total = group.getTotal();
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

            holder.networkImageView.setImageUrl(
                    GravatarUtils.fixGravatarUrl(icon, mResourceVars.headerAvatarSizePx, DefaultImage.STATUS_404),
                    WPNetworkImageView.ImageType.BLAVATAR);
            holder.networkImageView.setVisibility(View.VISIBLE);

            holder.chevronImageView.setVisibility(View.VISIBLE);
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
        return getString(R.string.stats_view_referrers);
    }
}
