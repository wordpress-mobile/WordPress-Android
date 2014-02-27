package org.wordpress.android.ui.stats;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;

/**
 * Created by nbradbury on 2/25/14.
 * View holder for stats_group_cell layout
 */
class StatsGroupViewHolder {
    public final TextView entryTextView;
    public final TextView totalsTextView;
    public final NetworkImageView networkImageView;
    public final ImageView chevronImageView;

    public StatsGroupViewHolder(View view) {
        entryTextView = (TextView) view.findViewById(R.id.stats_group_cell_entry);
        totalsTextView = (TextView) view.findViewById(R.id.stats_group_cell_total);
        chevronImageView = (ImageView) view.findViewById(R.id.stats_group_cell_chevron);
        networkImageView = (NetworkImageView) view.findViewById(R.id.stats_group_cell_image);

        networkImageView.setErrorImageResId(R.drawable.stats_blank_image);
        networkImageView.setDefaultImageResId(R.drawable.stats_blank_image);
    }
}
