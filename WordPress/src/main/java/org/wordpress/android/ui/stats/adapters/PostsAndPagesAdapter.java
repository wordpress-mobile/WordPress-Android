package org.wordpress.android.ui.stats.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.StatsViewHolder;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.util.FormatUtils;

import java.util.List;

public class PostsAndPagesAdapter extends ArrayAdapter<SingleItemModel> {

    private List<SingleItemModel> list;
    private final Activity context;
    private final LayoutInflater inflater;
    private final int localTableBlogID;

    public PostsAndPagesAdapter(Activity context, int localTableBlogID, List<SingleItemModel> list) {
        super(context, R.layout.stats_list_cell, list);
        this.context = context;
        this.list = list;
        this.localTableBlogID = localTableBlogID;
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

        // Entry
        holder.setEntryTextOpenInreader(context, currentRowData);

        holder.imgMore.setVisibility(View.GONE);

        // totals
        holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));

        // no icon
        holder.networkImageView.setVisibility(View.GONE);
        return rowView;
    }
}
