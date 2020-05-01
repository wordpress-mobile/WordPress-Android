package org.wordpress.android.ui.main;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.StatsStore;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.EmptyViewRecyclerView;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.Debouncer;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.viewmodel.main.SitePickerViewModel;
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.ContinueReblogTo;
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.NavigateToState;
import org.wordpress.android.widgets.WPDialogSnackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

public class SitePickerActivity extends LocaleAwareActivity
        implements SitePickerAdapter.OnSiteClickListener,
        SitePickerAdapter.OnSelectedCountChangedListener,
        SearchView.OnQueryTextListener {
    public static final String KEY_LOCAL_ID = "local_id";
    public static final String KEY_SITE_CREATED_BUT_NOT_FETCHED = "key_site_created_but_not_fetched";

    public static final String KEY_SITE_PICKER_MODE = "key_site_picker_mode";

    private static final String KEY_IS_IN_SEARCH_MODE = "is_in_search_mode";
    private static final String KEY_LAST_SEARCH = "last_search";
    private static final String KEY_REFRESHING = "refreshing_sites";

    private SitePickerAdapter mAdapter;
    private EmptyViewRecyclerView mRecycleView;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private ActionMode mActionMode;
    private ActionMode mReblogActionMode;
    private MenuItem mMenuEdit;
    private MenuItem mMenuAdd;
    private MenuItem mMenuSearch;
    private SearchView mSearchView;
    private int mCurrentLocalId;
    private SitePickerMode mSitePickerMode;
    private Debouncer mDebouncer = new Debouncer();
    private SitePickerViewModel mViewModel;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;
    @Inject StatsStore mStatsStore;
    @Inject ViewModelProvider.Factory mViewModelFactory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(SitePickerViewModel.class);

        setContentView(R.layout.site_picker_activity);
        restoreSavedInstanceState(savedInstanceState);
        setupActionBar();
        setupRecycleView();

        initSwipeToRefreshHelper(findViewById(android.R.id.content));
        if (savedInstanceState != null) {
            mSwipeToRefreshHelper.setRefreshing(savedInstanceState.getBoolean(KEY_REFRESHING, false));
        }

        if (mSitePickerMode.isReblogMode()) {
            mViewModel.getOnActionTriggered().observe(
                    this,
                    unitEvent -> unitEvent.applyIfNotHandled(action -> {
                switch (action.getActionType()) {
                    case NAVIGATE_TO_STATE:
                        switch (((NavigateToState) action).getNavigateState()) {
                            case TO_SITE_SELECTED:
                                mSitePickerMode = SitePickerMode.REBLOG_CONTINUE_MODE;
                                if (getAdapter().getIsInSearchMode()) {
                                    disableSearchMode();
                                }

                                if (mReblogActionMode == null) {
                                    startSupportActionMode(new ReblogActionModeCallback());
                                }

                                SiteRecord site = ((NavigateToState) action).getSiteForReblog();
                                if (site != null) {
                                    mReblogActionMode.setTitle(site.getBlogNameOrHomeURL());
                                }
                                break;
                            case TO_NO_SITE_SELECTED:
                                mSitePickerMode = SitePickerMode.REBLOG_SELECT_MODE;
                                getAdapter().clearReblogSelection();
                                break;
                        }
                        break;
                    case CONTINUE_REBLOG_TO:
                        SiteRecord siteToReblog = ((ContinueReblogTo) action).getSiteForReblog();
                        selectSiteAndFinish(siteToReblog);
                        break;
                    case ASK_FOR_SITE_SELECTION:
                        if (BuildConfig.DEBUG) {
                            throw new IllegalStateException(
                                    "SitePickerActivity > Selected site was null while attempting to reblog"
                            );
                        } else {
                            AppLog.e(
                                    AppLog.T.READER,
                                    "SitePickerActivity > Selected site was null while attempting to reblog"
                            );
                            ToastUtils.showToast(this, R.string.site_picker_ask_site_select);
                        }
                        break;
                }
                return null;
            }));
        }
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
        outState.putBoolean(KEY_REFRESHING, mSwipeToRefreshHelper.isRefreshing());
        outState.putSerializable(KEY_SITE_PICKER_MODE, mSitePickerMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.site_picker, menu);
        mMenuSearch = menu.findItem(R.id.menu_search);
        mMenuEdit = menu.findItem(R.id.menu_edit);
        mMenuAdd = menu.findItem(R.id.menu_add);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateMenuItemVisibility();
        setupSearchView();
        return true;
    }

    private void updateMenuItemVisibility() {
        if (mMenuAdd == null || mMenuEdit == null || mMenuSearch == null) {
            return;
        }

        if (getAdapter().getIsInSearchMode() || mSitePickerMode.isReblogMode()) {
            mMenuEdit.setVisible(false);
            mMenuAdd.setVisible(false);
        } else {
            // don't allow editing visibility unless there are multiple wp.com and jetpack sites
            mMenuEdit.setVisible(mSiteStore.getSitesAccessedViaWPComRestCount() > 1);
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
            startEditingVisibility();
            return true;
        } else if (itemId == R.id.menu_add) {
            addSite(this, mAccountStore.hasAccessToken());
            return true;
        } else if (itemId == R.id.continue_flow) {
            mViewModel.onContinueFlowSelected();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.ADD_ACCOUNT:
            case RequestCodes.CREATE_SITE:
                if (resultCode == RESULT_OK) {
                    debounceLoadSites();
                    if (data == null) {
                        data = new Intent();
                    }
                    if (data.getBooleanExtra(KEY_SITE_CREATED_BUT_NOT_FETCHED, false)) {
                        showSiteCreatedButNotFetchedSnackbar();
                    } else {
                        data.putExtra(WPMainActivity.ARG_CREATE_SITE, RequestCodes.CREATE_SITE);
                        setResult(resultCode, data);
                        finish();
                    }
                }
                break;
        }

        // Enable the block editor on sites created on mobile
        switch (requestCode) {
            case RequestCodes.CREATE_SITE:
                if (data != null) {
                    int newSiteLocalID = data.getIntExtra(SitePickerActivity.KEY_LOCAL_ID, -1);
                    SiteUtils.enableBlockEditorOnSiteCreation(mDispatcher, mSiteStore, newSiteLocalID);
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        mDispatcher.unregister(this);
        mDebouncer.shutdown();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteRemoved(OnSiteRemoved event) {
        if (!event.isError()) {
            debounceLoadSites();
        } else {
            // shouldn't happen
            AppLog.e(AppLog.T.DB, "Encountered unexpected error while attempting to remove site: " + event.error);
            ToastUtils.showToast(this, R.string.site_picker_remove_site_error);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (mSwipeToRefreshHelper.isRefreshing()) {
            mSwipeToRefreshHelper.setRefreshing(false);
        }
        debounceLoadSites();
    }

    private void debounceLoadSites() {
        mDebouncer.debounce(Void.class, () -> {
            if (!isFinishing()) {
                getAdapter().loadSites();
            }
        }, 200, TimeUnit.MILLISECONDS);
    }

    private void initSwipeToRefreshHelper(View view) {
        if (view == null) {
            return;
        }
        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                view.findViewById(R.id.ptr_layout),
                () -> {
                    if (isFinishing()) {
                        return;
                    }
                    if (!NetworkUtils.checkConnection(SitePickerActivity.this) || !mAccountStore.hasAccessToken()) {
                        mSwipeToRefreshHelper.setRefreshing(false);
                        return;
                    }
                    mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
                }
        );
    }

    private void setupRecycleView() {
        mRecycleView = findViewById(R.id.recycler_view);
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mRecycleView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

        mRecycleView.setItemAnimator(mSitePickerMode.isReblogMode() ? new DefaultItemAnimator() : null);

        mRecycleView.setAdapter(getAdapter());

        ActionableEmptyView actionableEmptyView = findViewById(R.id.actionable_empty_view);
        actionableEmptyView.updateLayoutForSearch(true, 0);
        mRecycleView.setEmptyView(actionableEmptyView);
    }

    private void restoreSavedInstanceState(Bundle savedInstanceState) {
        boolean isInSearchMode = false;
        String lastSearch = "";

        if (savedInstanceState != null) {
            mCurrentLocalId = savedInstanceState.getInt(KEY_LOCAL_ID);
            isInSearchMode = savedInstanceState.getBoolean(KEY_IS_IN_SEARCH_MODE);
            lastSearch = savedInstanceState.getString(KEY_LAST_SEARCH);
            mSitePickerMode = (SitePickerMode) savedInstanceState.getSerializable(KEY_SITE_PICKER_MODE);
        } else if (getIntent() != null) {
            mCurrentLocalId = getIntent().getIntExtra(KEY_LOCAL_ID, 0);
            mSitePickerMode = (SitePickerMode) getIntent().getSerializableExtra(KEY_SITE_PICKER_MODE);
        }

        setNewAdapter(lastSearch, isInSearchMode);
    }

    private void setupActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_cross_white_24dp);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.site_picker_title);

            if (mSitePickerMode == SitePickerMode.REBLOG_CONTINUE_MODE && mReblogActionMode == null) {
                mViewModel.onRefreshReblogActionMode();
            }
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
                R.layout.site_picker_listitem,
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
                        if (mSitePickerMode == SitePickerMode.REBLOG_CONTINUE_MODE && !isInSearchMode) {
                            mAdapter.findAndSelect(mCurrentLocalId);
                            int scrollPos = mAdapter.getItemPosByLocalId(mCurrentLocalId);
                            if (scrollPos > -1 && mRecycleView != null) {
                                mRecycleView.scrollToPosition(scrollPos);
                            }
                        }
                    }
                },
                mSitePickerMode);
        mAdapter.setOnSiteClickListener(this);
        mAdapter.setOnSelectedCountChangedListener(this);
    }

    private void saveSiteVisibility(SiteRecord siteRecord) {
        Set<SiteRecord> siteRecords = new HashSet<>();
        siteRecords.add(siteRecord);
        saveSitesVisibility(siteRecords);
    }

    private void saveSitesVisibility(Set<SiteRecord> changeSet) {
        boolean skippedCurrentSite = false;
        String currentSiteName = null;
        SiteList hiddenSites = getAdapter().getHiddenSites();
        List<SiteModel> siteList = new ArrayList<>();
        for (SiteRecord siteRecord : changeSet) {
            SiteModel siteModel = mSiteStore.getSiteByLocalId(siteRecord.getLocalId());
            if (hiddenSites.contains(siteRecord)) {
                if (siteRecord.getLocalId() == mCurrentLocalId) {
                    skippedCurrentSite = true;
                    currentSiteName = siteRecord.getBlogNameOrHomeURL();
                    continue;
                }
                siteModel.setIsVisible(false);
                // Remove stats data for hidden sites
                mStatsStore.deleteSiteData(siteModel);
            } else {
                siteModel.setIsVisible(true);
            }
            // Save the site
            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(siteModel));
            siteList.add(siteModel);
        }

        updateVisibilityOfSitesOnRemote(siteList);

        // let user know the current site wasn't hidden
        if (skippedCurrentSite) {
            String cantHideCurrentSite = getString(R.string.site_picker_cant_hide_current_site);
            ToastUtils.showToast(this,
                    String.format(cantHideCurrentSite, currentSiteName),
                    ToastUtils.Duration.LONG);
        }
    }

    private void updateVisibilityOfSitesOnRemote(List<SiteModel> siteList) {
        // Example json format for the request: {"sites":{"100001":{"visible":false}}}
        JSONObject jsonObject = new JSONObject();
        try {
            JSONObject sites = new JSONObject();
            for (SiteModel siteModel : siteList) {
                JSONObject visible = new JSONObject();
                visible.put("visible", siteModel.isVisible());
                sites.put(Long.toString(siteModel.getSiteId()), visible);
            }
            jsonObject.put("sites", sites);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.API, "Could not build me/sites json object");
        }

        if (jsonObject.length() == 0) {
            return;
        }

        WordPress.getRestClientUtilsV1_1().post("me/sites", jsonObject, null,
                response -> AppLog.v(AppLog.T.API, "Site visibility successfully updated"),
                volleyError -> AppLog
                        .e(AppLog.T.API, "An error occurred while updating site visibility: " + volleyError));
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
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        mMenuSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (!getAdapter().getIsInSearchMode()) {
                    enableSearchMode();
                    mMenuEdit.setVisible(false);
                    mMenuAdd.setVisible(false);

                    mSearchView.setOnQueryTextListener(SitePickerActivity.this);
                }

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                disableSearchMode();
                mSearchView.setOnQueryTextListener(null);
                return true;
            }
        });

        setQueryIfInSearch();
    }

    private void setQueryIfInSearch() {
        if (getAdapter().getIsInSearchMode()) {
            mMenuSearch.expandActionView();
            mSearchView.setOnQueryTextListener(SitePickerActivity.this);
            mSearchView.setQuery(getAdapter().getLastSearch(), true);
        }
    }

    private void enableSearchMode() {
        setIsInSearchModeAndSetNewAdapter(true);
        mRecycleView.swapAdapter(getAdapter(), true);
    }

    private void disableSearchMode() {
        hideSoftKeyboard();
        setIsInSearchModeAndSetNewAdapter(false);
        mRecycleView.swapAdapter(getAdapter(), true);
        invalidateOptionsMenu();
    }

    private void hideSoftKeyboard() {
        if (!DeviceUtils.getInstance().hasHardwareKeyboard(this)) {
            ActivityUtils.hideKeyboardForced(mSearchView);
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
    public boolean onSiteLongClick(final SiteRecord siteRecord) {
        final SiteModel site = mSiteStore.getSiteByLocalId(siteRecord.getLocalId());
        if (site == null) {
            return false;
        }
        if (site.isUsingWpComRestApi()) {
            if (mActionMode != null) {
                return false;
            }
            startEditingVisibility();
        } else {
            showRemoveSelfHostedSiteDialog(site);
        }
        return true;
    }

    @Override
    public void onSiteClick(SiteRecord siteRecord) {
        if (mSitePickerMode.isReblogMode()) {
            mCurrentLocalId = siteRecord.getLocalId();
            mViewModel.onSiteForReblogSelected(siteRecord);
        } else if (mActionMode == null) {
            selectSiteAndFinish(siteRecord);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        hideSoftKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        if (getAdapter().getIsInSearchMode()) {
            getAdapter().setLastSearch(s);
            getAdapter().searchSites(s);
        }
        return true;
    }

    public void showProgress(boolean show) {
        findViewById(R.id.progress).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void selectSiteAndFinish(SiteRecord siteRecord) {
        hideSoftKeyboard();
        AppPrefs.addRecentlyPickedSiteId(siteRecord.getLocalId());
        setResult(RESULT_OK, new Intent().putExtra(KEY_LOCAL_ID, siteRecord.getLocalId()));
        // If the site is hidden, make sure to make it visible
        if (siteRecord.isHidden()) {
            siteRecord.setHidden(false);
            saveSiteVisibility(siteRecord);
        }
        finish();
    }

    private final class ReblogActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mReblogActionMode = mode;
            mode.getMenuInflater().inflate(R.menu.site_picker_reblog_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();

            if (itemId == R.id.continue_flow) {
                mViewModel.onContinueFlowSelected();
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mViewModel.onReblogActionBackSelected();
            mReblogActionMode = null;
        }
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        private boolean mHasChanges;
        private Set<SiteRecord> mChangeSet;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
            mHasChanges = false;
            mChangeSet = new HashSet<>();
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
                Set<SiteRecord> changeSet = getAdapter().setVisibilityForSelectedSites(true);
                mChangeSet.addAll(changeSet);
                mHasChanges = true;
                mActionMode.finish();
            } else if (itemId == R.id.menu_hide) {
                Set<SiteRecord> changeSet = getAdapter().setVisibilityForSelectedSites(false);
                mChangeSet.addAll(changeSet);
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
                saveSitesVisibility(mChangeSet);
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
                    {getString(R.string.site_picker_create_wpcom),
                            getString(R.string.site_picker_add_self_hosted)};
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            builder.setTitle(R.string.site_picker_add_site);
            builder.setAdapter(
                    new ArrayAdapter<>(getActivity(), R.layout.add_new_site_dialog_item, R.id.text, items),
                    (dialog, which) -> {
                        if (which == 0) {
                            ActivityLauncher.newBlogForResult(getActivity());
                        } else {
                            ActivityLauncher.addSelfHostedSiteForResult(getActivity());
                        }
                    });
            return builder.create();
        }
    }

    private void startEditingVisibility() {
        mRecycleView.setItemAnimator(new DefaultItemAnimator());
        getAdapter().setEnableEditMode(true);
        startSupportActionMode(new ActionModeCallback());
    }

    private void showRemoveSelfHostedSiteDialog(@NonNull final SiteModel site) {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle(getResources().getText(R.string.remove_account));
        dialogBuilder.setMessage(getResources().getText(R.string.sure_to_remove_account));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                (dialog, whichButton) -> mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(site)));
        dialogBuilder.setNegativeButton(getResources().getText(R.string.no), null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
    }

    private void showSiteCreatedButNotFetchedSnackbar() {
        int duration = AccessibilityUtils
                .getSnackbarDuration(this, getResources().getInteger(R.integer.site_creation_snackbar_duration));
        String message = getString(R.string.site_created_but_not_fetched_snackbar_message);
        WPDialogSnackbar.make(findViewById(R.id.coordinatorLayout), message, duration).show();
    }
}
