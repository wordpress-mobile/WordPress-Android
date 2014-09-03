package org.wordpress.android.ui.stats;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsVideosTable;
import org.wordpress.android.models.StatsVideoSummary;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.FormatUtils;

/**
 * Fragment for video stats. Has three pages, for Today's and Yesterday's stats as well as a summary page.
 */
public class StatsVideoFragment extends StatsAbsPagedViewFragment {
    private static final Uri STATS_VIDEOS_URI = StatsContentProvider.STATS_VIDEOS_URI;
    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY, StatsTimeframe.SUMMARY };

    public static final String TAG = StatsVideoFragment.class.getSimpleName();

    @Override
    protected Fragment getFragment(int position) {
        if (position < 2) {
            int entryLabelResId = R.string.stats_entry_video_plays;
            int totalsLabelResId = R.string.stats_totals_plays;
            int emptyLabelResId = R.string.stats_empty_video;

            Uri uri = Uri.parse(STATS_VIDEOS_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());

            StatsCursorFragment fragment = StatsCursorFragment.newInstance(uri, entryLabelResId, totalsLabelResId, emptyLabelResId, getLocalTableBlogID());
            fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
            fragment.setCallback(this);
            return fragment;
        } else {
            return new VideoSummaryFragment();
        }
    }

    public class CustomCursorAdapter extends CursorAdapter {
        private final LayoutInflater inflater;

        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            View view = inflater.inflate(R.layout.stats_list_cell, root, false);
            view.setTag(new StatsViewHolder(view));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            String entry = cursor.getString(cursor.getColumnIndex(StatsVideosTable.Columns.NAME));
            String url = cursor.getString(cursor.getColumnIndex(StatsVideosTable.Columns.URL));
            int total = cursor.getInt(cursor.getColumnIndex(StatsVideosTable.Columns.PLAYS));

            // entries
            holder.setEntryTextOrLink(url, entry);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));
        }
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_video_plays);
    }

    @Override
    protected String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }

    @Override
    protected int getInnerFragmentID() {
        return R.id.stats_pager_container;
    }

    /**
     * Fragment used for video summary
     */
    public static class VideoSummaryFragment extends Fragment {
        private TextView mHeader;
        private TextView mPlays;
        private TextView mImpressions;
        private TextView mPlaybackTotals;
        private TextView mPlaybackUnit;
        private TextView mBandwidth;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.stats_video_summary, container, false);

            mHeader = (TextView) view.findViewById(R.id.stats_video_summary_header);
            mHeader.setText("");
            mPlays = (TextView) view.findViewById(R.id.stats_video_summary_plays_total);
            mImpressions = (TextView) view.findViewById(R.id.stats_video_summary_impressions_total);
            mPlaybackTotals = (TextView) view.findViewById(R.id.stats_video_summary_playback_length_total);
            mPlaybackUnit = (TextView) view.findViewById(R.id.stats_video_summary_playback_length_unit);
            mBandwidth = (TextView) view.findViewById(R.id.stats_video_summary_bandwidth_total);

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            refreshSummary();
            refreshStatsFromServer();
        }

        private void refreshSummary() {
            if (WordPress.getCurrentBlog() == null)
                return;

            String blogId = String.valueOf(WordPress.getCurrentBlog());

            new AsyncTask<String, Void, StatsVideoSummary>() {
                @Override
                protected StatsVideoSummary doInBackground(String... params) {
                    final String blogId = params[0];
                    return StatsUtils.getVideoSummary(blogId);
                }

                protected void onPostExecute(StatsVideoSummary result) {
                    refreshSummaryViews(result);
                }
            }.execute(blogId);
        }


        private void refreshStatsFromServer() {
            if (WordPress.getCurrentBlog() == null)
                return;

            final String blogId = String.valueOf(WordPress.getCurrentBlog());

        /*    WordPress.getRestClientUtils().getStatsVideoSummary(blogId,
                    new Listener() {
                        @Override
                        public void onResponse(final JSONObject response) {
                            new AsyncTask<Void, Void, Void> () {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    StatUtils.saveVideoSummary(blogId, response);
                                    return null;
                                }

                                protected void onPostExecute(Void result) {
                                    if (getActivity() == null)
                                        return;

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            refreshSummary();
                                        }
                                    });
                                }

                            }.execute();

                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub

                        }
                    });*/
        }

        void refreshSummaryViews(StatsVideoSummary result) {
            String header = "";
            int plays = 0;
            int impressions = 0;
            int playbackTotals = 0;
            String bandwidth = "0 MB";

            if (result != null) {
                plays = result.getPlays();
                impressions = result.getImpressions();
                playbackTotals = result.getMinutes();
                bandwidth = result.getBandwidth();
                header = String.format(getString(R.string.stats_video_summary_header), result.getTimeframe());
            }

            mHeader.setText(header);
            mPlays.setText(FormatUtils.formatDecimal(plays));
            mImpressions.setText(FormatUtils.formatDecimal(impressions));
            mPlaybackTotals.setText(playbackTotals + "");
            mBandwidth.setText(bandwidth);
        }


    }
}
