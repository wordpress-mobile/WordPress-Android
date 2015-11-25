package org.wordpress.android.ui.publicize;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter;
import org.wordpress.android.ui.publicize.services.PublicizeUpdateService;
import org.wordpress.android.util.ToastUtils;

import de.greenrobot.event.EventBus;

public class PublicizeListActivity extends AppCompatActivity
        implements
        PublicizeDetailFragment.OnPublicizeActionListener,
        PublicizeServiceAdapter.OnServiceConnectionClickListener {

    private int mSiteId;
    private Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.publicize_list_activity);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            PublicizeTable.createTables(WordPress.wpDB.getDatabase());
            mSiteId = getIntent().getIntExtra(PublicizeConstants.ARG_SITE_ID, 0);
            showListFragment(mSiteId);
            PublicizeUpdateService.updateConnectionsForSite(this, mSiteId);
        } else {
            mSiteId = savedInstanceState.getInt(PublicizeConstants.ARG_SITE_ID);
        }

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                // clear the nav icon and subtitle used for the webView fragment
                if (getFragmentManager().getBackStackEntryCount() < 2) {
                    mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
                    mToolbar.setSubtitle(null);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PublicizeConstants.ARG_SITE_ID, mSiteId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    private void showListFragment(int siteId) {
        if (isFinishing()) return;

        String tag = getString(R.string.fragment_tag_publicize_list);
        Fragment fragment = PublicizeListFragment.newInstance(siteId);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    private PublicizeListFragment getListFragment() {
        String tag = getString(R.string.fragment_tag_publicize_list);
        Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            return (PublicizeListFragment) fragment;
        } else {
            return null;
        }
    }

    private void reloadListFragment() {
        PublicizeListFragment listFragment = getListFragment();
        if (listFragment != null) {
            listFragment.reload();
        }
    }

    /*
     * close all but the first (list) fragment
     */
    private void returnToListFragment() {
        if (getFragmentManager().getBackStackEntryCount() == 0) return;

        String tag = getString(R.string.fragment_tag_publicize_detail);
        getFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void showDetailFragment(int siteId,
                                    PublicizeService service,
                                    PublicizeConnection connection) {
        if (isFinishing()) return;

        String tag = getString(R.string.fragment_tag_publicize_detail);
        Fragment detailFragment = PublicizeDetailFragment.newInstance(siteId, service, connection);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(tag)
                .commit();
    }

    private PublicizeDetailFragment getDetailFragment() {
        String tag = getString(R.string.fragment_tag_publicize_detail);
        Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            return (PublicizeDetailFragment) fragment;
        } else {
            return null;
        }
    }

    private void reloadDetailFragment() {
        PublicizeDetailFragment detailFragment = getDetailFragment();
        if (detailFragment != null) {
            detailFragment.getData();
        }
    }

    private void showWebViewFragment(int siteId, PublicizeService service) {
        if (isFinishing()) return;

        String tag = getString(R.string.fragment_tag_publicize_webview);
        Fragment webViewFragment = PublicizeWebViewFragment.newInstance(siteId, service);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, webViewFragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(tag)
                .commit();

        mToolbar.setSubtitle(service.getLabel());
        mToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
    }

    private PublicizeWebViewFragment getWebViewFragment() {
        String tag = getString(R.string.fragment_tag_publicize_webview);
        Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            return (PublicizeWebViewFragment) fragment;
        } else {
            return null;
        }
    }

    private void closeWebViewFragment() {
        String tag = getString(R.string.fragment_tag_publicize_webview);
        getFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    /*
     * user tapped a service in the list fragment
     */
    @Override
    public void onServiceConnectionClicked(PublicizeService service, PublicizeConnection connection) {
        showDetailFragment(mSiteId, service, connection);
    }

    /*
     * user requested to connect to a service from the detail fragment
     */
    @Override
    public void onRequestConnect(PublicizeService service) {
        showWebViewFragment(mSiteId, service);
    }

    /*
     * user requested to reconnect a broken connection from the detail fragment
     */
    @Override
    public void onRequestReconnect(PublicizeConnection connection) {
        PublicizeActions.reconnect(connection);
        returnToListFragment();
    }

    /*
     * user requested to disconnect a service connection from the detail fragment
     */
    @Override
    public void onRequestDisconnect(PublicizeConnection connection) {
        confirmDisconnect(connection);
    }

    private void confirmDisconnect(final PublicizeConnection connection) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
                String.format(getString(R.string.dlg_confirm_publicize_disconnect), connection.getLabel()));
        builder.setTitle(R.string.share_btn_disconnect);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.share_btn_disconnect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                PublicizeActions.disconnect(connection);
                returnToListFragment();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog alert = builder.create();
        alert.show();
    }

    /*
     * list of available services or list of connections has changed
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PublicizeEvents.ConnectionsChanged event) {
        reloadListFragment();
    }

    /*
     * request from fragment to connect/disconnect/reconnect completed
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PublicizeEvents.ActionCompleted event) {
        if (isFinishing()) return;

        if (event.didSucceed()) {
            returnToListFragment();
        } else {
            closeWebViewFragment();
            reloadDetailFragment();
            ToastUtils.showToast(this, R.string.error_generic);
        }
    }
}
