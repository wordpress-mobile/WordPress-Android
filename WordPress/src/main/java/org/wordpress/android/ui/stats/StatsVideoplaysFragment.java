package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.models.VideoPlaysModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;

import java.util.List;


public class StatsVideoplaysFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsVideoplaysFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (isErrorResponse()) {
            showErrorUI();
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
        return !isDataEmpty()
                && ((VideoPlaysModel) mDatamodels[0]).getPlays() != null
                && ((VideoPlaysModel) mDatamodels[0]).getPlays().size() > 0;
    }

    private List<SingleItemModel> getVideoplays() {
        if (!hasVideoplays()) {
            return null;
        }
        return ((VideoPlaysModel) mDatamodels[0]).getPlays();
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
        private final Activity context;
        private final LayoutInflater inflater;

        public TopPostsAndPagesAdapter(Activity context, List<SingleItemModel> list) {
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
    protected StatsService.StatsEndpointsEnum[] getSectionToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.VIDEO_PLAYS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_videos);
    }
}
