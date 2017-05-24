package org.wordpress.android.ui.publicize;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter;
import org.wordpress.android.ui.publicize.services.PublicizeUpdateService;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class PublicizeListActivity extends AppCompatActivity
        implements
        PublicizeActions.OnPublicizeActionListener,
        PublicizeServiceAdapter.OnServiceClickListener,
        PublicizeListFragment.PublicizeManageConnectionsListener {

    private SiteModel mSite;
    private ProgressDialog mProgressDialog;

    @Inject AccountStore mAccountStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.publicize_list_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            PublicizeTable.createTables(WordPress.wpDB.getDatabase());
            showListFragment(mSite.getId());
            PublicizeUpdateService.updateConnectionsForSite(this, mSite.getId());
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
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

        long currentUserId = mAccountStore.getAccount().getUserId();
        String tag = getString(R.string.fragment_tag_publicize_list);
        Fragment fragment = PublicizeListFragment.newInstance(siteId, currentUserId);
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

    private void showDetailFragment(int siteId, PublicizeService service) {
        if (isFinishing()) return;

        String tag = getString(R.string.fragment_tag_publicize_detail);
        Fragment detailFragment = PublicizeDetailFragment.newInstance(siteId, service);
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
            detailFragment.loadData();
        }
    }

    private void showWebViewFragment(int siteId,
                                     PublicizeService service,
                                     PublicizeConnection publicizeConnection) {
        if (isFinishing()) return;

        String tag = getString(R.string.fragment_tag_publicize_webview);
        Fragment webViewFragment = PublicizeWebViewFragment.newInstance(siteId, service, publicizeConnection);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, webViewFragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(tag)
                .commit();
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
    public void onServiceClicked(PublicizeService service) {
        showDetailFragment(mSite.getId(), service);
    }

    /*
     * user requested to connect to a service from the detail fragment
     */
    @Override
    public void onRequestConnect(PublicizeService service) {
        showWebViewFragment(mSite.getId(), service, null);
    }

    /*
     * user requested to reconnect a broken publicizeConnection from the detail fragment
     */
    @Override
    public void onRequestReconnect(PublicizeService service, PublicizeConnection publicizeConnection) {
        showWebViewFragment(mSite.getId(), service, publicizeConnection);
    }

    /*
     * user requested to disconnect a service publicizeConnection from the detail fragment
     */
    @Override
    public void onRequestDisconnect(PublicizeConnection publicizeConnection) {
        confirmDisconnect(publicizeConnection);
    }

    private void confirmDisconnect(final PublicizeConnection publicizeConnection) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
                String.format(getString(R.string.dlg_confirm_publicize_disconnect), publicizeConnection.getLabel()));
        builder.setTitle(R.string.share_btn_disconnect);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.share_btn_disconnect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                PublicizeActions.disconnect(publicizeConnection);
                reloadDetailFragment();
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

        closeWebViewFragment();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        reloadDetailFragment();

        if (!event.didSucceed()) {
            ToastUtils.showToast(this, R.string.error_generic);
        }
    }

    public void onEventMainThread(PublicizeEvents.ActionAccountChosen event) {
        if (isFinishing()) return;

        PublicizeActions.connectStepTwo(event.getSiteId(), event.getKeychainId());
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.connecting_account));
        mProgressDialog.show();
    }

    public void onEventMainThread(PublicizeEvents.ActionRequestChooseAccount event) {
        if (isFinishing()) return;

        closeWebViewFragment();

        PublicizeAccountChooserDialogFragment dialogFragment = new PublicizeAccountChooserDialogFragment();
        Bundle args = new Bundle();
        args.putString(PublicizeConstants.ARG_CONNECTION_ARRAY_JSON, event.getJSONObject().toString());
        args.putInt(PublicizeConstants.ARG_SITE_ID, event.getSiteId());
        args.putString(PublicizeConstants.ARG_SERVICE_ID, event.getServiceId());
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), PublicizeAccountChooserDialogFragment.TAG);
    }

    @Override
    public void onManageConnectionsClicked() {
        PublicizeManageConnectionsFragment fragment = PublicizeManageConnectionsFragment.newInstance(mSite);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
