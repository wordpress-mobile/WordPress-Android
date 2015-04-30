package org.wordpress.android.ui.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.AccountHelper;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.DividerItemDecoration;

import de.greenrobot.event.EventBus;

public class SitePickerActivity extends ActionBarActivity
        implements SitePickerAdapter.OnSiteClickListener,
                   SitePickerAdapter.OnSelectedCountChangedListener {

    public static final String KEY_LOCAL_ID = "local_id";

    private SitePickerAdapter mAdapter;
    private ActionMode mActionMode;
    private int mCurrentLocalId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.site_picker_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            mCurrentLocalId = savedInstanceState.getInt(KEY_LOCAL_ID);
        } else if (getIntent() != null) {
            mCurrentLocalId = getIntent().getIntExtra(KEY_LOCAL_ID, 0);
        }

        Button btnAddSite = (Button) findViewById(R.id.btn_add);
        btnAddSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSite();
            }
        });

        RecyclerView recycler = (RecyclerView) findViewById(R.id.recycler_view);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        recycler.setAdapter(getAdapter());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_LOCAL_ID, mCurrentLocalId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_left);
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

        // don't allow editing visibility unless there are multiple wp.com blogs
        int numSites = WordPress.wpDB.getNumDotComBlogs();
        MenuItem mnuEdit = menu.findItem(R.id.menu_edit);
        mnuEdit.setVisible(numSites > 1);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.menu_edit) {
            getAdapter().setEnableEditMode(true);
            startSupportActionMode(new ActionModeCallback());
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

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.BlogListChanged event) {
        if (!isFinishing()) {
            getAdapter().loadSites();
        }
    }

    private SitePickerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new SitePickerAdapter(this, mCurrentLocalId);
            mAdapter.setOnSiteClickListener(this);
            mAdapter.setOnSelectedCountChangedListener(this);
        }
        return mAdapter;
    }

    @Override
    public void onSelectedCountChanged(int numSelected) {
        if (mActionMode != null) {
            updateActionModeTitle();
            mActionMode.invalidate();
        }
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

    private void addSite() {
        // if user is signed into wp.com use the dialog to enable choosing whether to
        // create a new wp.com blog or add a self-hosted one
        if (AccountHelper.getDefaultAccount().isWordPressComUser()) {
            DialogFragment dialog = new AddSiteDialog();
            dialog.show(getSupportFragmentManager(), AddSiteDialog.ADD_SITE_DIALOG_TAG);
        } else {
            // user isn't signed into wp.com, so simply enable adding self-hosted
            ActivityLauncher.addSelfHostedSiteForResult(SitePickerActivity.this);
        }
    }

    @Override
    public void onSiteClick(SiteRecord site) {
        if (mActionMode == null) {
            Intent data = new Intent();
            data.putExtra(KEY_LOCAL_ID, site.localId);
            setResult(RESULT_OK, data);
            finish();
        }
    }

    private void updateActionModeTitle() {
        if (mActionMode != null) {
            int numSelected = getAdapter().getNumSelected();
            mActionMode.setTitle(getString(R.string.cab_selected, numSelected));
        }
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
