package org.wordpress.android.ui.posts;

import android.content.Intent;
import android.os.Bundle;
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
import org.wordpress.android.models.PageNode;
import org.wordpress.android.util.ListScrollPositionManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper.RefreshListener;
import org.xmlrpc.android.ApiHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SelectPageParentActivity extends ActionBarActivity {
    public static final int PAGES_REQUEST_COUNT = 100;

    private ListView mListView;
    private ListScrollPositionManager mListScrollPositionManager;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private int mSelectedParentId;

    private Blog mBlog;
    private int mPageId;
    private ArrayList<PageNode> mPageLevels;
    private Map<Integer, Integer> mPageIds = new HashMap<>();

    private ApiHelper.FetchPageListTask mCurrentFetchPageListTask;

    private boolean mFirstRefresh = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_categories);
        setTitle(getResources().getString(R.string.select_parent));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mListView = (ListView)findViewById(android.R.id.list);
        mListScrollPositionManager = new ListScrollPositionManager(mListView, true);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setItemsCanFocus(false);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                PageNode selectedParent = mPageLevels.get(position);
                if (selectedParent.getPageId() == mPageId || selectedParent.isDescendantOfPageWithId(mPageId)) {
                    // Can't set the parent of a page to the page itself or its descendants
                    // Return to previously selected parent
                    mListView.setItemChecked(mPageIds.get(mSelectedParentId), true);
                } else {
                    mSelectedParentId = selectedParent.getPageId();
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int blogId = extras.getInt("blogId");
            mPageId = extras.getInt("pageId");
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

        if (savedInstanceState == null) {
            mFirstRefresh = true;
        }

        populatePageList();

        // Refresh blog list if network is available and activity really starts
        if (NetworkUtils.isNetworkAvailable(this) && savedInstanceState == null) {
            mSwipeToRefreshHelper.setRefreshing(true);
            refreshPages();
        }
    }

    private void populatePageList() {
        int blogId = mBlog.getLocalTableBlogId();
        PageNode pageTree;

        if (WordPress.wpDB.getPageListSize(blogId) == 0) {
            // If we haven't fetched a full list of pages for this blogId yet, use the posts table to populate the list
            pageTree = PageNode.createPageTreeFromDB(blogId, true);
        } else {
            pageTree = PageNode.createPageTreeFromDB(blogId, false);
        }
        mPageLevels = PageNode.getSortedListOfPagesFromRoot(pageTree);

        // Add default "(no parent)" option to the top of the list
        mPageLevels.add(0, new PageNode(0, 0, getString(R.string.no_parent)));

        mPageIds.clear();
        for (int i = 0; i < mPageLevels.size(); i++) {
            mPageIds.put(mPageLevels.get(i).getPageId(), i);
        }

        PageParentArrayAdapter pageAdapter = new PageParentArrayAdapter(this, R.layout.page_parents_row, mPageLevels);
        mListView.setAdapter(pageAdapter);

        if (mFirstRefresh) {
            mFirstRefresh = false;
            if (mPageIds.containsKey(mSelectedParentId)) {
                final int checkedPosition = mPageIds.get(mSelectedParentId);

                mListView.setItemChecked(checkedPosition, true);

                // Auto-scroll to initial selected position
                mListView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mListView.getLastVisiblePosition() < checkedPosition) {
                            mListView.setSelectionFromTop(checkedPosition - 1, 0);
                        }
                    }
                });
            }
        } else {
            mListScrollPositionManager.restoreScrollOffset();
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
                mCurrentFetchPageListTask = null;
                mSwipeToRefreshHelper.setRefreshing(false);
                populatePageList();
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
        mListScrollPositionManager.saveScrollOffset();
        fetchPosts();
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
            String parentTitle =  "";
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
}
