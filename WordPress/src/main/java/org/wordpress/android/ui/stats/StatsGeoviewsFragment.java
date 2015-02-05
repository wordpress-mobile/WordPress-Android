package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.net.http.SslError;
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
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;


public class StatsGeoviewsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsGeoviewsFragment.class.getSimpleName();


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

                StringBuilder dataToLoad = new StringBuilder();

                for (int i = 0; i < countries.size(); i++) {
                    final GeoviewModel currentCountry = countries.get(i);
                    dataToLoad.append("['").append(currentCountry.getCountryFullName()).append("',")
                            .append(currentCountry.getViews()).append("],");
                }

                // This is the label that is shown when the user taps on a region
                String label = getResources().getString(getTotalsLabelResId());

                // See: https://developers.google.com/chart/interactive/docs/gallery/geochart
                StringBuilder htmlPage = new StringBuilder().append("<html>")
                        .append("<head>")
                        .append("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>")
                        .append("<script type=\"text/javascript\">")
                            .append("google.load(\"visualization\", \"1\", {packages:[\"geochart\"]});")
                            .append("google.setOnLoadCallback(drawRegionsMap);")
                            .append("function drawRegionsMap() {")
                                .append("var data = google.visualization.arrayToDataTable(")
                                .append("[")
                                .append("['Country', '").append(label).append("'],")
                                        .append(dataToLoad)
                                .append("]);")
                                .append("var options = {legend: 'none', keepAspectRatio: true, region: 'world', enableRegionInteractivity: true};")
                                .append("var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));")
                                .append("chart.draw(data, options);")
                            .append("}")
                            .append("</script>")
                        .append("</head>")
                        .append("<body>")
                        .append("<div id=\"regions_div\" style=\"width: 100%; height: 100%;\"></div>")
                        .append("</body>")
                        .append("</html>");


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
                webView.loadData(htmlPage.toString(), "text/html", "UTF-8");

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

        if (isErrorResponse()) {
            showErrorUI();
            hideMap();
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
        return !isDataEmpty() && ((GeoviewsModel) mDatamodels[0]).getCountries() != null;
    }

    private List<GeoviewModel> getCountries() {
        if (!hasCountries()) {
            return null;
        }
        return ((GeoviewsModel) mDatamodels[0]).getCountries();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return (!isDataEmpty()
                && ((GeoviewsModel) mDatamodels[0]).getCountries() != null
                && ((GeoviewsModel) mDatamodels[0]).getCountries().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST);
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
            holder.networkImageView.setImageUrl(PhotonUtils.fixAvatar(imageUrl, mResourceVars.headerAvatarSizePx), WPNetworkImageView.ImageType.SITE_AVATAR);
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
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.GEO_VIEWS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_countries);
    }
}
