package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.SearchTermModel;
import org.wordpress.android.ui.stats.models.SearchTermsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;


public class StatsSearchTermsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsSearchTermsFragment.class.getSimpleName();

    private final static String UNKNOWN_SEARCH_TERMS_HELP_PAGE = "http://en.support.wordpress.com/stats/#search-engine-terms";

    private SearchTermsModel mSearchTerms;

    @Override
    protected boolean hasDataAvailable() {
        return mSearchTerms != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mSearchTerms);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mSearchTerms = (SearchTermsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SearchTermsUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mSearchTerms = event.mSearchTerms;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mSearchTerms = null;
        showErrorUI(event.mError);
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (hasSearchTerms()) {

            /**
             *  At this point we can have:
             *  - A list of search terms
             *  - A list of search terms + Encrypted item
             *  - Encrypted item only
             *
             *  We want to display max 10 items regardless the kind of the items, AND Encrypted
             *  must be present if available.
             *
             *  We need to do some counts then...
             */

            List<SearchTermModel> originalSearchTermList = mSearchTerms.getSearchTerms();
            List<SearchTermModel> mySearchTermList;
            if (originalSearchTermList == null) {
                // No clear-text search terms. we know we have the encrypted search terms item available
                mySearchTermList = new ArrayList<>(0);
            } else {
                // Make sure the list has MAX 9 items if the "Encrypted" is available
                // we want to show exactly 10 items per module
                if (mSearchTerms.getEncryptedSearchTerms() > 0 && originalSearchTermList.size() > getMaxNumberOfItemsToShowInList() - 1) {
                    mySearchTermList = new ArrayList<>();
                    int minIndex = Math.min(originalSearchTermList.size(), getMaxNumberOfItemsToShowInList() - 1);
                    for (int i = 0; i < minIndex; i++) {
                        mySearchTermList.add(originalSearchTermList.get(i));
                    }
                } else {
                    mySearchTermList = originalSearchTermList;
                }
            }
            ArrayAdapter adapter = new SearchTermsAdapter(getActivity(), mySearchTermList, mSearchTerms.getEncryptedSearchTerms());
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasSearchTerms() {
        return mSearchTerms != null
                && ((mSearchTerms.getSearchTerms() != null && mSearchTerms.getSearchTerms().size() > 0)
                        ||  mSearchTerms.getEncryptedSearchTerms() > 0
                );
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        if (!hasSearchTerms()) {
            return false;
        }


        int total = mSearchTerms.getSearchTerms() != null ? mSearchTerms.getSearchTerms().size() : 0;
        // If "Encrypted" is available we only have 9 items of clear text terms in the list
        if (mSearchTerms.getEncryptedSearchTerms() > 0) {
            return total > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST - 1;
        } else {
            return total > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST;
        }
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class SearchTermsAdapter extends ArrayAdapter<SearchTermModel> {

        private final List<SearchTermModel> list;
        private final LayoutInflater inflater;
        private final int encryptedSearchTerms;

        public SearchTermsAdapter(Activity context, List<SearchTermModel> list, int encryptedSearchTerms) {
            super(context, R.layout.stats_list_cell, list);
            this.list = list;
            this.encryptedSearchTerms = encryptedSearchTerms;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return super.getCount() + (encryptedSearchTerms > 0 ? 1 : 0);
        }

        @Override
        public SearchTermModel getItem(int position) {
            // If it's an element in the list returns it, otherwise it's the position of "Encrypted"
            if (position < super.getCount()) {
                return super.getItem(position);
            }

            return new SearchTermModel("", null, "Unknown Search Terms", encryptedSearchTerms, true);
        }

        @Override
        public int getPosition(SearchTermModel item) {
            if (item.isEncriptedTerms()) {
                return super.getCount(); // "Encrypted" is always at the end of the list
            }

            return super.getPosition(item);
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

            final SearchTermModel currentRowData = this.getItem(position);
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
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.SEARCH_TERMS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_search_terms);
    }
}