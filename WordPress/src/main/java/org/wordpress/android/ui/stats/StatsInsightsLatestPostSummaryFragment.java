package org.wordpress.android.ui.stats;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.stats.models.InsightsLatestPostDetailsModel;
import org.wordpress.android.ui.stats.models.InsightsLatestPostModel;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

public class StatsInsightsLatestPostSummaryFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsLatestPostSummaryFragment.class.getSimpleName();

    private static final String ARG_REST_RESPONSE_DETAILS = "ARG_REST_RESPONSE_DETAILS";

    private InsightsLatestPostModel mInsightsLatestPostModel;
    private InsightsLatestPostDetailsModel mInsightsLatestPostDetailsModel;

    @Override
    protected boolean hasDataAvailable() {
        return mInsightsLatestPostModel != null && mInsightsLatestPostDetailsModel != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mInsightsLatestPostModel);
            outState.putSerializable(ARG_REST_RESPONSE_DETAILS, mInsightsLatestPostDetailsModel);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mInsightsLatestPostModel = (InsightsLatestPostModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE_DETAILS)) {
            mInsightsLatestPostDetailsModel = (InsightsLatestPostDetailsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE_DETAILS);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.InsightsLatestPostSummaryUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        if (event.mInsightsLatestPostModel == null) {
            showErrorUI();
            return;
        }

        mInsightsLatestPostModel = event.mInsightsLatestPostModel;

        // check if there is a post "published" on the blog
        View mainView = getView();
        if (mainView != null) {
            mainView.setVisibility(mInsightsLatestPostModel.isLatestPostAvailable() ? View.VISIBLE : View.GONE);
        }
        if (!mInsightsLatestPostModel.isLatestPostAvailable()) {
            // No need to go further into UI updating. There are no posts on this blog and the
            // entire fragment is hidden.
            return;
        }

        // Check if we already have the number of "views" for the latest post
        if (mInsightsLatestPostModel.getPostViewsCount() == Integer.MIN_VALUE) {
            // we don't have the views count. Need to call the service again here
            refreshStats(mInsightsLatestPostModel.getPostID(),
                    new StatsService.StatsEndpointsEnum[]{StatsService.StatsEndpointsEnum.INSIGHTS_LATEST_POST_VIEWS});
            showPlaceholderUI();
        } else {
            updateUI();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.InsightsLatestPostDetailsUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        if (mInsightsLatestPostModel == null || event.mInsightsLatestPostDetailsModel == null) {
            showErrorUI();
            return;
        }

        mInsightsLatestPostDetailsModel = event.mInsightsLatestPostDetailsModel;
        mInsightsLatestPostModel.setPostViewsCount(mInsightsLatestPostDetailsModel.getPostViewsCount());
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {

        if (!shouldUpdateFragmentOnErrorEvent(event)
                && event.mEndPointName != StatsService.StatsEndpointsEnum.INSIGHTS_LATEST_POST_VIEWS ) {
            return;
        }

        mInsightsLatestPostDetailsModel = null;
        mInsightsLatestPostModel =  null;
        showErrorUI(event.mError);
    }

    protected void updateUI() {
        super.updateUI();

        if (!isAdded() || !hasDataAvailable()) {
            return;
        }

        // check if there are posts "published" on the blog
        if (!mInsightsLatestPostModel.isLatestPostAvailable()) {
            // No need to go further into UI updating. There are no posts on this blog and the
            // entire fragment is hidden.
            return;
        }

        TextView moduleTitle = (TextView) getView().findViewById(R.id.stats_module_title);
        moduleTitle.setOnClickListener(ViewsTabOnClickListener);
        moduleTitle.setTextColor(getResources().getColor(R.color.stats_link_text_color));

        // update the tabs and the text now
        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.stats_insights_latest_post_item, (ViewGroup) mResultContainer.getRootView(), false);

        String trendLabel = getString(R.string.stats_insights_latest_post_trend);
        String sinceLabel = StatsUtils.getSinceLabel(
                getActivity(),
                mInsightsLatestPostModel.getPostDate()
        ).toLowerCase();

        String postTitle = StringEscapeUtils.unescapeHtml(mInsightsLatestPostModel.getPostTitle());
        if (TextUtils.isEmpty(postTitle)) {
            postTitle = getString(R.string.stats_insights_latest_post_no_title);
        }

        final String trendLabelFormatted = String.format(
                trendLabel, sinceLabel, postTitle);

        int startIndex, endIndex;
        startIndex = trendLabelFormatted.indexOf(postTitle);
        endIndex = startIndex + postTitle.length() +1;

        Spannable descriptionTextToSpan = new SpannableString(trendLabelFormatted);
        descriptionTextToSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.stats_link_text_color)),
                startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView trendLabelTextField = (TextView) ll.findViewById(R.id.stats_post_trend_label);
        trendLabelTextField.setText(descriptionTextToSpan);
        trendLabelTextField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StatsUtils.openPostInReaderOrInAppWebview(getActivity(),
                        mInsightsLatestPostModel.getBlogID(),
                        String.valueOf(mInsightsLatestPostModel.getPostID()),
                        StatsConstants.ITEM_TYPE_POST,
                        mInsightsLatestPostModel.getPostURL());
            }
        });

        LinearLayout tabs = (LinearLayout) ll.findViewById(R.id.stats_latest_post_tabs);

        for (int i = 0; i < tabs.getChildCount(); i++) {
            LinearLayout currentTab = (LinearLayout) tabs.getChildAt(i);
            switch (i) {
                case 0:
                    setupTab(currentTab, FormatUtils.formatDecimal(mInsightsLatestPostModel.getPostViewsCount()), StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS);
                    break;
                case 1:
                    setupTab(currentTab, FormatUtils.formatDecimal(mInsightsLatestPostModel.getPostLikeCount()), StatsVisitorsAndViewsFragment.OverviewLabel.LIKES);
                    break;
                case 2:
                    setupTab(currentTab, FormatUtils.formatDecimal(mInsightsLatestPostModel.getPostCommentCount()), StatsVisitorsAndViewsFragment.OverviewLabel.COMMENTS);
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
        // Only Views is clickable here
        if (itemType == StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS) {
            currentTab.setOnClickListener(ViewsTabOnClickListener);
        } else {
            currentTab.setClickable(false);
        }

        label = (TextView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_label);
        label.setText(itemType.getLabel());
        value = (TextView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_value);
        value.setText(total);
        if (total.equals("0")) {
            value.setTextColor(getResources().getColor(R.color.grey));
        } else {
            // Only Views is clickable here.
            // Likes and Comments shouldn't link anywhere because they don't have summaries
            // so their color should be Gray Darken 30 or #3d596d
            if (itemType == StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS) {
                value.setTextColor(getResources().getColor(R.color.blue_wordpress));
            } else {
                value.setTextColor(getResources().getColor(R.color.grey_darken_30));
            }
        }
        icon = (ImageView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_icon);
        icon.setImageDrawable(getTabIcon(itemType));

        if (itemType == StatsVisitorsAndViewsFragment.OverviewLabel.COMMENTS) {
            currentTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_white);
        } else {
            currentTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_white);
        }
    }

    private final View.OnClickListener ViewsTabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isAdded()) {
                return;
            }

            // Another check that the data is available
            if (mInsightsLatestPostModel == null) {
                showErrorUI();
                return;
            }

            PostModel postModel = new PostModel(
                    mInsightsLatestPostModel.getBlogID(),
                    String.valueOf(mInsightsLatestPostModel.getPostID()),
                    mInsightsLatestPostModel.getPostTitle(),
                    mInsightsLatestPostModel.getPostURL(),
                    StatsConstants.ITEM_TYPE_POST);
            ActivityLauncher.viewStatsSinglePostDetails(getActivity(), postModel);
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
                StatsService.StatsEndpointsEnum.INSIGHTS_LATEST_POST_SUMMARY,
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_latest_post_summary);
    }
}