package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.SingleItemModel;
import org.wordpress.android.ui.stats.model.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FormatUtils;

import java.util.List;


public class StatsTopPostsAndPagesFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsTopPostsAndPagesFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (mDatamodel != null && ((TopPostsAndPagesModel) mDatamodel).getTopPostsAndPages().size() > 0) {
            List<SingleItemModel> postViews = ((TopPostsAndPagesModel) mDatamodel).getTopPostsAndPages();
            ArrayAdapter adapter = new TopPostsAndPagesAdapter(getActivity(), postViews);
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return (mDatamodel != null && ((TopPostsAndPagesModel) mDatamodel).getTopPostsAndPages().size() > 10);
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

            holder.totalsTextView.setPaintFlags(holder.totalsTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            holder.totalsTextView.setTextColor(getResources().getColor(R.color.wordpress_blue));
            holder.totalsTextView.setOnClickListener(new
                             View.OnClickListener() {
                                 @Override
                                 public void onClick(View view) {
                                     AppLog.w(AppLog.T.STATS, currentRowData.getItemID() + "");
                                     Intent statsPostViewIntent = new Intent(getActivity(), StatsSinglePostDetailsActivity.class);
                                     statsPostViewIntent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, getLocalTableBlogID());
                                     statsPostViewIntent.putExtra(StatsSinglePostDetailsActivity.ARG_REMOTE_POST_ID, currentRowData.getItemID());
                                     getActivity().startActivity(statsPostViewIntent);
                                 }
                             });
            // no icon
            holder.networkImageView.setVisibility(View.GONE);

            return rowView;
        }
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_posts_and_pages;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_top_posts_title;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_top_posts_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum getSectionToUpdate() {
        return StatsService.StatsEndpointsEnum.TOP_POSTS;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_top_posts_and_pages);
    }
}
