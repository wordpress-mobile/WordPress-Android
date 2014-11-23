package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.GeoviewModel;
import org.wordpress.android.ui.stats.model.GeoviewsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.List;


public class StatsGeoviewsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsGeoviewsFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (mDatamodels != null && mDatamodels[0] != null &&
                ((GeoviewsModel) mDatamodels[0]).getCountries() != null) {
            List<GeoviewModel> postViews = ((GeoviewsModel) mDatamodels[0]).getCountries();
            ArrayAdapter adapter = new GeoviewsAdapter(getActivity(), postViews);
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return (mDatamodels != null && mDatamodels[0] != null
                && ((GeoviewsModel) mDatamodels[0]).getCountries() != null
                && ((GeoviewsModel) mDatamodels[0]).getCountries().size() > 10);
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
                rowView = inflater.inflate(R.layout.stats_list_cell, null);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final GeoviewModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();
            // fill data
            String entry = currentRowData.getCountryFullName();
            String imageUrl = currentRowData.getImageUrl();
            int total = currentRowData.getViews();

            holder.entryTextView.setText(entry);
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // image (country flag)
            holder.showNetworkImage(imageUrl);

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
    protected StatsService.StatsEndpointsEnum[] getSectionToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.GEO_VIEWS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_views_by_country);
    }
}
