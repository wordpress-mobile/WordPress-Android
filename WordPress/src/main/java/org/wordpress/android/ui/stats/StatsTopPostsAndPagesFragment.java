package org.wordpress.android.ui.stats;

import android.widget.ArrayAdapter;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.adapters.PostsAndPagesAdapter;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.stats.models.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.service.StatsService;

import java.util.List;


public class StatsTopPostsAndPagesFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsTopPostsAndPagesFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (isErrorResponse()) {
            showErrorUI();
            return;
        }

        if (hasTopPostsAndPages()) {
            List<PostModel> postViews = ((TopPostsAndPagesModel) mDatamodels[0]).getTopPostsAndPages();
            ArrayAdapter adapter = new PostsAndPagesAdapter(getActivity(), getLocalTableBlogID(), postViews);
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasTopPostsAndPages() {
        return !isDataEmpty() && ((TopPostsAndPagesModel) mDatamodels[0]).hasTopPostsAndPages();
    }

    private List<PostModel> getTopPostsAndPages() {
        if (!hasTopPostsAndPages()) {
            return null;
        }
        return ((TopPostsAndPagesModel) mDatamodels[0]).getTopPostsAndPages();
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
    protected StatsService.StatsEndpointsEnum[] getSectionToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.TOP_POSTS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_top_posts_and_pages);
    }
}
