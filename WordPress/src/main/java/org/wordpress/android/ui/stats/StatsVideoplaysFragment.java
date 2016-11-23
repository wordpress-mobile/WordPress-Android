package org.wordpress.android.ui.stats;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.models.VideoPlaysModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;


public class StatsVideoplaysFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsVideoplaysFragment.class.getSimpleName();

    private VideoPlaysModel mVideos;

    @Override
    protected boolean hasDataAvailable() {
        return mVideos != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mVideos);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mVideos = (VideoPlaysModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.VideoPlaysUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mVideos = event.mVideos;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mVideos = null;
        showErrorUI(event.mError);
    }


    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (hasVideoplays()) {
            ArrayAdapter adapter = new TopPostsAndPagesAdapter(getActivity(), getVideoplays());
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasVideoplays() {
        return mVideos != null
                && mVideos.getPlays() != null
                && mVideos.getPlays().size() > 0;
    }

    private List<SingleItemModel> getVideoplays() {
        if (!hasVideoplays()) {
            return new ArrayList<SingleItemModel>(0);
        }
        return mVideos.getPlays();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return hasVideoplays() && getVideoplays().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST;
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class TopPostsAndPagesAdapter extends ArrayAdapter<SingleItemModel> {

        private final List<SingleItemModel> list;
        private final LayoutInflater inflater;

        public TopPostsAndPagesAdapter(Context context, List<SingleItemModel> list) {
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

            final SingleItemModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();
            // fill data
            // entries
            holder.setEntryTextOrLink(currentRowData.getUrl(), currentRowData.getTitle());

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));

            // no icon
            holder.networkImageView.setVisibility(View.GONE);

            return rowView;
        }
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_video_plays;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_plays;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_video;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_video_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.VIDEO_PLAYS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_videos);
    }
}
