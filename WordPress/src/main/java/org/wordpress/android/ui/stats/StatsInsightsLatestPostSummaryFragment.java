package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.stats.models.InsightsLatestPostDetailsModel;
import org.wordpress.android.ui.stats.models.InsightsLatestPostModel;
import org.wordpress.android.ui.stats.models.StatsPostModel;
import org.wordpress.android.ui.stats.service.StatsServiceLogic;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.FormatUtils;

import java.util.Locale;

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
            mInsightsLatestPostDetailsModel =
                    (InsightsLatestPostDetailsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE_DETAILS);
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
                         new StatsServiceLogic.StatsEndpointsEnum[]{
                                 StatsServiceLogic.StatsEndpointsEnum.INSIGHTS_LATEST_POST_VIEWS});
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
            && event.mEndPointName != StatsServiceLogic.StatsEndpointsEnum.INSIGHTS_LATEST_POST_VIEWS) {
            return;
        }

        mInsightsLatestPostDetailsModel = null;
        mInsightsLatestPostModel = null;
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
        moduleTitle.setOnClickListener(mViewsTabOnClickListener);
        moduleTitle.setTextColor(getResources().getColor(R.color.stats_link_text_color));

        // update the tabs and the text now
        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                                                      .inflate(R.layout.stats_insights_latest_post_item,
                                                               (ViewGroup) mResultContainer.getRootView(), false);

        String trendLabel = getString(R.string.stats_insights_latest_post_trend);
        String sinceLabel = StatsUtils.getSinceLabel(
                getActivity(),
                mInsightsLatestPostModel.getPostDate()).toLowerCase(Locale.getDefault());

        String postTitle = StringEscapeUtils.unescapeHtml4(mInsightsLatestPostModel.getPostTitle());
        if (TextUtils.isEmpty(postTitle)) {
            postTitle = getString(R.string.stats_insights_latest_post_no_title);
        }

        final String trendLabelFormatted = String.format(
                trendLabel, sinceLabel, postTitle);

        int startIndex, endIndex;
        startIndex = trendLabelFormatted.indexOf(postTitle);
        endIndex = startIndex + postTitle.length() + 1;

        Spannable descriptionTextToSpan = new SpannableString(trendLabelFormatted);
        descriptionTextToSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.stats_link_text_color)),
                                      startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView trendLabelTextField = ll.findViewById(R.id.stats_post_trend_label);
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

        LinearLayout tabs = ll.findViewById(R.id.stats_latest_post_tabs);

        for (int i = 0; i < tabs.getChildCount(); i++) {
            LinearLayout currentTab = (LinearLayout) tabs.getChildAt(i);
            switch (i) {
                case 0:
                    setupTab(currentTab, FormatUtils.formatDecimal(mInsightsLatestPostModel.getPostViewsCount()),
                             StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS);
                    break;
                case 1:
                    setupTab(currentTab, FormatUtils.formatDecimal(mInsightsLatestPostModel.getPostLikeCount()),
                             StatsVisitorsAndViewsFragment.OverviewLabel.LIKES);
                    break;
                case 2:
                    setupTab(currentTab, FormatUtils.formatDecimal(mInsightsLatestPostModel.getPostCommentCount()),
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
        // Only Views is clickable here
        if (itemType == StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS) {
            currentTab.setOnClickListener(mViewsTabOnClickListener);
        } else {
            currentTab.setClickable(false);
        }

        label = currentTab.findViewById(R.id.stats_visitors_and_views_tab_label);
        label.setText(itemType.getLabel());
        value = currentTab.findViewById(R.id.stats_visitors_and_views_tab_value);
        value.setText(total);
        if (total.equals("0")) {
            value.setTextColor(getResources().getColor(R.color.grey_text_min));
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
        icon = currentTab.findViewById(R.id.stats_visitors_and_views_tab_icon);
        ColorUtils.INSTANCE.setImageResourceWithTint(icon, getTabIcon(itemType), R.color.grey_dark);

        if (itemType == StatsVisitorsAndViewsFragment.OverviewLabel.COMMENTS) {
            currentTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_white);
        } else {
            currentTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_white);
        }
    }

    private final View.OnClickListener mViewsTabOnClickListener = new View.OnClickListener() {
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

            StatsPostModel postModel = new StatsPostModel(
                    mInsightsLatestPostModel.getBlogID(),
                    String.valueOf(mInsightsLatestPostModel.getPostID()),
                    mInsightsLatestPostModel.getPostTitle(),
                    mInsightsLatestPostModel.getPostURL(),
                    StatsConstants.ITEM_TYPE_POST);
            ActivityLauncher.viewStatsSinglePostDetails(getActivity(), postModel);
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
                StatsServiceLogic.StatsEndpointsEnum.INSIGHTS_LATEST_POST_SUMMARY,
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_latest_post_summary);
    }
}
