package org.wordpress.android.ui.posts;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SelectPageParentActivity extends ActionBarActivity {
    private ListView mListView;
    private ListScrollPositionManager mListScrollPositionManager;
    private int mSelectedPageId;

    private Blog mBlog;
    private ArrayList<PageNode> mPageLevels;
    private Map<Integer, Integer> mPageIds = new HashMap<>();

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
                mListView.setItemChecked(position, true);
                mSelectedPageId = mPageLevels.get(position).getPageId();
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int blogId = extras.getInt("blogId");
            mBlog = WordPress.wpDB.instantiateBlogByLocalId(blogId);
            if (mBlog == null) {
                Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
                finish();
            }

            mSelectedPageId = extras.getInt("parentId");
        }

        if (savedInstanceState == null) {
            mFirstRefresh = true;
        }

        populatePageList();
    }

    private void populatePageList() {
        PageNode pageTree = PageNode.createPageTreeFromDB(mBlog.getLocalTableBlogId());
        mPageLevels = PageNode.getSortedListOfPagesFromRoot(pageTree);

        // Add default "(no parent)" option to the top of the list
        mPageLevels.add(0, new PageNode(0, 0, getString(R.string.no_parent)));

        for (int i = 0; i < mPageLevels.size(); i++) {
            mPageIds.put(mPageLevels.get(i).getPageId(), i);
        }

        PageParentArrayAdapter pageAdapter = new PageParentArrayAdapter(this, R.layout.page_parents_row, mPageLevels);
        mListView.setAdapter(pageAdapter);

        if (mFirstRefresh) {
            mFirstRefresh = false;
            if (mPageIds.containsKey(mSelectedPageId)) {
                final int checkedPosition = mPageIds.get(mSelectedPageId);

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

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            saveAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
        super.onBackPressed();
    }

    private void saveAndFinish() {
        Intent mIntent = new Intent();

        // If nothing is checked, the parent id initially passed was not found. Don't modify the post data
        if (mListView.getCheckedItemPosition() >= 0) {
            String parentTitle =  "";
            if (mSelectedPageId != 0) {
                parentTitle = mPageLevels.get(mListView.getCheckedItemPosition()).getName();
            }
            Bundle bundle = new Bundle();
            bundle.putInt("parentId", mSelectedPageId);
            bundle.putString("parentTitle", parentTitle);

            mIntent.putExtras(bundle);
        }

        setResult(RESULT_OK, mIntent);
        finish();
    }
}
