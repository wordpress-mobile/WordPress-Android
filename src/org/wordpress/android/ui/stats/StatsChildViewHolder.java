package org.wordpress.android.ui.stats;

import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;

/**
 * Created by nbradbury on 2/25/14.
 * View holder for stats_list_cell layout
 */
public class StatsChildViewHolder {
    public final TextView entryTextView;
    public final TextView totalsTextView;

    public StatsChildViewHolder(View view) {
        entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
        totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
    }
}
