package org.wordpress.android.ui.posts;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.HierarchyNode;
import org.wordpress.android.models.HierarchyNode.HierarchyType;
import org.wordpress.android.ui.posts.adapters.HierarchyListAdapter;
import org.wordpress.android.util.ListScrollPositionManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SelectPageParentActivity extends ActionBarActivity {
    public static final int PAGES_REQUEST_COUNT = 100;

    private ListView mListView;
    HierarchyListAdapter mListAdapter;
    private ListScrollPositionManager mListScrollPositionManager;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private int mSelectedParentId;

    private Blog mBlog;
    private int mPageId;
    private ArrayList<HierarchyNode> mPageLevels;
    private Map<Integer, Integer> mPageIds = new HashMap<>();

    private ApiHelper.FetchPageListTask mCurrentFetchPageListTask;

    private boolean mAutoScrollToCheckedPosition = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.hierarchy_activity);
        setTitle(getResources().getString(R.string.select_parent));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mListView = (ListView) findViewById(android.R.id.list);
        mListScrollPositionManager = new ListScrollPositionManager(mListView, true);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setItemsCanFocus(false);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                HierarchyNode selectedParent = mPageLevels.get(position);
                if (selectedParent.getId() == mPageId || selectedParent.isDescendantOfId(mPageId)) {
                    // Can't set the parent of a page to the page itself or its descendants
                    // Return to previously selected parent
                    mListView.setItemChecked(mPageIds.get(mSelectedParentId), true);
                } else {
                    mSelectedParentId = selectedParent.getId();
                }
            }
        });

        // Get extras from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int blogId = extras.getInt("blogId");

            String pageId = extras.getString("pageId");
            if (pageId.length() == 0) {
                // The page being edited is a local draft, and therefore doesn't have a remote id yet
                mPageId = -1;
            } else {
                mPageId = Integer.parseInt(pageId);
            }

            mBlog = WordPress.wpDB.instantiateBlogByLocalId(blogId);
            if (mBlog == null) {
                Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
                finish();
            }

            mSelectedParentId = extras.getInt("parentId");
        }

        // Swipe to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(this, (SwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        refreshPages();
                    }
                });

        if (savedInstanceState != null) {
            // Activity was recreated
            populatePageList(true);
            return;
        } else {
            populatePageList(false);
        }

        // Auto-refresh if it hasn't been done yet for this blog
        if (hasBeenUpdated()) {
            if (mPageIds.size() <= PostsListFragment.POSTS_REQUEST_COUNT) {
                // The number of pages is less than POSTS_REQUEST_COUNT, so there won't be any more to pull
                setUpdated();
            } else {
                if (NetworkUtils.isNetworkAvailable(this)) {
                    mSwipeToRefreshHelper.setRefreshing(true);
                    mAutoScrollToCheckedPosition = true;
                    refreshPages();
                    return;
                }
            }
        }

        // Adjust the ListView to focus on the position of the checked page parent
        mListView.post(new Runnable() {
            @Override
            public void run() {
                int checkedPosition = mListView.getCheckedItemPosition();
                if (mListView.getLastVisiblePosition() < checkedPosition) {
                    mListView.setSelectionFromTop(checkedPosition - 1, 0);
                }
            }
        });
    }

    private void populatePageList(boolean recreated) {
        int blogId = mBlog.getLocalTableBlogId();
        HierarchyNode pageTree = HierarchyNode.createTreeFromDB(blogId, HierarchyType.PAGE);
        mPageLevels = HierarchyNode.getSortedListFromRoot(pageTree);

        // Add default "(no parent)" option to the top of the list
        mPageLevels.add(0, new HierarchyNode(0, 0, getString(R.string.no_parent)));

        mPageIds.clear();
        for (int i = 0; i < mPageLevels.size(); i++) {
            mPageIds.put(mPageLevels.get(i).getId(), i);
        }

        // Re-assign current selected parent id in case it was changed in a recent refresh
        if (mPageIds.containsKey(mPageId)) {
            mSelectedParentId = mPageLevels.get(mPageIds.get(mPageId)).getParentId();
        }

        mListAdapter = new HierarchyListAdapter(this, R.layout.page_parents_row, mPageLevels);
        mListView.setAdapter(mListAdapter);

        if (mPageIds.containsKey(mSelectedParentId)) {
            final int checkedPosition = mPageIds.get(mSelectedParentId);

            mListView.setItemChecked(checkedPosition, true);

            if (recreated) {
                mListScrollPositionManager.restoreScrollOffset();
            } else {
                if (mAutoScrollToCheckedPosition) {
                    // Auto-scroll to current page's parent (highlighted)
                    mListView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mListView.getLastVisiblePosition() < checkedPosition) {
                                mListView.setSelectionFromTop(checkedPosition - 1, 0);
                            }
                        }
                    });
                }
            }
        }
    }

    public void fetchPosts() {
        List<Object> apiArgs = new Vector<>();
        apiArgs.add(WordPress.getCurrentBlog());
        apiArgs.add(PAGES_REQUEST_COUNT);
        apiArgs.add(0);

        mCurrentFetchPageListTask = new ApiHelper.FetchPageListTask(new ApiHelper.FetchPageListTask.Callback() {
            @Override
            public void onSuccess(int postCount) {
                setUpdated();
                mCurrentFetchPageListTask = null;
                mSwipeToRefreshHelper.setRefreshing(false);
                populatePageList(false);
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                mCurrentFetchPageListTask = null;
                mSwipeToRefreshHelper.setRefreshing(false);

                if (errorType != ApiHelper.ErrorType.TASK_CANCELLED && errorType != ApiHelper.ErrorType.NO_ERROR) {
                    switch (errorType) {
                        case UNAUTHORIZED:
                            ToastUtils.showToast(getBaseContext(), R.string.error_refresh_unauthorized_pages,
                                    Duration.LONG);
                            break;
                        default:
                            ToastUtils.showToast(getBaseContext(), R.string.error_refresh_pages, Duration.LONG);
                            break;
                    }
                }
            }
        });

        mCurrentFetchPageListTask.execute(apiArgs);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            saveAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshPages() {
        if (WordPress.wpDB.findLocalChanges(mBlog.getLocalTableBlogId(), true)) {
            // Don't update if there are local changes to pages
            mSwipeToRefreshHelper.setRefreshing(false);
            if (mAutoScrollToCheckedPosition) {
                mAutoScrollToCheckedPosition = false;
            } else {
                // If manually refreshed, let the user know why they're unable to refresh
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment.newAlertDialog(getString(R.string.local_changes),
                        getString(R.string.unable_to_refresh_local_changes));
                ft.add(alert, "alert");
                ft.commitAllowingStateLoss();
            }
        } else {
            mListScrollPositionManager.saveScrollOffset();
            fetchPosts();
        }
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
        super.onBackPressed();
    }

    private void saveAndFinish() {
        Intent mIntent = new Intent();

        // If nothing is checked, the parent id initially passed was not found. Don't modify the page data
        if (mListView.getCheckedItemPosition() >= 0) {
            String parentTitle = "";
            if (mSelectedParentId != 0) {
                parentTitle = mPageLevels.get(mListView.getCheckedItemPosition()).getName();
            }
            Bundle bundle = new Bundle();
            bundle.putInt("parentId", mSelectedParentId);
            bundle.putString("parentTitle", parentTitle);

            mIntent.putExtras(bundle);
        }

        setResult(RESULT_OK, mIntent);
        finish();
    }

    public boolean hasBeenUpdated() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        long timeStamp = preferences.getLong("last_updated_blog_" + mBlog.getLocalTableBlogId(), 0);
        return (timeStamp == 0);
    }

    public void setUpdated() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("last_updated_blog_" + mBlog.getLocalTableBlogId(), System.currentTimeMillis());
        editor.commit();
    }
}