package org.wordpress.android.ui.stats.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.StatsViewHolder;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.util.FormatUtils;

import java.util.List;

public class PostsAndPagesAdapter extends ArrayAdapter<PostModel> {

    private final List<PostModel> list;
    private final LayoutInflater inflater;

    public PostsAndPagesAdapter(Context context, List<PostModel> list) {
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

        final PostModel currentRowData = list.get(position);
        StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

        // Entry
        holder.setEntryTextOpenDetailsPage(currentRowData);

        // Setup the more button
        holder.setMoreButtonOpenInReader(currentRowData);

        // totals
        holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));

        // no icon
        holder.networkImageView.setVisibility(View.GONE);
        return rowView;
    }
}
