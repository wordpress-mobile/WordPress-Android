package org.wordpress.android.ui.stats;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;

/**
 * Created by nbradbury on 2/25/14.
 * View holder for stats_list_cell layout
 */
class StatsChildViewHolder {
    public final TextView entryTextView;
    public final TextView totalsTextView;
    public final NetworkImageView networkImageView;

    public StatsChildViewHolder(View view) {
        entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
        totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
        networkImageView = (NetworkImageView) view.findViewById(R.id.stats_list_cell_image);

        networkImageView.setErrorImageResId(R.drawable.stats_blank_image);
        networkImageView.setDefaultImageResId(R.drawable.stats_blank_image);
    }
}
