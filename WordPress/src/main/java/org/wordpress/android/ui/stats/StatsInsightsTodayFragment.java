package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsService;

import java.util.List;


public class StatsInsightsTodayFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsTodayFragment.class.getSimpleName();

    // Container Activity must implement this interface
    public interface OnInsightsTodayClickListener {
        void onInsightsClicked(StatsVisitorsAndViewsFragment.OverviewLabel item);
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

    void customizeUIWithResults() {
        mResultContainer.removeAllViews();

        // Another check that the data is available
        if (isDataEmpty(0) || !(mDatamodels[0] instanceof VisitsModel)) {
            showErrorUI(0);
            return;
        }

        VisitsModel visitsModel = (VisitsModel) mDatamodels[0];
        if (visitsModel.getVisits() == null || visitsModel.getVisits().size() == 0) {
            showErrorUI(0);
            return;
        }

        List<VisitModel> visits = visitsModel.getVisits();
        VisitModel data = visits.get(visits.size()-1);

        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.stats_insights_today_item, (ViewGroup) mResultContainer.getRootView(), false);

        for (int i = 0; i < ll.getChildCount(); i++) {
            LinearLayout currentTab = (LinearLayout) ll.getChildAt(i);
            switch (i) {
                case 0:
                    setupTab(currentTab, String.valueOf(data.getViews()), StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS );
                    break;
                case 1:
                    setupTab(currentTab, String.valueOf(data.getVisitors()), StatsVisitorsAndViewsFragment.OverviewLabel.VISITORS );
                    break;
                case 2:
                    setupTab(currentTab, String.valueOf(data.getLikes()), StatsVisitorsAndViewsFragment.OverviewLabel.LIKES );
                    break;
                case 3:
                    setupTab(currentTab, String.valueOf(data.getComments()), StatsVisitorsAndViewsFragment.OverviewLabel.COMMENTS );
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
        label.setTextColor(getResources().getColor(R.color.grey_darken_20));
        value.setTextColor(getResources().getColor(R.color.blue_wordpress));
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
            mListener.onInsightsClicked(tag);
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
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
               // StatsService.StatsEndpointsEnum.INSIGHTS_TODAY,
                StatsService.StatsEndpointsEnum.VISITS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_today);
    }

}
