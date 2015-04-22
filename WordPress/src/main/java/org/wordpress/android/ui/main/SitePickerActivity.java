package org.wordpress.android.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.widgets.DividerItemDecoration;

import de.greenrobot.event.EventBus;

public class SitePickerActivity extends ActionBarActivity
        implements SitePickerAdapter.OnSiteClickListener,
        SitePickerAdapter.OnSelectedCountChangedListener {

    // TODO: remove show/hide blogs from settings

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
        } else if (itemId == R.id.menu_add) {
            ActivityLauncher.addSelfHostedSiteForResult(this);
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

    private boolean hasAdapter() {
        return mAdapter != null;
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
            mActionMode.invalidate();
        }
    }

    private void saveHiddenSites() {
        WordPress.wpDB.getDatabase().beginTransaction();
        try {
            // make all sites visible...
            WordPress.wpDB.setAllDotComBlogsVisibility(true);

            // ...then update ones marked hidden in the adapter
            SiteList hiddenSites = getAdapter().getHiddenSites();
            for (SiteRecord site : hiddenSites) {
                // don't allow hiding the current site
                if (site.localId != mCurrentLocalId) {
                    WordPress.wpDB.setDotComBlogsVisibility(site.localId, false);
                }
            }

            WordPress.wpDB.getDatabase().setTransactionSuccessful();
        } finally {
            WordPress.wpDB.getDatabase().endTransaction();
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

    private final class ActionModeCallback implements ActionMode.Callback {
        private boolean mHasChanges;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
            mHasChanges = false;

            actionMode.setTitle(getString(R.string.site_picker_title_action_mode));
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
}
