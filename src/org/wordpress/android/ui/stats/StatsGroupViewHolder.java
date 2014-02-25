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
public class StatsGroupViewHolder {
    public final TextView entryTextView;
    public final TextView totalsTextView;
    public final View imageFrame;
    public final NetworkImageView networkImageView;
    public final ImageView errorImageView;

    public StatsGroupViewHolder(View view) {
        entryTextView = (TextView) view.findViewById(R.id.stats_group_cell_entry);
        errorImageView = (ImageView) view.findViewById(R.id.stats_group_cell_blank_image);
        imageFrame = view.findViewById(R.id.stats_group_cell_image_frame);
        networkImageView = (NetworkImageView) view.findViewById(R.id.stats_group_cell_image);
        totalsTextView = (TextView) view.findViewById(R.id.stats_group_cell_total);
    }
}
