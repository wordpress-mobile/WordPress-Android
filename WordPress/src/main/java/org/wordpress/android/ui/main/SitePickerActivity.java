package org.wordpress.android.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;

import java.util.List;

import javax.inject.Inject;

public class SitePickerActivity extends AppCompatActivity
        implements SitePickerAdapter.OnSiteClickListener,
        SitePickerAdapter.OnSelectedCountChangedListener,
        SearchView.OnQueryTextListener {

    public static final String KEY_LOCAL_ID = "local_id";
    private static final String KEY_IS_IN_SEARCH_MODE = "is_in_search_mode";
    private static final String KEY_LAST_SEARCH = "last_search";

    private SitePickerAdapter mAdapter;
    private RecyclerView mRecycleView;
    private ActionMode mActionMode;
    private MenuItem mMenuEdit;
    private MenuItem mMenuAdd;
    private MenuItem mMenuSearch;
    private SearchView mSearchView;
    private int mCurrentLocalId;
    private boolean mDidUserSelectSite;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.site_picker_activity);
        restoreSavedInstanceState(savedInstanceState);
        setupActionBar();
        setupRecycleView();
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.SITE_PICKER);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_LOCAL_ID, mCurrentLocalId);
        outState.putBoolean(KEY_IS_IN_SEARCH_MODE, getAdapter().getIsInSearchMode());
        outState.putString(KEY_LAST_SEARCH, getAdapter().getLastSearch());
        super.onSaveInstanceState(outState);
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
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        mMenuEdit = menu.findItem(R.id.menu_edit);
        mMenuAdd = menu.findItem(R.id.menu_add);
        mMenuSearch = menu.findItem(R.id.menu_search);

        updateMenuItemVisibility();
        setupSearchView();

        return true;
    }

    private void updateMenuItemVisibility() {
        if (mMenuAdd == null || mMenuEdit == null || mMenuSearch == null) return;

        if (getAdapter().getIsInSearchMode()) {
            mMenuEdit.setVisible(false);
            mMenuAdd.setVisible(false);
        } else {
            // don't allow editing visibility unless there are multiple wp.com and jetpack sites
            mMenuEdit.setVisible(mSiteStore.getVisibleWPComAndJetpackSitesCount() > 1);
            mMenuAdd.setVisible(true);
        }

        // no point showing search if there aren't multiple blogs
        mMenuSearch.setVisible(mSiteStore.getSitesCount() > 1);
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
            showSoftKeyboard();
            return true;
        } else if (itemId == R.id.menu_add) {
            addSite(this, mAccountStore.hasAccessToken());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.ADD_ACCOUNT:
            case RequestCodes.CREATE_BLOG:
                if (resultCode != RESULT_CANCELED) {
                    getAdapter().loadSites();
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (!isFinishing()) {
            getAdapter().loadSites();
        }
    }

    private void setupRecycleView() {
        mRecycleView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mRecycleView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mRecycleView.setItemAnimator(null);
        mRecycleView.setAdapter(getAdapter());
    }

    private void restoreSavedInstanceState(Bundle savedInstanceState) {
        boolean isInSearchMode = false;
        String lastSearch = "";

        if (savedInstanceState != null) {
            mCurrentLocalId = savedInstanceState.getInt(KEY_LOCAL_ID);
            isInSearchMode = savedInstanceState.getBoolean(KEY_IS_IN_SEARCH_MODE);
            lastSearch = savedInstanceState.getString(KEY_LAST_SEARCH);
        } else if (getIntent() != null) {
            mCurrentLocalId = getIntent().getIntExtra(KEY_LOCAL_ID, 0);
        }

        setNewAdapter(lastSearch, isInSearchMode);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.site_picker_title);
        }
    }

    private void setIsInSearchModeAndSetNewAdapter(boolean isInSearchMode) {
        String lastSearch = getAdapter().getLastSearch();
        setNewAdapter(lastSearch, isInSearchMode);
    }

    private SitePickerAdapter getAdapter() {
        if (mAdapter == null) {
            setNewAdapter("", false);
        }
        return mAdapter;
    }

    private void setNewAdapter(String lastSearch, boolean isInSearchMode) {
        mAdapter = new SitePickerAdapter(
                this,
                mCurrentLocalId,
                lastSearch,
                isInSearchMode,
                new SitePickerAdapter.OnDataLoadedListener() {
            @Override
            public void onBeforeLoad(boolean isEmpty) {
                if (isEmpty) {
                    showProgress(true);
                }
            }
            @Override
            public void onAfterLoad() {
                showProgress(false);
            }
        });
        mAdapter.setOnSiteClickListener(this);
        mAdapter.setOnSelectedCountChangedListener(this);
    }

    private void saveHiddenSites() {
        // TODO: FluxC: This is inefficient
        // Mark all sites visible
        List<SiteModel> sites = mSiteStore.getWPComAndJetpackSites();
        for (SiteModel site : sites) {
            site.setIsVisible(true);
            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site));
        }

        // Update sites marked hidden in the adapter, but don't hide the current site
        boolean skippedCurrentSite = false;
        String currentSiteName = null;
        SiteList hiddenSites = getAdapter().getHiddenSites();
        for (SiteRecord site : hiddenSites) {
            if (site.localId == mCurrentLocalId) {
                skippedCurrentSite = true;
                currentSiteName = site.getBlogNameOrHomeURL();
            } else {
                SiteModel siteModel = mSiteStore.getSiteByLocalId(site.localId);
                siteModel.setIsVisible(false);
                // Save the site
                mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(siteModel));
                // Remove stats data for hidden sites
                StatsTable.deleteStatsForBlog(this, site.localId);
            }
        }

        // let user know the current site wasn't hidden
        if (skippedCurrentSite) {
            String cantHideCurrentSite = getString(R.string.site_picker_cant_hide_current_site);
            ToastUtils.showToast(this,
                    String.format(cantHideCurrentSite, currentSiteName),
                    ToastUtils.Duration.LONG);
        }
    }

    private void updateActionModeTitle() {
        if (mActionMode != null) {
            int numSelected = getAdapter().getNumSelected();
            String cabSelected = getString(R.string.cab_selected);
            mActionMode.setTitle(String.format(cabSelected, numSelected));
        }
    }

    private void setupSearchView() {
        mSearchView = (SearchView) mMenuSearch.getActionView();
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(this);

        MenuItemCompat.setOnActionExpandListener(mMenuSearch, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                enableSearchMode();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                disableSearchMode();
                return true;
            }
        });

        setQueryIfInSearch();
    }

    private void setQueryIfInSearch() {
        if (getAdapter().getIsInSearchMode()) {
            mMenuSearch.expandActionView();
            mSearchView.setQuery(getAdapter().getLastSearch(), false);
        }
    }

    private void enableSearchMode() {
        setIsInSearchModeAndSetNewAdapter(true);
        mRecycleView.swapAdapter(getAdapter(), true);
        updateMenuItemVisibility();
    }

    private void disableSearchMode() {
        hideSoftKeyboard();
        setIsInSearchModeAndSetNewAdapter(false);
        mRecycleView.swapAdapter(getAdapter(), true);
        updateMenuItemVisibility();
    }

    private void hideSoftKeyboard() {
        if (!hasHardwareKeyboard()) {
            WPActivityUtils.hideKeyboard(mSearchView);
        }
    }

    private void showSoftKeyboard() {
        if (!hasHardwareKeyboard()) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private boolean hasHardwareKeyboard() {
        return (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS);
    }

    @Override
    public void onSelectedCountChanged(int numSelected) {
        if (mActionMode != null) {
            updateActionModeTitle();
            mActionMode.invalidate();
        }
    }

    @Override
    public void onSiteClick(SiteRecord siteRecord) {
        if (mActionMode == null) {
            hideSoftKeyboard();
            setResult(RESULT_OK, new Intent().putExtra(KEY_LOCAL_ID, siteRecord.localId));
            mDidUserSelectSite = true;
            finish();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        hideSoftKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        getAdapter().setLastSearch(s);
        getAdapter().searchSites(s);
        return true;
    }

    public void showProgress(boolean show) {
        findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        private boolean mHasChanges;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
            mHasChanges = false;
            updateActionModeTitle();
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
            mActionMode = null;
        }
    }

    public static void addSite(Activity activity, boolean isSignedInWpCom) {
        // if user is signed into wp.com use the dialog to enable choosing whether to
        // create a new wp.com blog or add a self-hosted one
        if (isSignedInWpCom) {
            DialogFragment dialog = new AddSiteDialog();
            dialog.show(activity.getFragmentManager(), AddSiteDialog.ADD_SITE_DIALOG_TAG);
        } else {
            // user isn't signed into wp.com, so simply enable adding self-hosted
            ActivityLauncher.addSelfHostedSiteForResult(activity);
        }
    }

    /*
     * dialog which appears after user taps "Add site" - enables choosing whether to create
     * a new wp.com blog or add an existing self-hosted one
     */
    public static class AddSiteDialog extends DialogFragment {
        static final String ADD_SITE_DIALOG_TAG = "add_site_dialog";

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            CharSequence[] items =
                    {getString(R.string.site_picker_create_dotcom),
                            getString(R.string.site_picker_add_self_hosted)};
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.site_picker_add_site);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        ActivityLauncher.newBlogForResult(getActivity());
                    } else {
                        ActivityLauncher.addSelfHostedSiteForResult(getActivity());
                    }
                }
            });
            return builder.create();
        }
    }
}
