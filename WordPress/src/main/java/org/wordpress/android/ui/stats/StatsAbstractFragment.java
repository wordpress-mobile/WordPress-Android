package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.android.volley.NoConnectionError;
import com.android.volley.VolleyError;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.stats.service.StatsServiceLogic;
import org.wordpress.android.ui.stats.service.StatsServiceStarter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.image.ImageManager;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;


public abstract class StatsAbstractFragment extends Fragment {
    public static final String TAG = StatsAbstractFragment.class.getSimpleName();

    public static final String ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE";
    public static final String ARGS_TIMEFRAME = "ARGS_TIMEFRAME";
    public static final String ARGS_SELECTED_DATE = "ARGS_SELECTED_DATE";
    static final String ARG_REST_RESPONSE = "ARG_REST_RESPONSE";
    public static final String ARGS_IS_SINGLE_VIEW = "ARGS_IS_SINGLE_VIEW";

    // The number of results to return for NON Paged REST endpoints.
    private static final int MAX_RESULTS_REQUESTED = 100;

    private String mDate;
    private StatsTimeframe mStatsTimeframe = StatsTimeframe.DAY;

    protected abstract StatsServiceLogic.StatsEndpointsEnum[] sectionsToUpdate();

    protected abstract void showPlaceholderUI();

    protected abstract void updateUI();

    protected abstract void showErrorUI(String label);

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ImageManager mImageManager;

    /**
     * Wheter or not previous data is available.
     *
     * @return True if previous data is already available in the fragment
     */
    protected abstract boolean hasDataAvailable();

    /**
     * Called in onSaveIstance. Fragments should persist data here.
     *
     * @param outState Bundle in which to place fragment saved state.
     */
    protected abstract void saveStatsData(Bundle outState);

    /**
     * Called in OnCreate. Fragment should restore here previous saved data.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    protected abstract void restoreStatsData(Bundle savedInstanceState); // called in onCreate

    protected StatsResourceVars mResourceVars;

    public void refreshStats() {
        refreshStats(-1, null);
    }

    // call an update for the stats shown in the fragment
    void refreshStats(int pageNumberRequested, StatsServiceLogic.StatsEndpointsEnum[] sections) {
        if (!isAdded()) {
            return;
        }

        // if no sections to update is passed to the method, default to fragment
        if (sections == null) {
            sections = sectionsToUpdate();
        }

        SiteModel site = mSiteStore.getSiteByLocalId(getLocalTableBlogID());
        if (site == null) {
            AppLog.w(AppLog.T.STATS, "Current blsiteog is null. This should never happen here.");
            return;
        }

        final long siteId = site.getSiteId();
        // Make sure the blogId is available.
        if (siteId == 0) {
            AppLog.e(AppLog.T.STATS, "remote siteId is 0: " + site.getUrl());
            return;
        }

        // Check credentials for jetpack blogs first
        if (!SiteUtils.isAccessedViaWPComRest(site) && !mAccountStore.hasAccessToken()) {
            AppLog.w(AppLog.T.STATS, "Current blog is accessible via .com API without valid .com credentials");
            return;
        }

        // Do not pass the array of StatsEndpointsEnum to the Service. Otherwise we get
        // java.lang.RuntimeException: Unable to start service org.wordpress.android.ui.stats.service.StatsService
        // with Intent { cmp=org.wordpress.android/.ui.stats.service.StatsService (has extras) }:
        // java.lang.ClassCastException: java.lang.Object[] cannot be cast to
        // org.wordpress.android.ui.stats.service.StatsService$StatsEndpointsEnum[] on older devices.
        // We should use Enumset, or array of int. Going for the latter, since we have an array and cannot create
        // an Enumset easily.
        int[] sectionsForTheService = new int[sections.length];
        for (int i = 0; i < sections.length; i++) {
            sectionsForTheService[i] = sections[i].ordinal();
        }

        // start service to get stats
        Bundle extras = new Bundle();
        extras.putLong(StatsService.ARG_BLOG_ID, siteId);
        extras.putInt(StatsService.ARG_PERIOD, mStatsTimeframe.ordinal());
        extras.putString(StatsService.ARG_DATE, mDate);
        if (isSingleView()) {
            // Single Item screen: request 20 items per page on paged requests.
            // Default to the first 100 items otherwise.
            int maxElementsToRetrieve =
                    pageNumberRequested > 0 ? StatsService.MAX_RESULTS_REQUESTED_PER_PAGE : MAX_RESULTS_REQUESTED;
            extras.putInt(StatsService.ARG_MAX_RESULTS, maxElementsToRetrieve);
        }
        if (pageNumberRequested > 0) {
            extras.putInt(StatsService.ARG_PAGE_REQUESTED, pageNumberRequested);
        }
        extras.putIntArray(StatsService.ARG_SECTION, sectionsForTheService);
        StatsServiceStarter.startService(getActivity(), extras);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARGS_TIMEFRAME)) {
                mStatsTimeframe = (StatsTimeframe) savedInstanceState.getSerializable(ARGS_TIMEFRAME);
            }
            if (savedInstanceState.containsKey(ARGS_SELECTED_DATE)) {
                mDate = savedInstanceState.getString(ARGS_SELECTED_DATE);
            }
            restoreStatsData(savedInstanceState); // Each fragment will override this to restore fragment dependant data
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mResourceVars = new StatsResourceVars(activity);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
       /* AppLog.d(AppLog.T.STATS, this.getClass().getCanonicalName() + " > saving instance state");
        AppLog.d(AppLog.T.STATS, "mStatsTimeframe: " + mStatsTimeframe.getLabel());
        AppLog.d(AppLog.T.STATS, "mDate: " + mDate); */

