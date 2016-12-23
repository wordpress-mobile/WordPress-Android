package org.wordpress.android.ui.posts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.LongSparseArray;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;
import org.wordpress.android.models.CategoryNode;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.helpers.ListScrollPositionManager;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

public class SelectCategoriesActivity extends AppCompatActivity {
    private static final int ACTIVITY_REQUEST_CODE_ADD_CATEGORY = 0;

    public static final String KEY_POST = "KEY_POST";

    private ListView mListView;
    private TextView mEmptyView;
    private ListScrollPositionManager mListScrollPositionManager;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private HashSet<Long> mSelectedCategories;
    private ArrayList<CategoryNode> mCategoryLevels;
    private LongSparseArray<Integer> mCategoryRemoteIdsToListPositions = new LongSparseArray<>();
    private SiteModel mSite;

    @Inject SiteStore mSiteStore;
    @Inject TaxonomyStore mTaxonomyStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }
        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.select_categories);
        setTitle(getResources().getString(R.string.select_categories));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mListView = (ListView)findViewById(android.R.id.list);
        mListScrollPositionManager = new ListScrollPositionManager(mListView, false);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setItemsCanFocus(false);

        mEmptyView = (TextView) findViewById(R.id.empty_view);
        mListView.setEmptyView(mEmptyView);

        mSelectedCategories = new HashSet<>();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(KEY_POST)) {
                PostModel post = (PostModel) extras.getSerializable(KEY_POST);
                if (post != null) {
                    for (Long categoryId : post.getCategoryIdList()) {
                        mSelectedCategories.add(categoryId);
                    }
                }
            }
        }

        // swipe to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(this, (CustomSwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        refreshCategories();
                    }
                });

        populateCategoryList();

        if (NetworkUtils.isNetworkAvailable(this)) {
            mEmptyView.setText(R.string.empty_list_default);
            if (isCategoryListEmpty()) {
                refreshCategories();
            }
        } else {
            mEmptyView.setText(R.string.no_network_title);
        }
    }

    @Override
    protected void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    private boolean isCategoryListEmpty() {
        if (mListView.getAdapter() != null) {
            return mListView.getAdapter().isEmpty();
        } else {
            return true;
        }
    }

    private void populateCategoryList() {
        CategoryNode categoryTree = CategoryNode.createCategoryTreeFromList(mTaxonomyStore.getCategoriesForSite(mSite));
        mCategoryLevels = CategoryNode.getSortedListOfCategoriesFromRoot(categoryTree);
        for (int i = 0; i < mCategoryLevels.size(); i++) {
            mCategoryRemoteIdsToListPositions.put(mCategoryLevels.get(i).getCategoryId(), i);
        }

        CategoryArrayAdapter categoryAdapter = new CategoryArrayAdapter(this, R.layout.categories_row, mCategoryLevels);
        mListView.setAdapter(categoryAdapter);
        if (mSelectedCategories != null) {
            for (Long selectedCategory : mSelectedCategories) {
                if (mCategoryRemoteIdsToListPositions.get(selectedCategory) != null) {
                    mListView.setItemChecked(mCategoryRemoteIdsToListPositions.get(selectedCategory), true);
                }
            }
        }
        mListScrollPositionManager.restoreScrollOffset();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            final Bundle extras = data.getExtras();

            switch (requestCode) {
                case ACTIVITY_REQUEST_CODE_ADD_CATEGORY:
                    if (resultCode == RESULT_OK) {
                        TermModel newCategory = (TermModel) extras.getSerializable(AddCategoryActivity.KEY_CATEGORY);

                        mSwipeToRefreshHelper.setRefreshing(true);
                        RemoteTermPayload payload = new RemoteTermPayload(newCategory, mSite);
                        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(payload));
                        break;
                    }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.categories, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_new_category) {
            if (NetworkUtils.checkConnection(this)) {
                Intent intent = new Intent(SelectCategoriesActivity.this, AddCategoryActivity.class);
                intent.putExtra(WordPress.SITE, mSite);
                startActivityForResult(intent, 0);
            }
            return true;
        } else if (itemId == android.R.id.home) {
            saveAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshCategories() {
        mSwipeToRefreshHelper.setRefreshing(true);
        mListScrollPositionManager.saveScrollOffset();
        updateSelectedCategoryList();
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(mSite));
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
        super.onBackPressed();
    }

    private void updateSelectedCategoryList() {
        SparseBooleanArray selectedItems = mListView.getCheckedItemPositions();
        for (int i = 0; i < selectedItems.size(); i++) {
            long categoryRemoteId = mCategoryLevels.get(selectedItems.keyAt(i)).getCategoryId();
            if (selectedItems.get(selectedItems.keyAt(i))) {
                mSelectedCategories.add(categoryRemoteId);
            } else {
                mSelectedCategories.remove(categoryRemoteId);
            }
        }
    }

    private void saveAndFinish() {
        Bundle bundle = new Bundle();
        updateSelectedCategoryList();
        List<TermModel> categories = new ArrayList<>();
        for (Long categoryRemoteId : mSelectedCategories) {
            categories.add(mTaxonomyStore.getCategoryByRemoteId(mSite, categoryRemoteId));
        }
        bundle.putSerializable("selectedCategories", new ArrayList<>(categories));
        Intent mIntent = new Intent();
        mIntent.putExtras(bundle);
        setResult(RESULT_OK, mIntent);
        finish();
    }

    private int getCheckedItemCount(ListView listView) {
        return listView.getCheckedItemCount();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(OnTaxonomyChanged event) {
        switch (event.causeOfChange) {
            case FETCH_CATEGORIES:
                mSwipeToRefreshHelper.setRefreshing(false);

                if (event.isError()) {
                    if (!isFinishing()) {
                        ToastUtils.showToast(SelectCategoriesActivity.this, R.string.category_refresh_error, Duration.LONG);
                    }
                } else {
                    populateCategoryList();
                }
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTermUploaded(OnTermUploaded event) {
        mSwipeToRefreshHelper.setRefreshing(false);

        if (event.isError()) {
            if (!isFinishing()) {
                ToastUtils.showToast(SelectCategoriesActivity.this, R.string.adding_cat_failed, Duration.LONG);
            }
        } else {
            populateCategoryList();
            if (!isFinishing()) {
                ToastUtils.showToast(SelectCategoriesActivity.this, R.string.adding_cat_success, Duration.SHORT);
            }
        }
    }
}
