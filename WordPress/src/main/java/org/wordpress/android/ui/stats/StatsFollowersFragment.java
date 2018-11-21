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
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.stats.models.FollowDataModel;
import org.wordpress.android.ui.stats.models.FollowerModel;
import org.wordpress.android.ui.stats.models.FollowersModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.stats.service.StatsServiceLogic;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.image.ImageType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class StatsFollowersFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsFollowersFragment.class.getSimpleName();

    private static final String ARG_REST_RESPONSE_FOLLOWERS_EMAIL = "ARG_REST_RESPONSE_FOLLOWERS_EMAIL";
    private final Map<String, Long> mUserBlogs = new HashMap<>();

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

        // Single background thread used to create the blogs list in BG
        ThreadPoolExecutor blogsListCreatorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        blogsListCreatorExecutor.submit(new Thread() {
            @Override
            public void run() {
                // Read all the WPComRest accessed sites and get the list of home URLs.
                // This will be used later to check if the user is a member of followers blog marked as private.
                List<SiteModel> sites = mSiteStore.getSitesAccessedViaWPComRest();
                for (SiteModel site : sites) {
                    if (site.getUrl() != null && site.getSiteId() != 0) {
                        String normURL = normalizeAndRemoveScheme(site.getUrl());
                        long blogID = site.getSiteId();
                        mUserBlogs.put(normURL, blogID);
                    }
                }
            }
        });
    }

    private FollowersModel mFollowersWPCOM;
    private FollowersModel mFollowersEmail;

    @Override
    protected boolean hasDataAvailable() {
        return mFollowersWPCOM != null || mFollowersEmail != null;
    }

    @Override
    protected void saveStatsData(Bundle outState) {
        if (mFollowersWPCOM != null) {
            outState.putSerializable(ARG_REST_RESPONSE, mFollowersWPCOM);
        }
        if (mFollowersEmail != null) {
            outState.putSerializable(ARG_REST_RESPONSE_FOLLOWERS_EMAIL, mFollowersEmail);
        }
    }

    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mFollowersWPCOM = (FollowersModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE_FOLLOWERS_EMAIL)) {
            mFollowersEmail = (FollowersModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE_FOLLOWERS_EMAIL);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.FollowersWPCOMUdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mFollowersWPCOM = event.mFollowers;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.FollowersEmailUdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mFollowersEmail = event.mFollowers;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mFollowersWPCOM = null;
        mFollowersEmail = null;
        showErrorUI(event.mError);
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (!hasDataAvailable()) {
            showHideNoResultsUI(true);
            mTotalsLabel.setText(getTotalFollowersLabel(0));
            return;
        }

        mTotalsLabel.setVisibility(View.VISIBLE);

        final FollowersModel followersModel = getCurrentDataModel();

        if (followersModel != null && followersModel.getFollowers() != null
            && followersModel.getFollowers().size() > 0) {
            ArrayAdapter adapter = new WPComFollowerAdapter(getActivity(), followersModel.getFollowers());
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
                                refreshStats(
                                        followersModel.getPage() - 1,
                                        new StatsServiceLogic.StatsEndpointsEnum[]{
                                                sectionsToUpdate()[mTopPagerSelectedButtonIndex]}
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
                                refreshStats(
                                        followersModel.getPage() + 1,
                                        new StatsServiceLogic.StatsEndpointsEnum[]{
                                                sectionsToUpdate()[mTopPagerSelectedButtonIndex]}
                                );
                            }
                        };
                        mBottomPaginationGoForwardButton.setOnClickListener(clickListener);
                        mTopPaginationGoForwardButton.setOnClickListener(clickListener);
                    }

                    // Change the total number of followers label by adding the current paging info
                    int startIndex = followersModel.getPage() * StatsService.MAX_RESULTS_REQUESTED_PER_PAGE
                                     - StatsService.MAX_RESULTS_REQUESTED_PER_PAGE + 1;
                    int endIndex = startIndex + followersModel.getFollowers().size() - 1;
                    String pagedLabel = getString(
                            mTopPagerSelectedButtonIndex == 0 ? R.string.stats_followers_total_wpcom_paged
                                    : R.string.stats_followers_total_email_paged,
                            startIndex,
                            endIndex,
                            FormatUtils.formatDecimal(
                                    mTopPagerSelectedButtonIndex == 0 ? followersModel.getTotalWPCom()
                                            : followersModel.getTotalEmail())
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

        // Always visible. Even if the current tab is empty, otherwise the user can't switch tab
        mTopPagerContainer.setVisibility(View.VISIBLE);
    }

    private FollowersModel getCurrentDataModel() {
        return mTopPagerSelectedButtonIndex == 0 ? mFollowersWPCOM : mFollowersEmail;
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
        if (!hasDataAvailable()) {
            return false;
        }
        FollowersModel followersModel = getCurrentDataModel();
        return !(followersModel == null || followersModel.getFollowers() == null
                 || followersModel.getFollowers().size() < MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST);
    }

    private String getTotalFollowersLabel(int total) {
        final String totalFollowersLabel;

        if (mTopPagerSelectedButtonIndex == 0) {
            totalFollowersLabel = getString(R.string.stats_followers_total_wpcom);
        } else {
            totalFollowersLabel = getString(R.string.stats_followers_total_email);
        }

        return String.format(totalFollowersLabel, FormatUtils.formatDecimal(total));
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class WPComFollowerAdapter extends ArrayAdapter<FollowerModel> {
        private final List<FollowerModel> mList;
        private final Activity mContext;
        private final LayoutInflater mInflater;

        WPComFollowerAdapter(Activity context, List<FollowerModel> list) {
            super(context, R.layout.stats_list_cell, list);
            mContext = context;
            mList = list;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                rowView = mInflater.inflate(R.layout.stats_list_cell, parent, false);
                // set a min-width value that is large enough to contains the "since" string
                LinearLayout totalContainer = (LinearLayout) rowView.findViewById(R.id.stats_list_cell_total_container);
                int dp64 = DisplayUtils.dpToPx(rowView.getContext(), 64);
                totalContainer.setMinimumWidth(dp64);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final FollowerModel currentRowData = mList.get(position);
            final StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

            holder.entryTextView.setTextColor(mContext.getResources().getColor(R.color.stats_text_color));
            holder.rowContent.setClickable(false);

            final FollowDataModel followData = currentRowData.getFollowData();

            // entries
            if (mTopPagerSelectedButtonIndex == 0 && !(TextUtils.isEmpty(currentRowData.getURL())
                                                       && followData == null)) {
                // WPCOM followers with no empty URL or empty follow data

                final long blogID;
                if (followData == null) {
                    // If follow data is empty, we cannot follow the blog, or access it in the reader.
                    // We need to check if the user is a member of this blog.
                    // If so, we can launch open the reader, otherwise open the blog in the in-app browser.
                    String normURL = normalizeAndRemoveScheme(currentRowData.getURL());
                    blogID = mUserBlogs.containsKey(normURL) ? mUserBlogs.get(normURL) : -1;
                } else {
                    blogID = followData.getSiteID();
                }

                if (blogID > -1) {
                    // Open the Reader
                    holder.entryTextView.setText(currentRowData.getLabel());
                    holder.rowContent.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ReaderActivityLauncher.showReaderBlogPreview(mContext, blogID);
                                }
                            });
                } else {
                    // Open the in-app web browser
                    holder.setEntryTextOrLink(currentRowData.getURL(), currentRowData.getLabel());
                }
                holder.entryTextView.setTextColor(mContext.getResources().getColor(R.color.stats_link_text_color));
            } else {
                // Email followers, or wpcom followers with empty URL and no blogID
                holder.setEntryText(currentRowData.getLabel());
            }

            // since date
            holder.totalsTextView.setText(StatsUtils.getSinceLabel(mContext, currentRowData.getDateSubscribed()));
            holder.totalsTextView.setContentDescription(
                    holder.totalsTextView.getContext().getString(
                            R.string.stats_follower_since_desc,
                            holder.totalsTextView.getText()));

            // Avatar
            mImageManager.loadIntoCircle(holder.networkImageView, ImageType.AVATAR_WITH_BACKGROUND,
                    GravatarUtils.fixGravatarUrl(currentRowData.getAvatar(), mResourceVars.mHeaderAvatarSizePx));
            holder.networkImageView.setVisibility(View.VISIBLE);

            if (followData == null) {
                holder.imgMore.setVisibility(View.GONE);
                holder.imgMore.setClickable(false);
            } else {
                holder.imgMore.setVisibility(View.VISIBLE);
                holder.imgMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FollowHelper fh = new FollowHelper(mContext);
                        fh.showPopup(holder.imgMore, followData);
                    }
                });
            }

            return rowView;
        }
    }

    private static String normalizeAndRemoveScheme(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        String normURL = UrlUtils.normalizeUrl(url.toLowerCase(Locale.ROOT));
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
    protected StatsServiceLogic.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsServiceLogic.StatsEndpointsEnum[]{
                StatsServiceLogic.StatsEndpointsEnum.FOLLOWERS_WPCOM,
                StatsServiceLogic.StatsEndpointsEnum.FOLLOWERS_EMAIL
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_followers);
    }
}
