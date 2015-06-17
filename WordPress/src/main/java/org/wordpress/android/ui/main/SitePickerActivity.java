package org.wordpress.android.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.ToastUtils;

import de.greenrobot.event.EventBus;

public class SitePickerActivity extends AppCompatActivity
        implements SitePickerAdapter.OnSiteClickListener,
        SitePickerAdapter.OnSelectedCountChangedListener {

    public static final String KEY_LOCAL_ID = "local_id";
    public static final String KEY_IS_IN_SEARCH_MODE = "is_in_search_mode";
    public static final String KEY_LAST_SEARCH = "last_search";

    private SitePickerAdapter mAdapter;
    private RecyclerView mRecycleView;
    private View mFabView;
    private ActionMode mActionMode;
    private SitePickerSearchView mSearchView;
    private int mCurrentLocalId;
    private boolean mDidUserSelectSite;
    private boolean mIsInSearchMode;
    private String mLastSearch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.site_picker_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mLastSearch = "";

        if (savedInstanceState != null) {
            mCurrentLocalId = savedInstanceState.getInt(KEY_LOCAL_ID);
            mIsInSearchMode = savedInstanceState.getBoolean(KEY_IS_IN_SEARCH_MODE);
            mLastSearch = savedInstanceState.getString(KEY_LAST_SEARCH);
        } else if (getIntent() != null) {
            mCurrentLocalId = getIntent().getIntExtra(KEY_LOCAL_ID, 0);
        }

        setupFab();

        mRecycleView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mRecycleView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mRecycleView.setItemAnimator(null);
        mRecycleView.setAdapter(getAdapter());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_LOCAL_ID, mCurrentLocalId);
        outState.putBoolean(KEY_IS_IN_SEARCH_MODE, mIsInSearchMode);
        outState.putString(KEY_LAST_SEARCH, mLastSearch);
        super.onSaveInstanceState(outState);
    }

    /*
     * if the user is signed into wp.com, show a fab menu which enables choosing between
     * adding a self-hosted site and creating a new wp.com one - if they're not signed in
     * we hide the menu and use a separate fab which directly adds a self-hosted site
     */
    private void setupFab() {
        final FloatingActionsMenu fabMenu = (FloatingActionsMenu) findViewById(R.id.fab_menu);
        if (AccountHelper.isSignedInWordPressDotCom()) {
            FloatingActionButton fabMenuItemCreateDotCom = (FloatingActionButton) findViewById(R.id.fab_item_create_dotcom);
            fabMenuItemCreateDotCom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityLauncher.newBlogForResult(SitePickerActivity.this);
                    fabMenu.collapse();
                }
            });

            FloatingActionButton fabMenuItemAddDotOrg = (FloatingActionButton) findViewById(R.id.fab_item_add_dotorg);
            fabMenuItemAddDotOrg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityLauncher.addSelfHostedSiteForResult(SitePickerActivity.this);
                    fabMenu.collapse();
                }
            });
            mFabView = fabMenu;
        } else {
            FloatingActionButton fabMenuAddDotOrg = (FloatingActionButton) findViewById(R.id.fab_add_dotorg);
            fabMenuAddDotOrg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityLauncher.addSelfHostedSiteForResult(SitePickerActivity.this);
                }
            });
            mFabView = fabMenuAddDotOrg;
        }

        // animate fab in after a delay which matches that of the activity transition
        long delayMs = getResources().getInteger(android.R.integer.config_shortAnimTime);
        AniUtils.showFabDelayed(mFabView, true, delayMs);
    }

    @Override
    public void finish() {
        super.finish();
        if (mDidUserSelectSite) {
            overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_left);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.site_picker, menu);
        setupSearchView(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // don't allow editing visibility unless there are multiple wp.com blogs and not in search mode
        int numSites = WordPress.wpDB.getNumDotComBlogs();
        MenuItem menuEdit = menu.findItem(R.id.menu_edit);
        if (mIsInSearchMode) {
            menuEdit.setVisible(false);
        } else {
            menuEdit.setVisible(numSites > 1);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.menu_edit) {
            mRecycleView.setItemAnimator(new DefaultItemAnimator());
            getAdapter().setEnableEditMode(true);
            startSupportActionMode(new ActionModeCallback());
            return true;
        } else if (itemId == R.id.menu_search) {
            mSearchView.requestFocus();
            mSearchView.showSoftKeyboard();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SignInActivity.CREATE_ACCOUNT_REQUEST:
            case RequestCodes.CREATE_BLOG:
                if (resultCode != RESULT_CANCELED) {
                    getAdapter().loadSites();
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    public String getLastSearch() {
        return mLastSearch;
    }

    public void setLastSearch(String lastSearch) {
        mLastSearch = lastSearch;
    }

    public boolean getIsInSearchMode() {
        return mIsInSearchMode;
    }

    public void setIsInSearchModeAndNullifyAdapter(boolean isInSearchMode) {
        mIsInSearchMode = isInSearchMode;
        mAdapter = null;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.BlogListChanged event) {
        if (!isFinishing()) {
            getAdapter().loadSites();
        }
    }

    protected SitePickerAdapter getAdapter() {
        if (mAdapter == null) {
            if (mIsInSearchMode) {
                mAdapter = new SitePickerSearchAdapter(this, mCurrentLocalId, mLastSearch);
            } else {
                mAdapter = new SitePickerAdapter(this, mCurrentLocalId);
            }
            mAdapter.setOnSiteClickListener(this);
            mAdapter.setOnSelectedCountChangedListener(this);
        }
        return mAdapter;
    }

    public RecyclerView getRecycleView() {
        return mRecycleView;
    }

    private void saveHiddenSites() {
        WordPress.wpDB.getDatabase().beginTransaction();
        try {
            // make all sites visible...
            WordPress.wpDB.setAllDotComBlogsVisibility(true);

            // ...then update ones marked hidden in the adapter, but don't hide the current site
            boolean skippedCurrentSite = false;
            String currentSiteName = null;
            SiteList hiddenSites = getAdapter().getHiddenSites();
            for (SiteRecord site : hiddenSites) {
                if (site.localId == mCurrentLocalId) {
                    skippedCurrentSite = true;
                    currentSiteName = site.getBlogNameOrHostName();
                } else {
                    WordPress.wpDB.setDotComBlogsVisibility(site.localId, false);
                    StatsTable.deleteStatsForBlog(this, site.localId); // Remove stats data for hidden sites
                }
            }

            // let user know the current site wasn't hidden
            if (skippedCurrentSite) {
                ToastUtils.showToast(this,
                        getString(R.string.site_picker_cant_hide_current_site, currentSiteName),
                        ToastUtils.Duration.LONG);
            }

            WordPress.wpDB.getDatabase().setTransactionSuccessful();
        } finally {
            WordPress.wpDB.getDatabase().endTransaction();
        }
    }

    private void setupSearchView(Menu menu) {
        MenuItem menuSearch = menu.findItem(R.id.menu_search);
        mSearchView = (SitePickerSearchView) menuSearch.getActionView();
        mSearchView.configure(this, menu);
    }

    private void updateActionModeTitle() {
        if (mActionMode != null) {
            int numSelected = getAdapter().getNumSelected();
            mActionMode.setTitle(getString(R.string.cab_selected, numSelected));
        }
    }

    @Override
    public void onSelectedCountChanged(int numSelected) {
        if (mActionMode != null) {
            updateActionModeTitle();
            mActionMode.invalidate();
        }
    }

    @Override
    public void onSiteClick(SiteRecord site) {
        if (mActionMode == null) {
            mSearchView.hideSoftKeyboard();
            AniUtils.showFab(mFabView, false);
            WordPress.setCurrentBlogAndSetVisible(site.localId);
            WordPress.wpDB.updateLastBlogId(site.localId);
            setResult(RESULT_OK);
            mDidUserSelectSite = true;
            finish();
        }
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        private boolean mHasChanges;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
            mHasChanges = false;
            updateActionModeTitle();
            AniUtils.showFab(mFabView, false);
            actionMode.getMenuInflater().inflate(R.menu.site_picker_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            MenuItem mnuShow = menu.findItem(R.id.menu_show);
            mnuShow.setEnabled(getAdapter().getNumHiddenSelected() > 0);

            MenuItem mnuHide = menu.findItem(R.id.menu_hide);
            mnuHide.setEnabled(getAdapter().getNumVisibleSelected() > 0);

            MenuItem mnuSelectAll = menu.findItem(R.id.menu_select_all);
            mnuSelectAll.setEnabled(getAdapter().getNumSelected() != getAdapter().getItemCount());

            MenuItem mnuDeselectAll = menu.findItem(R.id.menu_deselect_all);
            mnuDeselectAll.setEnabled(getAdapter().getNumSelected() > 0);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.menu_show) {
                getAdapter().setVisibilityForSelectedSites(true);
                mHasChanges = true;
                mActionMode.finish();
            } else if (itemId == R.id.menu_hide) {
                getAdapter().setVisibilityForSelectedSites(false);
                mHasChanges = true;
                mActionMode.finish();
            } else if (itemId == R.id.menu_select_all) {
                getAdapter().selectAll();
            } else if (itemId == R.id.menu_deselect_all) {
                getAdapter().deselectAll();
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            if (mHasChanges) {
                saveHiddenSites();
            }
            getAdapter().setEnableEditMode(false);
            AniUtils.showFab(mFabView, true);
            mActionMode = null;
        }
    }
}
