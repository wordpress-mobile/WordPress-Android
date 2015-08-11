package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.stats.models.InsightsLatestPostModel;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FormatUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class StatsInsightsLatestPostSummaryFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsLatestPostSummaryFragment.class.getSimpleName();

    private Handler mHandler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }

    void customizeUIWithResults() {
        if (!isAdded()) {
            return;
        }
        mResultContainer.removeAllViews();

        // Another check that the data is available
        if (isDataEmpty(0) || !(mDatamodels[0] instanceof InsightsLatestPostModel)) {
            showErrorUI(null);
            return;
        }

        final InsightsLatestPostModel latestPostModel = (InsightsLatestPostModel) mDatamodels[0];

        // check if the latest post is available on the blog
        View mainView = getView();
        if (mainView != null) {
            mainView.setVisibility(latestPostModel.isLatestPostAvailable() ? View.VISIBLE : View.GONE);
        }
        if (!latestPostModel.isLatestPostAvailable()) {
            // No need to go further into UI updating
            return;
        }

        // Check if the we already have the number of views for this post
        if (latestPostModel.getPostViewsCount() == Integer.MIN_VALUE) {
            // we don't have the views count. Need to call the server again.
            final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();

            final String singlePostRestPath = String.format(
                    "/sites/%s/stats/post/%s?fields=views", latestPostModel.getBlogID(), latestPostModel.getPostID());
            //AppLog.d(AppLog.T.STATS, "Enqueuing the following  request " + singlePostRestPath);
            RestCallListener vListener = new RestCallListener(getActivity());
            restClientUtils.get(singlePostRestPath, vListener, vListener);
            showPlaceholderUI();
            return;
        }

        TextView moduleTitle = (TextView) mainView.findViewById(R.id.stats_module_title);
        moduleTitle.setOnClickListener(ButtonsOnClickListener);
        moduleTitle.setTextColor(getResources().getColor(R.color.stats_link_text_color));

        // update the tabs now and the text now
        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.stats_insights_latest_post_item, (ViewGroup) mResultContainer.getRootView(), false);

        String trendLabel = getString(R.string.stats_insights_latest_post_trend);
        String sinceLabel = StatsUtils.getSinceLabel(
                getActivity(),
                latestPostModel.getPostDate()
        );

        final String trendLabelFormatted = String.format(
                trendLabel, sinceLabel, latestPostModel.getPostTitle());

        int startIndex, endIndex;
        startIndex = trendLabelFormatted.indexOf(latestPostModel.getPostTitle());
        endIndex = startIndex + latestPostModel.getPostTitle().length() +1;

        Spannable wordtoSpan = new SpannableString(trendLabelFormatted);
        wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.stats_link_text_color)),
                startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView trendLabelTextField = (TextView) ll.findViewById(R.id.stats_post_trend_label);
        trendLabelTextField.setText(wordtoSpan);
        trendLabelTextField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StatsUtils.openPostInReaderOrInAppWebview(getActivity(),
                        latestPostModel.getBlogID(),
                        String.valueOf(latestPostModel.getPostID()),
                        StatsConstants.ITEM_TYPE_POST,
                        latestPostModel.getPostURL());
            }
        });

        LinearLayout tabs = (LinearLayout) ll.findViewById(R.id.stats_latest_post_tabs);

        for (int i = 0; i < tabs.getChildCount(); i++) {
            LinearLayout currentTab = (LinearLayout) tabs.getChildAt(i);
            switch (i) {
                case 0:
                    setupTab(currentTab, FormatUtils.formatDecimal(latestPostModel.getPostViewsCount()), StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS);
                    break;
                case 1:
                    setupTab(currentTab, FormatUtils.formatDecimal(latestPostModel.getPostLikeCount()), StatsVisitorsAndViewsFragment.OverviewLabel.LIKES);
                    break;
                case 2:
                    setupTab(currentTab, FormatUtils.formatDecimal(latestPostModel.getPostCommentCount()), StatsVisitorsAndViewsFragment.OverviewLabel.COMMENTS);
                    break;
            }
        }

        mResultContainer.addView(ll);
    }


    private class RestCallListener implements RestRequest.Listener, RestRequest.ErrorListener {

        private final WeakReference<Activity> mActivityRef;

        public RestCallListener(Activity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onResponse(final JSONObject response) {
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing() || !isAdded()) {
                return;
            }

            final InsightsLatestPostModel latestPostModel = (InsightsLatestPostModel) mDatamodels[0];

            // single background thread used to parse the response in BG.
            ThreadPoolExecutor parseResponseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    AppLog.d(AppLog.T.STATS, "The REST response: " + response.toString());
                    try {

                        int view = response.getInt("views");
                        latestPostModel.setPostViewsCount(view);
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.STATS, "Cannot parse the JSON response", e);
                    }

                    // Update the UI
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateUI();
                        }
                    });
                }
            });
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            StatsUtils.logVolleyErrorDetails(volleyError);
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing() || !isAdded()) {
                return;
            }
            InsightsLatestPostModel latestPostModel = (InsightsLatestPostModel) mDatamodels[0];
            latestPostModel.setPostViewsCount(0);
            // Update the UI
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }
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

            // Another check that the data is available
            if (isDataEmpty(0) || !(mDatamodels[0] instanceof InsightsLatestPostModel)) {
                showErrorUI(null);
                return;
            }
            InsightsLatestPostModel latestPostModel = (InsightsLatestPostModel) mDatamodels[0];

            PostModel postModel = new PostModel(
                    latestPostModel.getBlogID(),
                    String.valueOf(latestPostModel.getPostID()),
                    latestPostModel.getPostTitle(),
                    latestPostModel.getPostURL(),
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
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.INSIGHTS_LATEST_POST_SUMMARY
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_latest_post_summary);
    }
}