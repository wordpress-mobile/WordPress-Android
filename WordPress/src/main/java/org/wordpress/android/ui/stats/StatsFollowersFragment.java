package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.stats.models.FollowDataModel;
import org.wordpress.android.ui.stats.models.FollowerModel;
import org.wordpress.android.ui.stats.models.FollowersModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class StatsFollowersFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsFollowersFragment.class.getSimpleName();

    private HashSet<String> dotComUserBlogsURL = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Resources res = container.getContext().getResources();

        String[] titles = {
                res.getString(R.string.stats_followers_wpcom_selector),
                res.getString(R.string.stats_followers_email_selector),
        };


        setupTopModulePager(inflater, container, view, titles);

        mTopPagerContainer.setVisibility(View.VISIBLE);
        mTotalsLabel.setVisibility(View.VISIBLE);
        mTotalsLabel.setText("");

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.STATS, this.getTag() + " > restoring instance state");
            if (savedInstanceState.containsKey(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX)) {
                mTopPagerSelectedButtonIndex = savedInstanceState.getInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX);
            }
        } else {
            // first time it's created
            mTopPagerSelectedButtonIndex = getArguments().getInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, 0);
        }

        // Single background thread used to create the blogs list in BG
        ThreadPoolExecutor blogsListCreatorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        blogsListCreatorExecutor.submit(new Thread() {
            @Override
            public void run() {
                // Read all the dotcomBlog blogs and get the list of home URLs.
                // This will be used later to check if the user is a member of followers blog marked as private.
                List <Map<String, Object>> dotComUserBlogs = WordPress.wpDB.getAccountsBy("dotcomFlag=1", new String[]{"homeURL"});
                for (Map<String, Object> blog : dotComUserBlogs) {
                    if (blog != null && blog.get("homeURL") != null) {
                        String normURL = normalizeAndRemoveScheme(blog.get("homeURL").toString());
                        dotComUserBlogsURL.add(normURL);
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //AppLog.d(AppLog.T.STATS, this.getTag() + " > saving instance state");
        outState.putInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, mTopPagerSelectedButtonIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        mTopPagerContainer.setVisibility(View.VISIBLE);
        mTotalsLabel.setVisibility(View.VISIBLE);

        if (mDatamodels == null) {
            showHideNoResultsUI(true);
            mTotalsLabel.setText(getTotalFollowersLabel(0));
            return;
        }

        if (isErrorResponse()) {
            showErrorUI();
            return;
        }

        final FollowersModel followersModel = (FollowersModel) mDatamodels[mTopPagerSelectedButtonIndex];
        if (followersModel != null && followersModel.getFollowers() != null &&
                followersModel.getFollowers().size() > 0) {
            ArrayAdapter adapter = new DotComFollowerAdapter(getActivity(), followersModel.getFollowers());
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);

            if (mTopPagerSelectedButtonIndex == 0) {
                mTotalsLabel.setText(getTotalFollowersLabel(followersModel.getTotalWPCom()));
            } else {
                mTotalsLabel.setText(getTotalFollowersLabel(followersModel.getTotalEmail()));
            }

            if (isSingleView()) {
                if (followersModel.getPages() > 1) {
                    mBottomPaginationContainer.setVisibility(View.VISIBLE);
                    mTopPaginationContainer.setVisibility(View.VISIBLE);
                    String paginationLabel = String.format(
                            getString(R.string.stats_pagination_label),
                            FormatUtils.formatDecimal(followersModel.getPage()),
                            FormatUtils.formatDecimal(followersModel.getPages())
                    );
                    mBottomPaginationText.setText(paginationLabel);
                    mTopPaginationText.setText(paginationLabel);
                    setNavigationButtonsEnabled(true);

                    // Setting up back buttons
                    if (followersModel.getPage() == 1) {
                        // first page. No go back buttons
                        setNavigationBackButtonsVisibility(false);
                    } else {
                        setNavigationBackButtonsVisibility(true);
                        View.OnClickListener clickListener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setNavigationButtonsEnabled(false);
                                mMoreDataListener.onMoreDataRequested(
                                        getSectionsToUpdate()[mTopPagerSelectedButtonIndex],
                                        followersModel.getPage() - 1
                                );
                            }
                        };
                        mBottomPaginationGoBackButton.setOnClickListener(clickListener);
                        mTopPaginationGoBackButton.setOnClickListener(clickListener);
                    }

                    // Setting up forward buttons
                    if (followersModel.getPage() == followersModel.getPages()) {
                        // last page. No go forward buttons
                        setNavigationForwardButtonsVisibility(false);
                    } else {
                        setNavigationForwardButtonsVisibility(true);
                        View.OnClickListener clickListener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setNavigationButtonsEnabled(false);
                                mMoreDataListener.onMoreDataRequested(
                                        getSectionsToUpdate()[mTopPagerSelectedButtonIndex],
                                        followersModel.getPage() + 1
                                );
                            }
                        };
                        mBottomPaginationGoForwardButton.setOnClickListener(clickListener);
                        mTopPaginationGoForwardButton.setOnClickListener(clickListener);
                    }

                    // Change the total number of followers label by adding the current paging info
                    int startIndex = followersModel.getPage() * StatsViewAllActivity.MAX_RESULTS_PER_PAGE - StatsViewAllActivity.MAX_RESULTS_PER_PAGE + 1;
                    int endIndex = startIndex + followersModel.getFollowers().size() - 1;
                    String pagedLabel  = getString(
                            mTopPagerSelectedButtonIndex == 0 ? R.string.stats_followers_total_wpcom_paged : R.string.stats_followers_total_email_paged,
                            startIndex,
                            endIndex,
                            FormatUtils.formatDecimal(mTopPagerSelectedButtonIndex == 0 ? followersModel.getTotalWPCom() : followersModel.getTotalEmail())
                    );
                    mTotalsLabel.setText(pagedLabel);
                } else {
                    // No paging required. Hide the controls.
                    mBottomPaginationContainer.setVisibility(View.GONE);
                    mTopPaginationContainer.setVisibility(View.GONE);
                }
            }
        } else {
            showHideNoResultsUI(true);
            mBottomPaginationContainer.setVisibility(View.GONE);
            mTotalsLabel.setText(getTotalFollowersLabel(0));
        }
    }

    private void setNavigationBackButtonsVisibility(boolean visible) {
        mBottomPaginationGoBackButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        mTopPaginationGoBackButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private void setNavigationForwardButtonsVisibility(boolean visible) {
        mBottomPaginationGoForwardButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        mTopPaginationGoForwardButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private void setNavigationButtonsEnabled(boolean enable) {
        mBottomPaginationGoBackButton.setEnabled(enable);
        mBottomPaginationGoForwardButton.setEnabled(enable);
        mTopPaginationGoBackButton.setEnabled(enable);
        mTopPaginationGoForwardButton.setEnabled(enable);
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        if (isDataEmpty()) {
            return false;
        }
        FollowersModel followersModel = (FollowersModel) mDatamodels[mTopPagerSelectedButtonIndex];
        return !(followersModel == null || followersModel.getFollowers() == null
                || followersModel.getFollowers().size() < MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST);

    }

    private String getTotalFollowersLabel(int total) {
        if ( mTopPagerSelectedButtonIndex == 0 ) {
            return getString(R.string.stats_followers_total_wpcom, FormatUtils.formatDecimal(total));
        }

        return  getString(R.string.stats_followers_total_email, FormatUtils.formatDecimal(total));
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class DotComFollowerAdapter extends ArrayAdapter<FollowerModel> {

        private final List<FollowerModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public DotComFollowerAdapter(Activity context, List<FollowerModel> list) {
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
                rowView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // set a min-width value that is large enough to contains the "since" string
                LinearLayout totalContainer = (LinearLayout) rowView.findViewById(R.id.stats_list_cell_total_container);
                int dp64 = DisplayUtils.dpToPx(rowView.getContext(), 64);
                totalContainer.setMinimumWidth(dp64);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final FollowerModel currentRowData = list.get(position);
            final StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

            holder.entryTextView.setTextColor(context.getResources().getColor(R.color.stats_text_color));
            holder.rowContent.setClickable(false);

            final FollowDataModel followData = currentRowData.getFollowData();

            // entries
            if (mTopPagerSelectedButtonIndex == 0 && !TextUtils.isEmpty(currentRowData.getURL())) {
                // WPCOM followers with no empty URL

                boolean openInReader = true;
                if (followData == null) {
                    // If follow data is empty, we cannot follow the blog, or access it in the reader.
                    // We need to check if the user is a member of this blog.
                    // If so, we can launch open the reader, otherwise open the blog in the in-app browser.
                    String normURL = normalizeAndRemoveScheme(currentRowData.getURL());
                    openInReader = dotComUserBlogsURL.contains(normURL);
                }

                if (openInReader) {
                    holder.entryTextView.setText(currentRowData.getLabel());
                    holder.rowContent.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ReaderActivityLauncher.showReaderBlogPreview(
                                            context,
                                            0L,
                                            currentRowData.getURL()
                                    );
                                }
                            });
                } else {
                    holder.setEntryTextOrLink(currentRowData.getURL(), currentRowData.getLabel());
                }
                holder.entryTextView.setTextColor(context.getResources().getColor(R.color.stats_link_text_color));
            } else {
                // Email followers, or wpcom followers with empty URL
                holder.setEntryText(currentRowData.getLabel());
            }

            // since date
            holder.totalsTextView.setText(getSinceLabel(currentRowData.getDateSubscribed()));

            // Avatar
            holder.networkImageView.setImageUrl(PhotonUtils.fixAvatar(currentRowData.getAvatar(), mResourceVars.headerAvatarSizePx), WPNetworkImageView.ImageType.AVATAR);
            holder.networkImageView.setVisibility(View.VISIBLE);

            if (followData == null) {
                holder.imgMore.setVisibility(View.GONE);
                holder.imgMore.setClickable(false);
            } else {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FollowHelper fh = new FollowHelper(context);
                        fh.showPopup(holder.imgMore, followData);
                    }
                });
            }

            return rowView;
        }

        private int roundUp(double num, double divisor) {
            double unrounded = num / divisor;
            return (int) (unrounded + 0.5);
        }

        private String getSinceLabel(String dataSubscribed) {

            Date currentDateTime = new Date();

            try {
                SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                Date date = from.parse(dataSubscribed);

                // See http://momentjs.com/docs/#/displaying/fromnow/
                long currentDifference = Math.abs(
                        StatsUtils.getDateDiff(date, currentDateTime, TimeUnit.SECONDS)
                );

                if (currentDifference <= 45 ) {
                    return getString(R.string.stats_followers_seconds_ago);
                }
                if (currentDifference < 90 ) {
                    return getString(R.string.stats_followers_a_minute_ago);
                }

                // 90 seconds to 45 minutes
                if (currentDifference <= 2700 ) {
                    long minutes = this.roundUp(currentDifference, 60);
                    return getString(
                            R.string.stats_followers_minutes,
                            minutes
                    );
                }

                // 45 to 90 minutes
                if (currentDifference <= 5400 ) {
                    return getString(R.string.stats_followers_an_hour_ago);
                }

                // 90 minutes to 22 hours
                if (currentDifference <= 79200 ) {
                    long hours = this.roundUp(currentDifference, 60*60);
                    return getString(
                            R.string.stats_followers_hours,
                            hours
                    );
                }

                // 22 to 36 hours
                if (currentDifference <= 129600 ) {
                    return getString(R.string.stats_followers_a_day);
                }

                // 36 hours to 25 days
                // 86400 secs in a day -  2160000 secs in 25 days
                if (currentDifference <= 2160000 ) {
                    long days = this.roundUp(currentDifference, 86400);
                    return getString(
                            R.string.stats_followers_days,
                            days
                    );
                }

                // 25 to 45 days
                // 3888000 secs in 45 days
                if (currentDifference <= 3888000 ) {
                    return getString(R.string.stats_followers_a_month);
                }

                // 45 to 345 days
                // 2678400 secs in a month - 29808000 secs in 345 days
                if (currentDifference <= 29808000 ) {
                    long months = this.roundUp(currentDifference, 2678400);
                    return getString(
                            R.string.stats_followers_months,
                            months
                    );
                }

                // 345 to 547 days (1.5 years)
                if (currentDifference <= 47260800 ) {
                    return getString(R.string.stats_followers_a_year);
                }

                // 548 days+
                // 31536000 secs in a year
                long years = this.roundUp(currentDifference, 31536000);
                return getString(
                        R.string.stats_followers_years,
                        years
                );

            } catch (ParseException e) {
                AppLog.e(AppLog.T.STATS, e);
            }

            return "";
        }
    }

    private static String normalizeAndRemoveScheme(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        String normURL = UrlUtils.normalizeUrl(url.toLowerCase());
        int pos = normURL.indexOf("://");
        if (pos > -1) {
            return normURL.substring(pos + 3);
        } else {
            return normURL;
        }
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_followers;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_followers;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_followers;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_followers_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.FOLLOWERS_WPCOM, StatsService.StatsEndpointsEnum.FOLLOWERS_EMAIL
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_followers);
    }
}
