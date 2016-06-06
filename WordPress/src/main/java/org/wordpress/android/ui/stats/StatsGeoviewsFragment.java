package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.GeoviewModel;
import org.wordpress.android.ui.stats.models.GeoviewsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;


public class StatsGeoviewsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsGeoviewsFragment.class.getSimpleName();

    private GeoviewsModel mCountries;

    @Override
    protected boolean hasDataAvailable() {
        return mCountries != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mCountries);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mCountries = (GeoviewsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.CountriesUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mCountries = event.mCountries;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mCountries = null;
        showErrorUI(event.mError);
    }

    private void hideMap() {
        if (!isAdded()) {
            return;
        }

        mTopPagerContainer.setVisibility(View.GONE);
    }

    private void showMap(final List<GeoviewModel> countries) {
        if (!isAdded()) {
            return;
        }

        // setting up different margins for the map. We're basically remove left margins since the
        // chart service produce a map that's slightly shifted on the right. See the Web version.
        int dp4 = DisplayUtils.dpToPx(mTopPagerContainer.getContext(), 4);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, dp4, 0);
        mTopPagerContainer.setLayoutParams(layoutParams);

        mTopPagerContainer.removeAllViews();

        // must wait for mTopPagerContainer to be fully laid out (ie: measured). Then we can read the width and
        // calculate the right height for the map div
        mTopPagerContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTopPagerContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                if (!isAdded()) {
                    return;
                }

                StringBuilder dataToLoad = new StringBuilder();

                for (int i = 0; i < countries.size(); i++) {
                    final GeoviewModel currentCountry = countries.get(i);
                    dataToLoad.append("['").append(currentCountry.getCountryFullName()).append("',")
                            .append(currentCountry.getViews()).append("],");
                }

                // This is the label that is shown when the user taps on a region
                String label = getResources().getString(getTotalsLabelResId());

                // See: https://developers.google.com/chart/interactive/docs/gallery/geochart
                // Loading the v42 of the Google Charts API, since the latest stable version has a problem with the legend. https://github.com/wordpress-mobile/WordPress-Android/issues/4131
                // https://developers.google.com/chart/interactive/docs/release_notes#release-candidate-details
                String htmlPage = "<html>" +
                        "<head>" +
                        "<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>" +
                        "<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>" +
                        "<script type=\"text/javascript\">" +
                            "google.charts.load('42', {'packages':['geochart']});" +
                            "google.charts.setOnLoadCallback(drawRegionsMap);" +
                            "function drawRegionsMap() {" +
                                "var data = google.visualization.arrayToDataTable(" +
                                "[" +
                                "['Country', '" + label + "']," +
                                        dataToLoad +
                                "]);" +
                                "var options = {keepAspectRatio: true, region: 'world', colorAxis: { colors: [ '#FFF088', '#F34605' ] }, enableRegionInteractivity: true};" +
                                "var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));" +
                                "chart.draw(data, options);" +
                            "}" +
                            "</script>" +
                        "</head>" +
                        "<body>" +
                        "<div id=\"regions_div\" style=\"width: 100%; height: 100%;\"></div>" +
                        "</body>" +
                        "</html>";

                WebView webView = new WebView(getActivity());
                mTopPagerContainer.addView(webView);

                int width = mTopPagerContainer.getWidth();
                int height = width * 3 / 4;

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) webView.getLayoutParams();
                params.width = WebView.LayoutParams.MATCH_PARENT;
                params.height = height;

                webView.setLayoutParams(params);

                webView.setWebViewClient(new MyWebViewClient()); // Hide map in case of unrecoverable errors
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                webView.loadData(htmlPage, "text/html", "UTF-8");

            }
        });
        mTopPagerContainer.setVisibility(View.VISIBLE);
    }

    // Hide the Map in case of errors
    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            mTopPagerContainer.setVisibility(View.GONE);
            AppLog.e(AppLog.T.STATS, "Cannot load geochart."
                    + " ErrorCode: " + errorCode
                    + " Description: " + description
                    + " Failing URL: " + failingUrl);
        }
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            mTopPagerContainer.setVisibility(View.GONE);
            AppLog.e(AppLog.T.STATS, "Cannot load geochart. SSL ERROR. " + error.toString());
        }
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (hasCountries()) {
            List<GeoviewModel> countries = getCountries();
            ArrayAdapter adapter = new GeoviewsAdapter(getActivity(), countries);
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
            showMap(countries);
        } else {
            showHideNoResultsUI(true);
            hideMap();
        }
    }

    private boolean hasCountries() {
        return mCountries != null && mCountries.getCountries() != null;
    }

    private List<GeoviewModel> getCountries() {
        if (!hasCountries()) {
            return null;
        }
        return mCountries.getCountries();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return (hasCountries()
                && mCountries.getCountries().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST);
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class GeoviewsAdapter extends ArrayAdapter<GeoviewModel> {

        private final List<GeoviewModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public GeoviewsAdapter(Activity context, List<GeoviewModel> list) {
            super(context, R.layout.stats_list_cell, list);
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                rowView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final GeoviewModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();
            // fill data
            String entry = currentRowData.getCountryFullName();
            String imageUrl = currentRowData.getFlatFlagIconURL();
            int total = currentRowData.getViews();

            holder.setEntryText(entry);
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // image (country flag)
            holder.networkImageView.setImageUrl(
                    GravatarUtils.fixGravatarUrl(imageUrl, mResourceVars.headerAvatarSizePx),
                    WPNetworkImageView.ImageType.BLAVATAR);
            holder.networkImageView.setVisibility(View.VISIBLE);

            return rowView;
        }
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_country;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_geoviews;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_geoviews_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.GEO_VIEWS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_countries);
    }
}
