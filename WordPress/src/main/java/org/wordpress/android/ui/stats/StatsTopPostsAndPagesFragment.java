package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.adapters.PostsAndPagesAdapter;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.stats.models.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.service.StatsService;

import java.util.ArrayList;
import java.util.List;


public class StatsTopPostsAndPagesFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsTopPostsAndPagesFragment.class.getSimpleName();

    private TopPostsAndPagesModel mTopPostsAndPagesModel = null;

    @Override
    protected boolean hasDataAvailable() {
        return mTopPostsAndPagesModel != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mTopPostsAndPagesModel);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mTopPostsAndPagesModel = (TopPostsAndPagesModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.TopPostsUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mGroupIdToExpandedMap.clear();
        mTopPostsAndPagesModel = event.mTopPostsAndPagesModel;

        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mTopPostsAndPagesModel = null;
        mGroupIdToExpandedMap.clear();
        showErrorUI(event.mError);
    }

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (hasTopPostsAndPages()) {
            List<PostModel> postViews = mTopPostsAndPagesModel.getTopPostsAndPages();
            ArrayAdapter adapter = new PostsAndPagesAdapter(getActivity(), postViews);
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasTopPostsAndPages() {
        return mTopPostsAndPagesModel != null && mTopPostsAndPagesModel.hasTopPostsAndPages();
    }

    private List<PostModel> getTopPostsAndPages() {
        if (!hasTopPostsAndPages()) {
            return new ArrayList<PostModel>(0);
        }
        return mTopPostsAndPagesModel.getTopPostsAndPages();
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return hasTopPostsAndPages() && getTopPostsAndPages().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST;
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_posts_and_pages;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_top_posts_title;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_top_posts_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.TOP_POSTS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_top_posts_and_pages);
    }
}
