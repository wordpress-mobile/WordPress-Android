package org.wordpress.android.ui.stats;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.GeoviewModel;
import org.wordpress.android.ui.stats.models.GeoviewsModel;
import org.wordpress.android.ui.stats.service.StatsService;
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

    @SuppressLint("NewApi")
    private void showMap(final List<GeoviewModel> countries) {
        if (!isAdded()) {
            return;
        }

        mTopPagerContainer.removeAllViews();

        // must wait for mTopPagerContainer to be fully laid out (ie: measured). Then we can read the width and
        // calculate the right height for the map div
        mTopPagerContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTopPagerContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                String dataToLoad = "";

                for (int i = 0; i < countries.size(); i++) {
                    final GeoviewModel currentCountry = countries.get(i);
                    dataToLoad += "['" + currentCountry.getCountryFullName() + "'," + currentCountry.getViews() + "],";
                }

                String label = getResources().getString(getTotalsLabelResId());

                String html_value = "<html>\n" +
                        "  <head>\n" +
                        "    <script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>\n" +
                        "    <script type=\"text/javascript\">\n" +
                        "      google.load(\"visualization\", \"1\", {packages:[\"geochart\"]});\n" +
                        "      google.setOnLoadCallback(drawRegionsMap);\n" +
                        "\n" +
                        "      function drawRegionsMap() {\n" +
                        "\n" +
                        "        var data = google.visualization.arrayToDataTable([" +
                                            "['Country', '" + label + "'],"+
                                            dataToLoad +
                        "        ]);" +
                        "\n" +
                        "        var options = {" +
                        "legend: 'none'," +
                        "keepAspectRatio: true," +
                        "region: 'world'," +
                        "enableRegionInteractivity: true," +
                        "};\n" +
                        "\n" +
                        "        var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));\n" +
                        "\n" +
                        "        chart.draw(data, options);\n" +
                        "      }\n" +
                        "    </script>\n" +
                        "  </head>\n" +
                        "  <body>\n" +
                        "    <div id=\"regions_div\" style=\"width: 100%; height: 100%;\"></div>\n" +
                        "  </body>\n" +
                        "</html>";


                WebView webView = new WebView(getActivity());
                mTopPagerContainer.addView(webView);

                int width = mTopPagerContainer.getWidth();
                int height = width * 3 / 4;

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) webView.getLayoutParams();
                params.width = WebView.LayoutParams.MATCH_PARENT;
                params.height = height;

                webView.setLayoutParams(params);

                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                webView.loadData(html_value, "text/html", "UTF-8");

            }
        });
        mTopPagerContainer.setVisibility(View.VISIBLE);
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