        outState.putString(ARGS_SELECTED_DATE, mDate);
        outState.putSerializable(ARGS_TIMEFRAME, mStatsTimeframe);
        saveStatsData(outState); // Each fragment will override this
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Init the UI
        if (hasDataAvailable()) {
            updateUI();
        } else {
            showPlaceholderUI();
            refreshStats();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public boolean shouldUpdateFragmentOnUpdateEvent(StatsEvents.SectionUpdatedAbstract event) {
        if (!isAdded()) {
            return false;
        }

        if (!getDate().equals(event.mDate)) {
            return false;
        }

        if (!isSameBlog(event)) {
            return false;
        }

        if (!event.mTimeframe.equals(getTimeframe())) {
            return false;
        }

        return true;
    }

    boolean isSameBlog(StatsEvents.SectionUpdatedAbstract event) {
        SiteModel site = mSiteStore.getSiteByLocalId(getLocalTableBlogID());
        if (site != null) {
            return event.mRequestBlogId == site.getSiteId();
        }
        return false;
    }

    protected void showErrorUI(VolleyError error) {
        if (!isAdded()) {
            return;
        }

        String label = "<b>" + getString(R.string.error_refresh_stats) + "</b>";

        if (error instanceof NoConnectionError) {
            label += "<br/>" + getString(R.string.no_network_message);
        }

        if (StatsUtils.isRESTDisabledError(error)) {
            label += "<br/>" + getString(R.string.stats_enable_rest_api_in_jetpack);
        }

        showErrorUI(label);
    }

    protected void showErrorUI() {
        String label = "<b>" + getString(R.string.error_refresh_stats) + "</b>";
        showErrorUI(label);
    }

    public boolean shouldUpdateFragmentOnErrorEvent(StatsEvents.SectionUpdateError errorEvent) {
        if (!shouldUpdateFragmentOnUpdateEvent(errorEvent)) {
            return false;
        }

        StatsServiceLogic.StatsEndpointsEnum sectionToUpdate = errorEvent.mEndPointName;
        StatsServiceLogic.StatsEndpointsEnum[] sectionsToUpdate = sectionsToUpdate();

        for (int i = 0; i < sectionsToUpdate().length; i++) {
            if (sectionToUpdate == sectionsToUpdate[i]) {
                return true;
            }
        }

        return false;
    }

    public static StatsAbstractFragment newVisitorsAndViewsInstance(StatsViewType viewType, int localTableBlogID,
                  StatsTimeframe timeframe, String date, StatsVisitorsAndViewsFragment.OverviewLabel itemToSelect) {
        StatsVisitorsAndViewsFragment fragment =
                (StatsVisitorsAndViewsFragment) newInstance(viewType, localTableBlogID, timeframe, date);
        fragment.setSelectedOverviewItem(itemToSelect);
        return fragment;
    }

    public static StatsAbstractFragment newInstance(StatsViewType viewType, int localTableBlogID,
                                                    StatsTimeframe timeframe, String date) {
        StatsAbstractFragment fragment = null;

        switch (viewType) {
            // case TIMEFRAME_SELECTOR:
            // fragment = new StatsDateSelectorFragment();
            // break;
            case GRAPH_AND_SUMMARY:
                fragment = new StatsVisitorsAndViewsFragment();
                break;
            case TOP_POSTS_AND_PAGES:
                fragment = new StatsTopPostsAndPagesFragment();
                break;
            case REFERRERS:
                fragment = new StatsReferrersFragment();
                break;
            case CLICKS:
                fragment = new StatsClicksFragment();
                break;
            case GEOVIEWS:
                fragment = new StatsGeoviewsFragment();
                break;
            case AUTHORS:
                fragment = new StatsAuthorsFragment();
                break;
            case VIDEO_PLAYS:
                fragment = new StatsVideoplaysFragment();
                break;
            case COMMENTS:
                fragment = new StatsCommentsFragment();
                break;
            case TAGS_AND_CATEGORIES:
                fragment = new StatsTagsAndCategoriesFragment();
                break;
            case PUBLICIZE:
                fragment = new StatsPublicizeFragment();
                break;
            case FOLLOWERS:
                fragment = new StatsFollowersFragment();
                break;
            case SEARCH_TERMS:
                fragment = new StatsSearchTermsFragment();
                break;
            case INSIGHTS_MOST_POPULAR:
                fragment = new StatsInsightsMostPopularFragment();
                break;
            case INSIGHTS_ALL_TIME:
                fragment = new StatsInsightsAllTimeFragment();
                break;
            case INSIGHTS_TODAY:
                fragment = new StatsInsightsTodayFragment();
                break;
            case INSIGHTS_LATEST_POST_SUMMARY:
                fragment = new StatsInsightsLatestPostSummaryFragment();
                break;
        }

        fragment.setTimeframe(timeframe);
        fragment.setDate(date);

        Bundle args = new Bundle();
        args.putSerializable(ARGS_VIEW_TYPE, viewType);
        args.putInt(OldStatsActivity.ARG_LOCAL_TABLE_SITE_ID, localTableBlogID);
        fragment.setArguments(args);

        return fragment;
    }

    public void setDate(String newDate) {
        mDate = newDate;
    }

    String getDate() {
        return mDate;
    }

    public void setTimeframe(StatsTimeframe newTimeframe) {
        mStatsTimeframe = newTimeframe;
    }

    StatsTimeframe getTimeframe() {
        return mStatsTimeframe;
    }

    StatsViewType getViewType() {
        return (StatsViewType) getArguments().getSerializable(ARGS_VIEW_TYPE);
    }

    int getLocalTableBlogID() {
        return getArguments().getInt(OldStatsActivity.ARG_LOCAL_TABLE_SITE_ID);
    }

    boolean isSingleView() {
        return getArguments().getBoolean(ARGS_IS_SINGLE_VIEW, false);
    }

    protected abstract String getTitle();
}
