package org.wordpress.android.ui.main;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
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
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.databinding.SitePickerActivityBinding;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.StatsStore;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginFragment;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord;
import org.wordpress.android.ui.mysite.SelectedSiteRepository;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.BuildConfigWrapper;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SitePickerActivity extends LocaleAwareActivity
        implements SitePickerAdapter.OnSiteClickListener,
        SitePickerAdapter.OnSelectedCountChangedListener,
        SearchView.OnQueryTextListener {
    public static final String KEY_SITE_LOCAL_ID = "local_id";
    public static final String KEY_SITE_CREATED_BUT_NOT_FETCHED = "key_site_created_but_not_fetched";
    public static final String KEY_SITE_TITLE_TASK_COMPLETED = "key_site_title_task_completed";

    public static final String KEY_SITE_PICKER_MODE = "key_site_picker_mode";

    private static final String KEY_IS_IN_SEARCH_MODE = "is_in_search_mode";
    private static final String KEY_LAST_SEARCH = "last_search";
    private static final String KEY_REFRESHING = "refreshing_sites";

    // Used for preserving selection states after configuration change.
    private static final String KEY_SELECTED_POSITIONS = "selected_positions";
    private static final String KEY_IS_IN_EDIT_MODE = "is_in_edit_mode";
    private static final String KEY_IS_SHOW_MENU_ENABLED = "is_show_menu_enabled";
    private static final String KEY_IS_HIDE_MENU_ENABLED = "is_hide_menu_enabled";

    private static final String ARG_SITE_CREATION_SOURCE = "ARG_SITE_CREATION_SOURCE";
    private static final String SOURCE = "source";
    private static final String TRACK_PROPERTY_STATE = "state";
    private static final String TRACK_PROPERTY_STATE_EDIT = "edit";
    private static final String TRACK_PROPERTY_STATE_DONE = "done";
    private static final String TRACK_PROPERTY_BLOG_ID = "blog_id";
    private static final String TRACK_PROPERTY_VISIBLE = "visible";

    @Nullable private SitePickerAdapter mAdapter;
    @Nullable private SwipeToRefreshHelper mSwipeToRefreshHelper;
    @Nullable private ActionMode mActionMode;
    @Nullable private ActionMode mReblogActionMode;
    @Nullable private MenuItem mMenuEdit;
    @Nullable private MenuItem mMenuAdd;
    @Nullable private MenuItem mMenuSearch;
    @Nullable private SearchView mSearchView;
    private int mCurrentLocalId;
    @Nullable private SitePickerMode mSitePickerMode;
    @NonNull private final Debouncer mDebouncer = new Debouncer();
    @Nullable private SitePickerViewModel mViewModel;

    @NonNull private HashSet<Integer> mSelectedPositions = new HashSet<>();
    private boolean mIsInEditMode;

    private boolean mShowMenuEnabled = false;
    private boolean mHideMenuEnabled = false;


    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;
    @Inject StatsStore mStatsStore;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject BuildConfigWrapper mBuildConfigWrapper;

    @Nullable private SitePickerActivityBinding mBinding = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(SitePickerViewModel.class);

        mBinding = SitePickerActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        restoreSavedInstanceState(savedInstanceState);
        setupActionBar(mBinding);
        setupRecycleView(mBinding);

        mSwipeToRefreshHelper = initSwipeToRefreshHelper(mBinding);
        if (savedInstanceState != null) {
            mSwipeToRefreshHelper.setRefreshing(savedInstanceState.getBoolean(KEY_REFRESHING, false));
        } else {
            AnalyticsTracker.track(Stat.SITE_SWITCHER_DISPLAYED);
        }

        mViewModel.getOnActionTriggered().observe(
                this,
                unitEvent -> unitEvent.applyIfNotHandled(action -> {
                    switch (action.getActionType()) {
                        case NAVIGATE_TO_STATE:
                            if (!(mSitePickerMode != null && mSitePickerMode.isReblogMode())) break;
                            switch (((NavigateToState) action).getNavigateState()) {
                                case TO_SITE_SELECTED:
                                    mSitePickerMode = SitePickerMode.REBLOG_CONTINUE_MODE;
                                    if (getAdapter().getIsInSearchMode()) {
                                        disableSearchMode(mBinding);
                                    }

                                    if (mReblogActionMode == null) {
                                        startSupportActionMode(new ReblogActionModeCallback());
                                    }

                                    SiteRecord site = ((NavigateToState) action).getSiteForReblog();
                                    if (site != null && mReblogActionMode != null) {
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
                            if (!(mSitePickerMode != null && mSitePickerMode.isReblogMode())) break;
                            SiteRecord siteToReblog = ((ContinueReblogTo) action).getSiteForReblog();
                            if (siteToReblog != null) selectSiteAndFinish(siteToReblog);
                            break;
                        case ASK_FOR_SITE_SELECTION:
                            if (!(mSitePickerMode != null && mSitePickerMode.isReblogMode())) break;
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
                        case SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY:
                            WPJetpackIndividualPluginFragment.show(getSupportFragmentManager());
                            break;
                    }
                    return null;
                }));
        // If the picker is already in editing mode from previous configuration, re-enable the editing mode.
        if (mIsInEditMode) {
            startEditingVisibility(mBinding);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.SITE_PICKER);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_SITE_LOCAL_ID, mCurrentLocalId);
        outState.putBoolean(KEY_IS_IN_SEARCH_MODE, getAdapter().getIsInSearchMode());
        outState.putString(KEY_LAST_SEARCH, getAdapter().getLastSearch());
        if (mSwipeToRefreshHelper != null) {
            outState.putBoolean(KEY_REFRESHING, mSwipeToRefreshHelper.isRefreshing());
        } else {
            outState.putBoolean(KEY_REFRESHING, false);
        }
        outState.putSerializable(KEY_SITE_PICKER_MODE, mSitePickerMode);

        outState.putSerializable(KEY_SELECTED_POSITIONS, getAdapter().getSelectedPositions());
        outState.putBoolean(KEY_IS_IN_EDIT_MODE, mIsInEditMode);
        outState.putBoolean(KEY_IS_SHOW_MENU_ENABLED, mShowMenuEnabled);
        outState.putBoolean(KEY_IS_HIDE_MENU_ENABLED, mHideMenuEnabled);

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.site_picker, menu);
        mMenuSearch = menu.findItem(R.id.menu_search);
        mMenuEdit = menu.findItem(R.id.menu_edit);
        mMenuAdd = menu.findItem(R.id.menu_add);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mBinding != null && mMenuSearch != null) {
            updateMenuItemVisibility();
            mSearchView = getSearchView(mMenuSearch);
            setupSearchView(mBinding, mMenuSearch, mSearchView);
            return true;
        } else {
            return false;
        }
    }

    private void updateMenuItemVisibility() {
        if (mMenuAdd == null || mMenuEdit == null || mMenuSearch == null) {
            return;
        }

        if (getAdapter().getIsInSearchMode()
            || mSitePickerMode == null
            || mSitePickerMode.isReblogMode()
            || mSitePickerMode.isBloggingPromptsMode()
            || mSitePickerMode == SitePickerMode.SIMPLE_MODE) {
            mMenuEdit.setVisible(false);
            mMenuAdd.setVisible(false);
        } else {
            // don't allow editing visibility unless there are multiple wp.com and jetpack sites
            mMenuEdit.setVisible(mSiteStore.getSitesAccessedViaWPComRestCount() > 1);
            mMenuAdd.setVisible(mBuildConfigWrapper.isSiteCreationEnabled());
        }

        // no point showing search if there aren't multiple blogs
        mMenuSearch.setVisible(mSiteStore.getSitesCount() > 1);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            AnalyticsTracker.track(Stat.SITE_SWITCHER_DISMISSED);
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (itemId == R.id.menu_edit) {
            if (mBinding != null) {
                AnalyticsTracker.track(Stat.SITE_SWITCHER_TOGGLED_EDIT_TAPPED,
                        Collections.singletonMap(TRACK_PROPERTY_STATE, TRACK_PROPERTY_STATE_EDIT));
                startEditingVisibility(mBinding);
                return true;
            }
        } else if (itemId == R.id.menu_add) {
            AnalyticsTracker.track(Stat.SITE_SWITCHER_ADD_SITE_TAPPED);
            addSite(this, mAccountStore.hasAccessToken(), SiteCreationSource.MY_SITE);
            return true;
        } else if (itemId == R.id.continue_flow) {
            if (mViewModel != null) mViewModel.onContinueFlowSelected();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
                        if (mBinding != null) {
                            showSiteCreatedButNotFetchedSnackbar(mBinding);
                        }
                    } else {
                        data.putExtra(WPMainActivity.ARG_CREATE_SITE, RequestCodes.CREATE_SITE);
                        setResult(resultCode, data);
                        finish();
                    }
                }
                break;
        }

        // Enable the block editor on sites created on mobile
        if (requestCode == RequestCodes.CREATE_SITE) {
            if (data != null) {
                int newSiteLocalID = data.getIntExtra(
                        SitePickerActivity.KEY_SITE_LOCAL_ID,
                        SelectedSiteRepository.UNAVAILABLE
                );
                SiteUtils.enableBlockEditorOnSiteCreation(mDispatcher, mSiteStore, newSiteLocalID);
            }
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
        if (mSwipeToRefreshHelper != null && mSwipeToRefreshHelper.isRefreshing()) {
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

    @NonNull
    private SwipeToRefreshHelper initSwipeToRefreshHelper(@NonNull SitePickerActivityBinding binding) {
        return buildSwipeToRefreshHelper(
                binding.ptrLayout,
                () -> {
                    if (isFinishing()) {
                        return;
                    }
                    if (!NetworkUtils.checkConnection(this) || !mAccountStore.hasAccessToken()) {
                        if (mSwipeToRefreshHelper != null) mSwipeToRefreshHelper.setRefreshing(false);
                        return;
                    }
                    mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(SiteUtils.getFetchSitesPayload()));
                }
        );
    }

    private void setupRecycleView(@NonNull SitePickerActivityBinding binding) {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

        binding.recyclerView.setItemAnimator(
                (mSitePickerMode != null && mSitePickerMode.isReblogMode()) ? new DefaultItemAnimator() : null
        );

        binding.recyclerView.setAdapter(getAdapter());

        binding.actionableEmptyView.updateLayoutForSearch(true, 0);
        binding.recyclerView.setEmptyView(binding.actionableEmptyView);
    }

    @SuppressWarnings("unchecked")
    private void restoreSavedInstanceState(@Nullable Bundle savedInstanceState) {
        boolean isInSearchMode = false;
        String lastSearch = "";

        if (savedInstanceState != null) {
            mCurrentLocalId = savedInstanceState.getInt(KEY_SITE_LOCAL_ID);
            isInSearchMode = savedInstanceState.getBoolean(KEY_IS_IN_SEARCH_MODE);
            lastSearch = savedInstanceState.getString(KEY_LAST_SEARCH);
            mSitePickerMode = (SitePickerMode) savedInstanceState.getSerializable(KEY_SITE_PICKER_MODE);

            mSelectedPositions = (HashSet<Integer>) savedInstanceState.getSerializable(KEY_SELECTED_POSITIONS);
            mIsInEditMode = savedInstanceState.getBoolean(KEY_IS_IN_EDIT_MODE);
            mShowMenuEnabled = savedInstanceState.getBoolean(KEY_IS_SHOW_MENU_ENABLED);
            mHideMenuEnabled = savedInstanceState.getBoolean(KEY_IS_HIDE_MENU_ENABLED);
        } else if (getIntent() != null) {
            mCurrentLocalId = getIntent().getIntExtra(KEY_SITE_LOCAL_ID, SelectedSiteRepository.UNAVAILABLE);
            mSitePickerMode = (SitePickerMode) getIntent().getSerializableExtra(KEY_SITE_PICKER_MODE);
        }

        mAdapter = createNewAdapter(lastSearch, isInSearchMode);
    }

    private void setupActionBar(@NonNull SitePickerActivityBinding binding) {
        setSupportActionBar(binding.toolbarMain);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.site_picker_title);

            if (mSitePickerMode == SitePickerMode.REBLOG_CONTINUE_MODE && mReblogActionMode == null) {
                if (mViewModel != null) mViewModel.onRefreshReblogActionMode();
            }
        }
    }

    private void setIsInSearchModeAndSetNewAdapter(boolean isInSearchMode) {
        String lastSearch = getAdapter().getLastSearch();
        mAdapter = createNewAdapter(lastSearch, isInSearchMode);
    }

    @NonNull
    private SitePickerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = createNewAdapter("", false);
        }
        return mAdapter;
    }

    @NonNull
    private SitePickerAdapter createNewAdapter(String lastSearch, boolean isInSearchMode) {
        SitePickerAdapter adapter = new SitePickerAdapter(
                this,
                R.layout.site_picker_listitem,
                mCurrentLocalId,
                lastSearch,
                isInSearchMode,
                new SitePickerAdapter.OnDataLoadedListener() {
                    @Override
                    public void onBeforeLoad(boolean isEmpty) {
                        if (mBinding != null && isEmpty) {
                            showProgress(mBinding, true);
                        }
                    }

                    @Override
                    public void onAfterLoad() {
                        if (mBinding != null && mViewModel != null) {
                            showProgress(mBinding, false);
                            if (mSitePickerMode == SitePickerMode.REBLOG_CONTINUE_MODE && !isInSearchMode) {
                                getAdapter().findAndSelect(mCurrentLocalId);
                                int scrollPos = getAdapter().getItemPosByLocalId(mCurrentLocalId);
                                if (scrollPos > -1) {
                                    mBinding.recyclerView.scrollToPosition(scrollPos);
                                }
                            }
                            mViewModel.onSiteListLoaded();
                        }
                    }
                },
                mSitePickerMode,
                mIsInEditMode);
        adapter.setOnSiteClickListener(this);
        adapter.setOnSelectedCountChangedListener(this);
        return adapter;
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
            if (siteModel != null) {
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
                trackVisibility(Long.toString(siteModel.getSiteId()), siteModel.isVisible());
            }
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

    @NonNull
    private SearchView getSearchView(@NonNull MenuItem menuSearch) {
        SearchView searchView = (SearchView) menuSearch.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        return searchView;
    }

    private void setupSearchView(
            @NonNull SitePickerActivityBinding binding,
            @NonNull MenuItem menuSearch,
            @NonNull SearchView searchView
    ) {
        menuSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                if (!getAdapter().getIsInSearchMode() && mMenuEdit != null && mMenuAdd != null) {
                    enableSearchMode(binding);
                    mMenuEdit.setVisible(false);
                    mMenuAdd.setVisible(false);

                    searchView.setOnQueryTextListener(SitePickerActivity.this);
                }

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                disableSearchMode(binding);
                searchView.setOnQueryTextListener(null);
                return true;
            }
        });

        setQueryIfInSearch(menuSearch, searchView);
    }

    private void setQueryIfInSearch(
            @NonNull MenuItem menuSearch,
            @NonNull SearchView searchView
    ) {
        if (getAdapter().getIsInSearchMode()) {
            menuSearch.expandActionView();
            searchView.setOnQueryTextListener(SitePickerActivity.this);
            searchView.setQuery(getAdapter().getLastSearch(), true);
        }
    }

    private void enableSearchMode(@NonNull SitePickerActivityBinding binding) {
        setIsInSearchModeAndSetNewAdapter(true);
        binding.recyclerView.swapAdapter(getAdapter(), true);
    }

    private void disableSearchMode(@NonNull SitePickerActivityBinding binding) {
        hideSoftKeyboard();
        setIsInSearchModeAndSetNewAdapter(false);
        binding.recyclerView.swapAdapter(getAdapter(), true);
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
            mShowMenuEnabled = getAdapter().getNumHiddenSelected() > 0;
            mHideMenuEnabled = getAdapter().getNumVisibleSelected() > 0;
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
            if (mBinding == null || mActionMode != null) {
                return false;
            }
            startEditingVisibility(mBinding);
        } else {
            showRemoveSelfHostedSiteDialog(site);
        }

        return true;
    }

    @Override
    public void onSiteClick(SiteRecord siteRecord) {
        if (mSitePickerMode != null && mSitePickerMode.isReblogMode() && mViewModel != null) {
            mCurrentLocalId = siteRecord.getLocalId();
            mViewModel.onSiteForReblogSelected(siteRecord);
        } else if (mActionMode == null) {
            selectSiteAndFinish(siteRecord);
        }
    }

    @Override
    public boolean onQueryTextSubmit(@NonNull String s) {
        hideSoftKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(@NonNull String s) {
        if (getAdapter().getIsInSearchMode()) {
            AnalyticsTracker.track(Stat.SITE_SWITCHER_SEARCH_PERFORMED);
            getAdapter().setLastSearch(s);
            getAdapter().searchSites(s);
        }
        return true;
    }

    public void showProgress(@NonNull SitePickerActivityBinding binding, boolean show) {
        binding.progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void selectSiteAndFinish(@NonNull SiteRecord siteRecord) {
        hideSoftKeyboard();
        AppPrefs.addRecentlyPickedSiteId(siteRecord.getLocalId());
        setResult(RESULT_OK, new Intent().putExtra(KEY_SITE_LOCAL_ID, siteRecord.getLocalId()));
        // If the site is hidden, make sure to make it visible
        if (siteRecord.isHidden()) {
            siteRecord.setHidden(false);
            saveSiteVisibility(siteRecord);
        }
        finish();
    }

    private final class ReblogActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
            mReblogActionMode = mode;
            mode.getMenuInflater().inflate(R.menu.site_picker_reblog_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(@NonNull ActionMode mode, @NonNull MenuItem item) {
            int itemId = item.getItemId();

            if (itemId == R.id.continue_flow && mViewModel != null) {
                mViewModel.onContinueFlowSelected();
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(@NonNull ActionMode mode) {
            if (mViewModel != null) mViewModel.onReblogActionBackSelected();
            mReblogActionMode = null;
        }
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        private boolean mHasChanges;
        private Set<SiteRecord> mChangeSet;

        @Override
        public boolean onCreateActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
            mActionMode = mode;
            mHasChanges = false;
            mChangeSet = new HashSet<>();
            updateActionModeTitle();
            mode.getMenuInflater().inflate(R.menu.site_picker_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
            MenuItem mnuShow = menu.findItem(R.id.menu_show);
            mnuShow.setEnabled(mShowMenuEnabled);

            MenuItem mnuHide = menu.findItem(R.id.menu_hide);
            mnuHide.setEnabled(mHideMenuEnabled);

            MenuItem mnuSelectAll = menu.findItem(R.id.menu_select_all);
            mnuSelectAll.setEnabled(getAdapter().getNumSelected() != getAdapter().getItemCount());

            MenuItem mnuDeselectAll = menu.findItem(R.id.menu_deselect_all);
            mnuDeselectAll.setEnabled(getAdapter().getNumSelected() > 0);

            return true;
        }

        @Override
        public boolean onActionItemClicked(@NonNull ActionMode mode, @NonNull MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_show) {
                Set<SiteRecord> changeSet = getAdapter().setVisibilityForSelectedSites(true);
                mChangeSet.addAll(changeSet);
                mHasChanges = true;
                if (mActionMode != null) mActionMode.finish();
            } else if (itemId == R.id.menu_hide) {
                Set<SiteRecord> changeSet = getAdapter().setVisibilityForSelectedSites(false);
                mChangeSet.addAll(changeSet);
                mHasChanges = true;
                if (mActionMode != null) mActionMode.finish();
            } else if (itemId == R.id.menu_select_all) {
                getAdapter().selectAll();
            } else if (itemId == R.id.menu_deselect_all) {
                getAdapter().deselectAll();
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(@NonNull ActionMode actionMode) {
            if (mHasChanges) {
                saveSitesVisibility(mChangeSet);
            }
            AnalyticsTracker.track(Stat.SITE_SWITCHER_TOGGLED_EDIT_TAPPED,
                    Collections.singletonMap(TRACK_PROPERTY_STATE, TRACK_PROPERTY_STATE_DONE));
            getAdapter().setEnableEditMode(false, mSelectedPositions);
            mActionMode = null;
            mIsInEditMode = false;
            mSelectedPositions.clear();
        }
    }

    public static void addSite(FragmentActivity activity, boolean hasAccessToken, SiteCreationSource source) {
        if (hasAccessToken) {
            if (!BuildConfig.ENABLE_ADD_SELF_HOSTED_SITE) {
                ActivityLauncher.newBlogForResult(activity, source);
            } else {
                // user is signed into wordpress app, so use the dialog to enable choosing whether to
                // create a new wp.com blog or add a self-hosted one
                showAddSiteDialog(activity, source);
            }
        } else {
            // user doesn't have an access token, so simply enable adding self-hosted
            ActivityLauncher.addSelfHostedSiteForResult(activity);
        }
    }

    private static void showAddSiteDialog(FragmentActivity activity, SiteCreationSource source) {
        DialogFragment dialog = new AddSiteDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SITE_CREATION_SOURCE, source.getLabel());
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), AddSiteDialog.ADD_SITE_DIALOG_TAG);
    }

    /**
     * Dialog which appears after user taps "Add site" - enables choosing whether to create
     * a new wp.com blog or add an existing self-hosted one.
     *
     * @apiNote Must pass {@link SitePickerActivity#ARG_SITE_CREATION_SOURCE} in arguments when creating this dialog.
     */
    public static class AddSiteDialog extends DialogFragment {
        static final String ADD_SITE_DIALOG_TAG = "add_site_dialog";

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            SiteCreationSource source =
                    SiteCreationSource.fromString(requireArguments().getString(ARG_SITE_CREATION_SOURCE));
            CharSequence[] items =
                    {getString(R.string.site_picker_create_wpcom),
                            getString(R.string.site_picker_add_self_hosted)};
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
            builder.setTitle(R.string.site_picker_add_site);
            builder.setAdapter(
                    new ArrayAdapter<>(requireActivity(), R.layout.add_new_site_dialog_item, R.id.text, items),
                    (dialog, which) -> {
                        if (which == 0) {
                            ActivityLauncher.newBlogForResult(getActivity(), source);
                        } else {
                            ActivityLauncher.addSelfHostedSiteForResult(getActivity());
                        }
                    });

            AnalyticsTracker.track(Stat.ADD_SITE_ALERT_DISPLAYED, Collections.singletonMap(SOURCE, source.getLabel()));
            return builder.create();
        }
    }

    private void startEditingVisibility(@NonNull SitePickerActivityBinding binding) {
        binding.recyclerView.setItemAnimator(new DefaultItemAnimator());
        getAdapter().setEnableEditMode(true, mSelectedPositions);
        startSupportActionMode(new ActionModeCallback());
        mIsInEditMode = true;
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

    private void showSiteCreatedButNotFetchedSnackbar(@NonNull SitePickerActivityBinding binding) {
        int duration = AccessibilityUtils
                .getSnackbarDuration(this, getResources().getInteger(R.integer.site_creation_snackbar_duration));
        String message = getString(R.string.site_created_but_not_fetched_snackbar_message);
        WPDialogSnackbar.make(binding.coordinatorLayout, message, duration).show();
    }

    private void trackVisibility(String blogId, boolean isVisible) {
        Map<String, String> props = new HashMap<>();
        props.put(TRACK_PROPERTY_BLOG_ID, blogId);
        props.put(TRACK_PROPERTY_VISIBLE, isVisible ? "1" : "0");
        AnalyticsTracker.track(Stat.SITE_SWITCHER_TOGGLE_BLOG_VISIBLE, props);
    }
}
