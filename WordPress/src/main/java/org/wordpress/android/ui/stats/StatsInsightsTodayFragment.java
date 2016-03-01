package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.List;


public class StatsInsightsTodayFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsTodayFragment.class.getSimpleName();

    // Container Activity must implement this interface
    public interface OnInsightsTodayClickListener {
        void onInsightsTodayClicked(StatsVisitorsAndViewsFragment.OverviewLabel item);
    }

    private OnInsightsTodayClickListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnInsightsTodayClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnInsightsTodayClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        TextView moduleTitle = (TextView) view.findViewById(R.id.stats_module_title);
        moduleTitle.setTag(StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS);
        moduleTitle.setOnClickListener(ButtonsOnClickListener);
        moduleTitle.setTextColor(getResources().getColor(R.color.stats_link_text_color));
        return view;
    }


    private VisitsModel mVisitsModel;

    @Override
    protected boolean hasDataAvailable() {
        return mVisitsModel != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mVisitsModel);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mVisitsModel = (VisitsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.VisitorsAndViewsUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mVisitsModel = event.mVisitsAndViews;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mVisitsModel = null;
        showErrorUI(event.mError);
    }

    protected void updateUI() {
        super.updateUI();

        if (!isAdded() || !hasDataAvailable()) {
            return;
        }

        if (mVisitsModel.getVisits() == null || mVisitsModel.getVisits().size() == 0) {
            showErrorUI();
            return;
        }

        List<VisitModel> visits = mVisitsModel.getVisits();
        VisitModel data = visits.get(visits.size() - 1);

        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.stats_insights_today_item, (ViewGroup) mResultContainer.getRootView(), false);

        LinearLayout tabs = (LinearLayout) ll.findViewById(R.id.stats_post_tabs);

        for (int i = 0; i < tabs.getChildCount(); i++) {
            LinearLayout currentTab = (LinearLayout) tabs.getChildAt(i);
            switch (i) {
                case 0:
                    setupTab(currentTab, FormatUtils.formatDecimal(data.getViews()), StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS);
                    break;
                case 1:
                    setupTab(currentTab, FormatUtils.formatDecimal(data.getVisitors()), StatsVisitorsAndViewsFragment.OverviewLabel.VISITORS );
                    break;
                case 2:
                    setupTab(currentTab, FormatUtils.formatDecimal(data.getLikes()), StatsVisitorsAndViewsFragment.OverviewLabel.LIKES );
                    break;
                case 3:
                    setupTab(currentTab, FormatUtils.formatDecimal(data.getComments()), StatsVisitorsAndViewsFragment.OverviewLabel.COMMENTS );
                    break;
            }
        }

        mResultContainer.addView(ll);
    }

    private void setupTab(LinearLayout currentTab, String total, final StatsVisitorsAndViewsFragment.OverviewLabel itemType) {
        final TextView label;
        final TextView value;
        final ImageView icon;

        currentTab.setTag(itemType);
        currentTab.setOnClickListener(ButtonsOnClickListener);

        label = (TextView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_label);
        label.setText(itemType.getLabel());
        value = (TextView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_value);
        value.setText(total);
        if (total.equals("0")) {
            value.setTextColor(getResources().getColor(R.color.grey));
        } else {
            value.setTextColor(getResources().getColor(R.color.blue_wordpress));
        }
        icon = (ImageView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_icon);
        icon.setImageDrawable(getTabIcon(itemType));

        if (itemType == StatsVisitorsAndViewsFragment.OverviewLabel.COMMENTS) {
            currentTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_white);
        } else {
            currentTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_white);
        }
    }

    private final View.OnClickListener ButtonsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isAdded()) {
                return;
            }
            if (mListener == null) {
                return;
            }
            StatsVisitorsAndViewsFragment.OverviewLabel tag = (StatsVisitorsAndViewsFragment.OverviewLabel) v.getTag();
            mListener.onInsightsTodayClicked(tag);
        }
    };

    private Drawable getTabIcon(final StatsVisitorsAndViewsFragment.OverviewLabel labelItem) {
        switch (labelItem) {
            case VISITORS:
                return getResources().getDrawable(R.drawable.stats_icon_visitors);
            case COMMENTS:
                return getResources().getDrawable(R.drawable.stats_icon_comments);
            case LIKES:
                return getResources().getDrawable(R.drawable.stats_icon_likes);
            default:
                // Views and when no prev match
                return getResources().getDrawable(R.drawable.stats_icon_views);
        }
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.VISITS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_today);
    }

}
