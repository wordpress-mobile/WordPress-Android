package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsServiceLogic;
import org.wordpress.android.util.ColorUtils;
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
        TextView moduleTitle = view.findViewById(R.id.stats_module_title);
        moduleTitle.setTag(StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS);
        moduleTitle.setOnClickListener(mButtonsOnClickListener);
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
                                                      .inflate(R.layout.stats_insights_today_item,
                                                               (ViewGroup) mResultContainer.getRootView(), false);

        LinearLayout tabs = ll.findViewById(R.id.stats_post_tabs);

        for (int i = 0; i < tabs.getChildCount(); i++) {
            LinearLayout currentTab = (LinearLayout) tabs.getChildAt(i);
            switch (i) {
                case 0:
                    setupTab(currentTab, FormatUtils.formatDecimal(data.getViews()),
                             StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS);
                    break;
                case 1:
                    setupTab(currentTab, FormatUtils.formatDecimal(data.getVisitors()),
                             StatsVisitorsAndViewsFragment.OverviewLabel.VISITORS);
                    break;
                case 2:
                    setupTab(currentTab, FormatUtils.formatDecimal(data.getLikes()),
                             StatsVisitorsAndViewsFragment.OverviewLabel.LIKES);
                    break;
                case 3:
                    setupTab(currentTab, FormatUtils.formatDecimal(data.getComments()),
                             StatsVisitorsAndViewsFragment.OverviewLabel.COMMENTS);
                    break;
            }
        }

        mResultContainer.addView(ll);
    }

    private void setupTab(LinearLayout currentTab, String total,
                          final StatsVisitorsAndViewsFragment.OverviewLabel itemType) {
        final TextView label;
        final TextView value;
        final ImageView icon;

        currentTab.setTag(itemType);
        currentTab.setOnClickListener(mButtonsOnClickListener);

        label = currentTab.findViewById(R.id.stats_visitors_and_views_tab_label);
        label.setText(itemType.getLabel());
        value = currentTab.findViewById(R.id.stats_visitors_and_views_tab_value);
        value.setText(total);
        if (total.equals("0")) {
            value.setTextColor(getResources().getColor(R.color.grey_text_min));
        } else {
            value.setTextColor(getResources().getColor(R.color.blue_wordpress));
        }
        icon = currentTab.findViewById(R.id.stats_visitors_and_views_tab_icon);
        ColorUtils.INSTANCE.setImageResourceWithTint(icon, getTabIcon(itemType), R.color.grey_dark);

        if (itemType == StatsVisitorsAndViewsFragment.OverviewLabel.COMMENTS) {
            currentTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_white);
        } else {
            currentTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_white);
        }
    }

    private final View.OnClickListener mButtonsOnClickListener = new View.OnClickListener() {
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

    private @DrawableRes int getTabIcon(final StatsVisitorsAndViewsFragment.OverviewLabel labelItem) {
        switch (labelItem) {
            case VISITORS:
                return R.drawable.ic_user_white_24dp;
            case COMMENTS:
                return R.drawable.ic_comment_white_24dp;
            case LIKES:
                return R.drawable.ic_star_white_24dp;
            default:
                // Views and when no prev match
                return R.drawable.ic_visible_on_white_24dp;
        }
    }

    @Override
    protected StatsServiceLogic.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsServiceLogic.StatsEndpointsEnum[]{
                StatsServiceLogic.StatsEndpointsEnum.VISITS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_today);
    }
}
