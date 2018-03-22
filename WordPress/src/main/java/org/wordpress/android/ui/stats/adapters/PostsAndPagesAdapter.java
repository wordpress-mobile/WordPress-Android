package org.wordpress.android.ui.stats.adapters;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.StatsViewHolder;
import org.wordpress.android.ui.stats.models.StatsPostModel;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StringUtils;

import java.util.List;

public class PostsAndPagesAdapter extends ArrayAdapter<StatsPostModel> {
    private final List<StatsPostModel> mList;
    private final LayoutInflater mInflater;
    private final boolean mAnnounceValueAsComments;

    public PostsAndPagesAdapter(Context context, List<StatsPostModel> list, boolean announceValueAsComments) {
        super(context, R.layout.stats_list_cell, list);
        mList = list;
        mInflater = LayoutInflater.from(context);
        mAnnounceValueAsComments = announceValueAsComments;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            rowView = mInflater.inflate(R.layout.stats_list_cell, parent, false);
            // configure view holder
            StatsViewHolder viewHolder = new StatsViewHolder(rowView);
            rowView.setTag(viewHolder);
        }

        final StatsPostModel currentRowData = mList.get(position);
        StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

        // Entry
        holder.setEntryTextOpenDetailsPage(currentRowData);

        // Setup the more button
        holder.setMoreButtonOpenInReader(currentRowData);

        // totals
        holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));
        if (mAnnounceValueAsComments) {
            holder.totalsTextView.setContentDescription(
                    StringUtils.getQuantityString(
                            holder.totalsTextView.getContext(),
                            R.string.stats_comments_zero_desc,
                            R.string.stats_comments_one_desc,
                            R.string.stats_comments_many_desc,
                            currentRowData.getTotals()));
        } else {
            holder.totalsTextView.setContentDescription(
                    StringUtils.getQuantityString(
                            holder.totalsTextView.getContext(),
                            R.string.stats_views_zero_desc,
                            R.string.stats_views_one_desc,
                            R.string.stats_views_many_desc,
                            currentRowData.getTotals()));
        }

        // no icon
        holder.networkImageView.setVisibility(View.GONE);
        return rowView;
    }
}
