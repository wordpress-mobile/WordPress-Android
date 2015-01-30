package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.SearchTermModel;
import org.wordpress.android.ui.stats.models.SearchTermsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.List;


public class StatsSearchTermsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsSearchTermsFragment.class.getSimpleName();

    private final static String UNKNOWN_SEARCH_TERMS_HELP_PAGE = "http://en.support.wordpress.com/stats/#search-engine-terms";

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (isErrorResponse()) {
            showErrorUI();
            return;
        }

        if (hasSearchTerms()) {
            ArrayAdapter adapter = new SearchTermsAdapter(getActivity(), getSearchTerms());
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasSearchTerms() {
        if (isDataEmpty()) {
            return false;
        }

        SearchTermsModel searchTerms = (SearchTermsModel) mDatamodels[0];
        if (searchTerms.getSearchTerms() != null && searchTerms.getSearchTerms().size() > 0) {
            return true;
        }

        return false;
    }

    private List<SearchTermModel> getSearchTerms() {
        if (!hasSearchTerms()) {
            return null;
        }

        return ((SearchTermsModel) mDatamodels[0]).getSearchTerms();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return hasSearchTerms() && getSearchTerms().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST;
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class SearchTermsAdapter extends ArrayAdapter<SearchTermModel> {

        private final List<SearchTermModel> list;
        private final LayoutInflater inflater;

        public SearchTermsAdapter(Activity context, List<SearchTermModel> list) {
            super(context, R.layout.stats_list_cell, list);
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

            final SearchTermModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

            String term = currentRowData.getTitle();

            if (currentRowData.isEncriptedTerms()) {
                holder.setEntryTextOrLink(UNKNOWN_SEARCH_TERMS_HELP_PAGE, getString(R.string.stats_search_terms_unknown_search_terms));
            } else {
                holder.setEntryText(term, getResources().getColor(R.color.stats_text_color));
            }

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));

            // image
            holder.networkImageView.setVisibility(View.GONE);

            return rowView;
        }
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_search_terms;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_search_terms;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_search_terms_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.SEARCH_TERMS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_search_terms);
    }
}