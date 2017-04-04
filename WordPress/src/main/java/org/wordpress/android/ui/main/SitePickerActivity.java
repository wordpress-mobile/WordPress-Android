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

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
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
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.helpers.Debouncer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
    private Debouncer mDebouncer = new Debouncer();

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
            mMenuEdit.setVisible(mSiteStore.getWPComAndJetpackSitesCount() > 1);
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
            case RequestCodes.CREATE_SITE:
                if (resultCode == RESULT_OK) {
                    getAdapter().loadSites();
                    setResult(resultCode, data);
                    finish();
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
    public void onSiteChanged(OnSiteChanged event) {
        mDebouncer.debounce(Void.class, new Runnable() {
            @Override public void run() {
                if (!isFinishing()) {
                    getAdapter().loadSites();
                }
            }
        }, 200, TimeUnit.MILLISECONDS);
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
            actionBar.setHomeAsUpIndicator(R.drawable.ic_cross_white_24dp);
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
            SiteModel siteModel = mSiteStore.getSiteByLocalId(siteRecord.localId);
            if (hiddenSites.contains(siteRecord)) {
                if (siteRecord.localId == mCurrentLocalId) {
                    skippedCurrentSite = true;
                    currentSiteName = siteRecord.getBlogNameOrHomeURL();
                    continue;
                }
                siteModel.setIsVisible(false);
                // Remove stats data for hidden sites
                StatsTable.deleteStatsForBlog(this, siteRecord.localId);
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

        WordPress.getRestClientUtilsV1_1().post("me/sites", jsonObject, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                AppLog.v(AppLog.T.API, "Site visibility successfully updated");
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.API, "An error occurred while updating site visibility: " + volleyError);
            }
        });
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
            AppPrefs.addRecentlyPickedSiteId(siteRecord.localId);
            setResult(RESULT_OK, new Intent().putExtra(KEY_LOCAL_ID, siteRecord.localId));
            mDidUserSelectSite = true;
            // If the site is hidden, make sure to make it visible
            if (siteRecord.isHidden) {
                siteRecord.isHidden = false;
                saveSiteVisibility(siteRecord);
            }
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
