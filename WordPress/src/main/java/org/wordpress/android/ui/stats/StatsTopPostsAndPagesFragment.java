package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
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
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;


public class StatsTopPostsAndPagesFragment extends StatsAbstractFragment {
    public static final String TAG = StatsTopPostsAndPagesFragment.class.getSimpleName();

    private static final int NO_STRING_ID = -1;
    private TextView mEmptyLabel;
    private LinearLayout mListContainer;
    private LinearLayout mTopPostList;
    private ArrayAdapter mAdapter;
    private TopPostsAndPagesModel mTopPostsAndPagesModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_list_fragment, container, false);

        TextView titleTextView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleTextView.setText(getTitle().toUpperCase(Locale.getDefault()));

        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());
        TextView totalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        totalsLabel.setText(getTotalsLabelResId());
        mEmptyLabel = (TextView) view.findViewById(R.id.stats_list_empty_text);
        mTopPostList = (LinearLayout) view.findViewById(R.id.stats_list_linearlayout);
        mTopPostList.setVisibility(View.INVISIBLE);
        mListContainer = (LinearLayout) view.findViewById(R.id.stats_list_header_container);

        // Init the UI
        if (mTopPostsAndPagesModel != null) {
            updateUI();
        } else {
            showEmptyUI(true);
        }
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.STATS, "StatsTopPostsAndPagesFragment > restoring instance state");
            if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
                mTopPostsAndPagesModel = (TopPostsAndPagesModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(AppLog.T.STATS, "StatsTopPostsAndPagesFragment > saving instance state");
        outState.putSerializable(ARG_REST_RESPONSE, mTopPostsAndPagesModel);
        super.onSaveInstanceState(outState);
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
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_UPDATING));
    }

    private void updateUI() {
        if (mTopPostsAndPagesModel != null && mTopPostsAndPagesModel.getTopPostsAndPages().size() > 0) {
            List<TopPostModel> postViews = mTopPostsAndPagesModel.getTopPostsAndPages();
            setListAdapter(new TopPostsAndPagesAdapter(getActivity(), postViews));
            showEmptyUI(false);
        } else {
            showEmptyUI(true);
        }
    }

    private void showLoadingUI() {
        mEmptyLabel.setText("Loading...");
        mEmptyLabel.setVisibility(View.VISIBLE);
        mTopPostList.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);
        return;
    }

    private void showEmptyUI(boolean show) {
        if (show) {
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
            mEmptyLabel.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
            mTopPostList.setVisibility(View.GONE);
        } else {
            mEmptyLabel.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
            mTopPostList.setVisibility(View.VISIBLE);
        }
    }

    /*
 * receives broadcast when data has been updated
 */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = StringUtils.notNullStr(intent.getAction());

            if (!(action.equals(StatsService.ACTION_STATS_UPDATED) || action.equals(StatsService.ACTION_STATS_UPDATING))) {
                return;
            }

            if (!intent.hasExtra(StatsService.EXTRA_SECTION_NAME)) {
                return;
            }

            StatsService.StatsSectionEnum sectionToUpdate = (StatsService.StatsSectionEnum) intent.getSerializableExtra(StatsService.EXTRA_SECTION_NAME);
            if (sectionToUpdate != StatsService.StatsSectionEnum.TOP_POSTS) {
                return;
            }

            if (action.equals(StatsService.ACTION_STATS_UPDATED)) {
                Serializable dataObj = intent.getSerializableExtra(StatsService.EXTRA_SECTION_DATA);
               /* if (dataObj == null || dataObj instanceof VolleyError) {
                    //TODO: show the error on the section ???
                    return;
                }*/
                mTopPostsAndPagesModel = (dataObj == null || dataObj instanceof VolleyError) ? null : (TopPostsAndPagesModel) dataObj;
                updateUI();
            } if (action.equals(StatsService.ACTION_STATS_UPDATING)) {
               showLoadingUI();
            }

            return;
        }
    };

    public void setListAdapter(ArrayAdapter adapter) {
        mAdapter = adapter;
        StatsUIHelper.reloadLinearLayout(getActivity(), mAdapter, mTopPostList);
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

            final TopPostModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();
            // fill data
            // entries
            holder.setEntryTextOrLink(currentRowData.getUrl(), currentRowData.getTitle());
            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getViews()));

            holder.totalsTextView.setPaintFlags(holder.totalsTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            holder.totalsTextView.setTextColor(getResources().getColor(R.color.wordpress_blue));
            holder.totalsTextView.setOnClickListener(new
                             View.OnClickListener() {
                                 @Override
                                 public void onClick(View view) {
                                     AppLog.w(AppLog.T.STATS, currentRowData.getPostId() + "");
                                     Intent statsPostViewIntent = new Intent(getActivity(), StatsSinglePostDetailsActivity.class);
                                     statsPostViewIntent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, getLocalTableBlogID());
                                     statsPostViewIntent.putExtra(StatsSinglePostDetailsActivity.ARG_REMOTE_POST_ID, currentRowData.getPostId());
                                     getActivity().startActivity(statsPostViewIntent);
                                 }
                             });
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

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_top_posts_and_pages);
    }
}
