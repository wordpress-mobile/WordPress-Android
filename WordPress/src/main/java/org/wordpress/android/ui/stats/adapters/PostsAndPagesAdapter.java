package org.wordpress.android.ui.stats.adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsSinglePostDetailsActivity;
import org.wordpress.android.ui.stats.StatsViewHolder;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.util.AppLog;
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
        // fill data
        // entries
//            holder.setEntryTextOrLink(currentRowData.getUrl(), currentRowData.getTitle());
        //          StatsUtils.removeUnderlines((Spannable)holder.entryTextView.getText());
        holder.entryTextView.setText(currentRowData.getTitle());
        holder.entryTextView.setTextColor(context.getResources().getColor(R.color.wordpress_blue));
        holder.entryTextView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppLog.w(AppLog.T.STATS, currentRowData.getItemID() + "");
                        Intent statsPostViewIntent = new Intent(context, StatsSinglePostDetailsActivity.class);
                        statsPostViewIntent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
                        statsPostViewIntent.putExtra(StatsSinglePostDetailsActivity.ARG_REMOTE_POST_ID, currentRowData.getItemID());
                        context.startActivity(statsPostViewIntent);
                    }
                });

        holder.entryTextView.setBackgroundResource(R.drawable.list_bg_selector);
        holder.entryTextView.setTextColor(context.getResources().getColor(R.color.wordpress_blue));

        holder.imgMore.setVisibility(View.VISIBLE);
        holder.imgMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(context, view);
                MenuItem menuItem = popup.getMenu().add(context.getString(R.string.stats_view));
                menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        WPWebViewActivity.openURL(context, currentRowData.getUrl());
                        return true;
                    }
                });
                popup.show();
            }
        });

        // totals
        holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));

        // no icon
        holder.networkImageView.setVisibility(View.GONE);
        return rowView;
    }
}
