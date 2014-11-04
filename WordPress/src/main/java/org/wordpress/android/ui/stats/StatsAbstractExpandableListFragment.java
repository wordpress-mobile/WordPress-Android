package org.wordpress.android.ui.stats;

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
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.Locale;


public abstract class StatsAbstractExpandableListFragment extends StatsAbstractFragment {

    protected static final int NO_STRING_ID = -1;

    protected TextView mEmptyLabel;
    protected LinearLayout mListContainer;
    protected LinearLayout mList;
    protected BaseExpandableListAdapter mAdapter;
    protected Serializable mDatamodel;

    protected SparseBooleanArray mGroupIdToExpandedMap;

    protected abstract int getEntryLabelResId();
    protected abstract int getTotalsLabelResId();
    protected abstract int getEmptyLabelTitleResId();
    protected abstract int getEmptyLabelDescResId();
    protected abstract StatsService.StatsSectionEnum getSectionToUpdate();
    protected abstract void updateUI();


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
        if (mDatamodel != null) {
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
            AppLog.d(AppLog.T.STATS, this.getTag() + " > restoring instance state");
            if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
                mDatamodel = savedInstanceState.getSerializable(ARG_REST_RESPONSE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(AppLog.T.STATS, this.getTag() + " > saving instance state");
        outState.putSerializable(ARG_REST_RESPONSE, mDatamodel);
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

    protected void showLoadingUI() {
        mEmptyLabel.setText("Loading...");
        mEmptyLabel.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);
        return;
    }

    protected void showEmptyUI(boolean show) {
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
            if (sectionToUpdate != getSectionToUpdate()) {
                return;
            }

            mGroupIdToExpandedMap.clear();
            if (action.equals(StatsService.ACTION_STATS_UPDATED)) {
                Serializable dataObj = intent.getSerializableExtra(StatsService.EXTRA_SECTION_DATA);
               /* if (dataObj == null || dataObj instanceof VolleyError) {
                    //TODO: show the error on the section ???
                    return;
                }*/
                mDatamodel = (dataObj == null || dataObj instanceof VolleyError) ? null : dataObj;
                updateUI();
            } if (action.equals(StatsService.ACTION_STATS_UPDATING)) {
                showLoadingUI();
            }

            return;
        }
    };
}
