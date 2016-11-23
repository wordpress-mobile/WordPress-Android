package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.ReferrerGroupModel;
import org.wordpress.android.ui.stats.models.ReferrerResultModel;
import org.wordpress.android.ui.stats.models.ReferrersModel;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

public class StatsReferrersFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsReferrersFragment.class.getSimpleName();

    private ReferrersModel mReferrers;

    @Override
    protected boolean hasDataAvailable() {
        return mReferrers != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mReferrers);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mReferrers = (ReferrersModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.ReferrersUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mGroupIdToExpandedMap.clear();
        mReferrers = event.mReferrers;

        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mReferrers = null;
        mGroupIdToExpandedMap.clear();
        showErrorUI(event.mError);
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
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
        return mReferrers != null
                && mReferrers.getGroups() != null
                && mReferrers.getGroups().size() > 0;
    }

    private List<ReferrerGroupModel> getReferrersGroups() {
        if (!hasReferrers()) {
            return new ArrayList<ReferrerGroupModel>(0);
        }
        return mReferrers.getGroups();
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
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
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
        public final Activity act;
        private final List<ReferrerGroupModel> groups;
        private final List<List<MyChildModel>> children;

        public MyExpandableListAdapter(Activity act, List<ReferrerGroupModel> groups) {
            this.groups = groups;
            this.inflater = LayoutInflater.from(act);
            this.act = act;

            // The code below flattens the 3-levels tree of children to a 2-levels structure
            // that will be used later to populate the UI
            this.children = new ArrayList<>(groups.size());
            // pre-populate the structure with null values
            for (int i = 0; i < groups.size(); i++) {
                this.children.add(null);
            }

            for (int i = 0; i < groups.size(); i++) {
                ReferrerGroupModel currentGroup = groups.get(i);
                List<MyChildModel> currentGroupChildren = new ArrayList<>();
                List<ReferrerResultModel> childrenOfLevelOne = currentGroup.getResults();
                if (childrenOfLevelOne != null) {
                    // Children at first level could be a single item or another tree
                    // Levels 2 children are skipped in the UI.
                    for (ReferrerResultModel singleLevelOneChild : childrenOfLevelOne) {
                        // Use all the info given in the first level child.
                        MyChildModel myChild = new MyChildModel();
                        myChild.icon = singleLevelOneChild.getIcon();
                        myChild.url = singleLevelOneChild.getUrl();
                        myChild.name = singleLevelOneChild.getName();
                        myChild.views = singleLevelOneChild.getViews();

                        // read the URL from the first second-level child if available.
                        List<SingleItemModel> secondLevelChildren = singleLevelOneChild.getChildren();
                        if (secondLevelChildren != null && secondLevelChildren.size() > 0) {
                            SingleItemModel firstThirdLevelChild = secondLevelChildren.get(0);
                            myChild.url = firstThirdLevelChild.getUrl();
                        }
                        currentGroupChildren.add(myChild);
                    }
                }
                this.children.set(i, currentGroupChildren);
            }
        }

        private final class MyChildModel {
            String name;
            int views;
            String url;
            String icon;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            List<MyChildModel> currentGroupChildren = children.get(groupPosition);
            return currentGroupChildren.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            final MyChildModel currentChild = (MyChildModel) getChild(groupPosition, childPosition);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(convertView);
                convertView.setTag(viewHolder);
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            String name = currentChild.name;
            int views = currentChild.views;

            holder.chevronImageView.setVisibility(View.GONE);
            holder.linkImageView.setVisibility(TextUtils.isEmpty(currentChild.url) ? View.GONE : View.VISIBLE);
            holder.setEntryTextOrLink(currentChild.url, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(views));

            // site icon
            holder.networkImageView.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(currentChild.icon)) {
                holder.networkImageView.setImageUrl(
                        GravatarUtils.fixGravatarUrl(currentChild.icon, mResourceVars.headerAvatarSizePx),
                        WPNetworkImageView.ImageType.GONE_UNTIL_AVAILABLE);
            }

            // no more btm
            holder.imgMore.setVisibility(View.GONE);

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            List<MyChildModel> currentGroupChildren = children.get(groupPosition);
            if (currentGroupChildren == null) {
                return 0;
            } else {
                return currentGroupChildren.size();
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
                convertView.setTag(holder);
            } else {
                holder = (StatsViewHolder) convertView.getTag();
            }

            final ReferrerGroupModel group = (ReferrerGroupModel) getGroup(groupPosition);

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

            // Site icon
            holder.networkImageView.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(icon)) {
                holder.networkImageView.setImageUrl(
                        GravatarUtils.fixGravatarUrl(icon, mResourceVars.headerAvatarSizePx),
                        WPNetworkImageView.ImageType.GONE_UNTIL_AVAILABLE);
            }

            if (children == 0) {
                holder.showLinkIcon();
            } else {
                holder.showChevronIcon();
            }

            // Setup the spam button
            if (ReferrerSpamHelper.isSpamActionAvailable(group)) {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ReferrerSpamHelper rp = new ReferrerSpamHelper(act);
                        rp.showPopup(holder.imgMore, group);
                    }
                });

            } else {
                holder.imgMore.setVisibility(View.GONE);
                holder.imgMore.setClickable(false);
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
