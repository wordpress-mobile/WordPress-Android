package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.TopPostModel;
import org.wordpress.android.ui.stats.model.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;


public class StatsTopPostsAndPagesFragment extends StatsAbstractFragment {
    public static final String TAG = StatsTopPostsAndPagesFragment.class.getSimpleName();

    private static final int NO_STRING_ID = -1;
    private TextView mEmptyLabel;
    private LinearLayout mLinearLayout;
    private ArrayAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_list_fragment, container, false);
        setRetainInstance(true);

        TextView titleTextView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleTextView.setText(getTitle().toUpperCase(Locale.getDefault()));

        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());
        TextView totalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        totalsLabel.setText(getTotalsLabelResId());
        mEmptyLabel = (TextView) view.findViewById(R.id.stats_list_empty_text);

        String label;
        if (getEmptyLabelDescResId() == NO_STRING_ID) {
            label = "<b>" + getString(getEmptyLabelTitleResId()) + "</b>";
        } else {
            label = "<b>" + getString(getEmptyLabelTitleResId()) + "</b> " + getString(getEmptyLabelDescResId());
        }
        if (label.contains("<")) {
            mEmptyLabel.setText(Html.fromHtml(label));
        } else {
            mEmptyLabel.setText(label);
        }
        configureEmptyLabel();

        mLinearLayout = (LinearLayout) view.findViewById(R.id.stats_list_linearlayout);
        mLinearLayout.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_UPDATED));
    }


    /*
 * receives broadcast when data has been updated
 */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = StringUtils.notNullStr(intent.getAction());

            if (!action.equals(StatsService.ACTION_STATS_UPDATED) || !intent.hasExtra(StatsService.EXTRA_UPDATED_SECTION_NAME)) {
                return;
            }

            StatsService.StatsSectionEnum sectionToUpdate = (StatsService.StatsSectionEnum) intent.getSerializableExtra(StatsService.EXTRA_UPDATED_SECTION_NAME);
            if (sectionToUpdate != StatsService.StatsSectionEnum.TOP_POSTS) {
                return;
            }

            Serializable dataObj = intent.getSerializableExtra(StatsService.EXTRA_UPDATED_SECTION_DATA);
            if ( dataObj == null || dataObj instanceof VolleyError) {
                //TODO: show the error on the section ???
                return;
            }
            //TODO: check period and blogID
            final String blogId = StatsUtils.getBlogId(getLocalTableBlogID());

            TopPostsAndPagesModel topPostsAndPagesModel = (TopPostsAndPagesModel) dataObj;
            List<TopPostModel> postViews = topPostsAndPagesModel.getTopPostsAndPages();
            setListAdapter(new TopPostsAndPagesAdapter(getActivity(), postViews));
            return;
        }
    };

    public void setListAdapter(ArrayAdapter adapter) {
        mAdapter = adapter;
        StatsUIHelper.reloadLinearLayout(getActivity(), mAdapter, mLinearLayout);
    }

    public class TopPostsAndPagesAdapter extends ArrayAdapter<TopPostModel> {

        private final List<TopPostModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public TopPostsAndPagesAdapter(Activity context, List<TopPostModel> list) {
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

            TopPostModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();
            // fill data
            // entries
            holder.setEntryTextOrLink(currentRowData.getUrl(), currentRowData.getTitle());
            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getViews()));
            // no icon
            holder.networkImageView.setVisibility(View.GONE);

            return rowView;
        }
    }

    private int getEntryLabelResId() {
        return R.string.stats_entry_posts_and_pages;
    }

    private int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }

    private int getEmptyLabelTitleResId() {
        return R.string.stats_empty_top_posts_title;
    }

    private int getEmptyLabelDescResId() {
        return R.string.stats_empty_top_posts_desc;
    }

    private void configureEmptyLabel() {
        if (mAdapter == null || mAdapter.getCount() == 0)
            mEmptyLabel.setVisibility(View.VISIBLE);
        else
            mEmptyLabel.setVisibility(View.GONE);
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_top_posts_and_pages);
    }
}
