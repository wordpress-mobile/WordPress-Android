package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.ReferrerGroupModel;
import org.wordpress.android.ui.stats.model.ReferrerResultModel;
import org.wordpress.android.ui.stats.model.ReferrersModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;


public class StatsReferrersFragment extends StatsAbstractFragment {
    public static final String TAG = StatsReferrersFragment.class.getSimpleName();

    private static final int NO_STRING_ID = -1;
    private TextView mEmptyLabel;
    private LinearLayout mListContainer;
    private LinearLayout mList;
    private BaseExpandableListAdapter mAdapter;
    private ReferrersModel mReferrersModel;

    private SparseBooleanArray mGroupIdToExpandedMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_expandable_list_fragment, container, false);

        TextView titleTextView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleTextView.setText(getTitle().toUpperCase(Locale.getDefault()));

        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());
        TextView totalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        totalsLabel.setText(getTotalsLabelResId());

        mEmptyLabel = (TextView) view.findViewById(R.id.stats_list_empty_text);
        mList = (LinearLayout) view.findViewById(R.id.stats_list_linearlayout);
        mList.setVisibility(View.VISIBLE);
        mListContainer = (LinearLayout) view.findViewById(R.id.stats_list_header_container);

        // Init the UI
        if (mReferrersModel != null) {
            updateUI();
        } else {
            showEmptyUI(true);
        }
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGroupIdToExpandedMap = new SparseBooleanArray();

        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.STATS, "StatsReferrersFragment > restoring instance state");
            if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
                mReferrersModel = (ReferrersModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(AppLog.T.STATS, "StatsReferrersFragment > saving instance state");
        outState.putSerializable(ARG_REST_RESPONSE, mReferrersModel);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_UPDATED));
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_UPDATING));
    }

    private void updateUI() {
        if (mReferrersModel != null && mReferrersModel.getGroups().size() > 0) {
            setListAdapter(new MyExpandableListAdapter(getActivity(), mReferrersModel.getGroups()));
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    private void showLoadingUI() {
        mEmptyLabel.setText("Loading...");
        mEmptyLabel.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);
        return;
    }

    private void showEmptyUI(boolean show) {
        if (show) {
            mGroupIdToExpandedMap.clear();
            String label;
            if (getEmptyLabelDescResId() == NO_STRING_ID) {
                label = "<b>" + getString(getEmptyLabelTitleResId()) + "</b>";
            } else {
                label = "<b>" + getString(getEmptyLabelTitleResId()) + "</b> " + getString(getEmptyLabelDescResId());
            }
            if (label.contains("<")) {
                mEmptyLabel.setText(Html.fromHtml(label));
            } else {
                mEmptyLabel.setText(label);
            }
            mEmptyLabel.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
            mList.setVisibility(View.GONE);
        } else {
            mEmptyLabel.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
            mList.setVisibility(View.VISIBLE);
            StatsUIHelper.reloadGroupViews(getActivity(), mAdapter, mGroupIdToExpandedMap, mList);
        }
    }

    /*
 * receives broadcast when data has been updated
 */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = StringUtils.notNullStr(intent.getAction());

            if (!(action.equals(StatsService.ACTION_STATS_UPDATED) || action.equals(StatsService.ACTION_STATS_UPDATING))) {
                return;
            }

            if (!intent.hasExtra(StatsService.EXTRA_SECTION_NAME)) {
                return;
            }

            StatsService.StatsSectionEnum sectionToUpdate = (StatsService.StatsSectionEnum) intent.getSerializableExtra(StatsService.EXTRA_SECTION_NAME);
            if (sectionToUpdate != StatsService.StatsSectionEnum.REFERRERS) {
                return;
            }

            mGroupIdToExpandedMap.clear();
            if (action.equals(StatsService.ACTION_STATS_UPDATED)) {
                Serializable dataObj = intent.getSerializableExtra(StatsService.EXTRA_SECTION_DATA);
               /* if (dataObj == null || dataObj instanceof VolleyError) {
                    //TODO: show the error on the section ???
                    return;
                }*/
                mReferrersModel = (dataObj == null || dataObj instanceof VolleyError) ? null : (ReferrersModel) dataObj;
                updateUI();
            } if (action.equals(StatsService.ACTION_STATS_UPDATING)) {
               showLoadingUI();
            }

            return;
        }
    };

    public void setListAdapter(BaseExpandableListAdapter adapter) {
        mAdapter = adapter;
        StatsUIHelper.reloadGroupViews(getActivity(), mAdapter, mGroupIdToExpandedMap, mList);
    }

    private int getEntryLabelResId() {
        return R.string.stats_entry_referrers;
    }

    private int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }

    private int getEmptyLabelTitleResId() {
        return R.string.stats_empty_referrers_title;
    }

    private int getEmptyLabelDescResId() {
        return R.string.stats_empty_referrers_desc;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_referrers);
    }

    private class MyExpandableListAdapter extends BaseExpandableListAdapter {
        public LayoutInflater inflater;
        public Activity activity;
        private List<ReferrerGroupModel> groups;

        // Used to hold the single child values
        private class SingleChildEntry {
            String mName;
            String mIcon;
            int mTotal;
            String mUrl;
        }

        public MyExpandableListAdapter(Activity act, List<ReferrerGroupModel> groups) {
            this.activity = act;
            this.groups = groups;
            this.inflater = act.getLayoutInflater();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            ReferrerGroupModel currentGroup = groups.get(groupPosition);
            List<ReferrerResultModel> referrals = currentGroup.getResults();
            SingleChildEntry entry = new SingleChildEntry();

            if (referrals == null || referrals.size() == 0) {
                entry.mIcon = currentGroup.getIcon();
                entry.mName = currentGroup.getName();
                entry.mTotal = currentGroup.getTotal();
                entry.mUrl = currentGroup.getUrl();
            } else {
                ReferrerResultModel currentRef = referrals.get(childPosition);
                entry.mIcon = currentRef.getIcon();
                entry.mName = currentRef.getName();
                entry.mTotal = currentRef.getTotal();
                entry.mUrl = currentRef.getUrl();
            }

            return entry;
        }


        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            final SingleChildEntry children = (SingleChildEntry) getChild(groupPosition, childPosition);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(convertView);
                convertView.setTag(viewHolder);
            }

            final StatsViewHolder holder = (StatsViewHolder) convertView.getTag();

            String name = children.mName;
            int total = children.mTotal;

            // name, url
            holder.setEntryTextOrLink(name, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon, make it invisible so children are indented
            holder.networkImageView.setVisibility(View.INVISIBLE);

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            ReferrerGroupModel currentGroup = groups.get(groupPosition);
            List<ReferrerResultModel> referrals = currentGroup.getResults();
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
            holder.showNetworkImage(icon);

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

}
