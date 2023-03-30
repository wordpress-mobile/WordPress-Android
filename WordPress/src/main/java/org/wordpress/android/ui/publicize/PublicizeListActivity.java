package org.wordpress.android.ui.publicize;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.ScrollableViewInitializedListener;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter;
import org.wordpress.android.ui.publicize.services.PublicizeUpdateService;
import org.wordpress.android.util.JetpackBrandingUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.extensions.AppBarLayoutExtensionsKt;
import org.wordpress.android.util.extensions.CompatExtensionsKt;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PublicizeListActivity extends LocaleAwareActivity
        implements
        PublicizeActions.OnPublicizeActionListener,
        PublicizeServiceAdapter.OnServiceClickListener,
        PublicizeListFragment.PublicizeButtonPrefsListener, ScrollableViewInitializedListener {
    private static final String WPCOM_CONNECTIONS_URL = "https://wordpress.com/marketing/connections/";

    private SiteModel mSite;
    private ProgressDialog mProgressDialog;
    private AppBarLayout mAppBarLayout;

    @Inject SiteStore mSiteStore;
    @Inject JetpackBrandingUtils mJetpackBrandingUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.publicize_list_activity);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    CompatExtensionsKt.onBackPressedCompat(getOnBackPressedDispatcher(), this);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAppBarLayout = findViewById(R.id.appbar_main);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            PublicizeTable.createTables(WordPress.wpDB.getDatabase());
            showListFragment();
            if (mSite == null) {
                ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
                finish();
                return;
            }
            PublicizeUpdateService.updateConnectionsForSite(this, mSite);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
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

    private void showListFragment() {
        if (isFinishing()) {
            return;
        }

        String tag = getString(R.string.fragment_tag_publicize_list);
        Fragment fragment = PublicizeListFragment.newInstance(mSite);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    private PublicizeListFragment getListFragment() {
        String tag = getString(R.string.fragment_tag_publicize_list);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
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
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            return;
        }

        String tag = getString(R.string.fragment_tag_publicize_detail);
        getSupportFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void showDetailFragment(PublicizeService service) {
        if (isFinishing()) {
            return;
        }

        String tag = getString(R.string.fragment_tag_publicize_detail);
        Fragment detailFragment = PublicizeDetailFragment.newInstance(mSite, service);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(tag)
                .commit();
    }

    private PublicizeDetailFragment getDetailFragment() {
        String tag = getString(R.string.fragment_tag_publicize_detail);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
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

    private void showWebViewFragment(PublicizeService service, PublicizeConnection publicizeConnection) {
        if (isFinishing()) {
            return;
        }

        String tag = getString(R.string.fragment_tag_publicize_webview);
        Fragment webViewFragment = PublicizeWebViewFragment.newInstance(mSite, service, publicizeConnection);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, webViewFragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(tag)
                .commit();
    }

    private void closeWebViewFragment() {
        String tag = getString(R.string.fragment_tag_publicize_webview);
        getSupportFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * user tapped a service in the list fragment
     */
    @Override
    public void onServiceClicked(PublicizeService service) {
        showDetailFragment(service);
    }

    /*
     * user requested to connect to a service from the detail fragment
     */
    @Override
    public void onRequestConnect(PublicizeService service) {
        if (isFacebook(service)) {
            showFacebookWarning();
        } else {
            showWebViewFragment(service, null);
        }
    }

    /*
     * user requested to reconnect a broken publicizeConnection from the detail fragment
     */
    @Override
    public void onRequestReconnect(PublicizeService service, PublicizeConnection publicizeConnection) {
        if (isFacebook(service)) {
            showFacebookWarning();
        } else {
            PublicizeActions.reconnect(publicizeConnection);
            showWebViewFragment(service, null);
        }
    }

    /*
     * user requested to disconnect a service publicizeConnection from the detail fragment
     */
    @Override
    public void onRequestDisconnect(PublicizeConnection publicizeConnection) {
        confirmDisconnect(publicizeConnection);
    }

    private boolean isFacebook(PublicizeService service) {
        return service.getId().equals(PublicizeConstants.FACEBOOK_ID);
    }

    private String getConnectionsUrl(SiteModel site) {
        return WPCOM_CONNECTIONS_URL + SiteUtils.getHomeURLOrHostName(site);
    }

    /*
     * As of Oct 5, 2021 Facebook has deprecated support for authentication on embedded browsers, so Publicize
     * connections can't be established through web views anymore (ref: pbArwn-3uU-p2).
     * This method shows a temporary warning message to the user instead.
     */
    private void showFacebookWarning() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.sharing_facebook_warning_message)
                .setPositiveButton(R.string.sharing_facebook_warning_positive_button,
                        (dialog, id) -> ActivityLauncher.openUrlExternal(this, getConnectionsUrl(mSite)))
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .create()
                .show();
    }

    private void confirmDisconnect(final PublicizeConnection publicizeConnection) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(
                String.format(getString(R.string.dlg_confirm_publicize_disconnect), publicizeConnection.getLabel()));
        builder.setTitle(R.string.share_btn_disconnect);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.share_btn_disconnect, (dialog, id) -> {
            PublicizeActions.disconnect(publicizeConnection);
            // if the user disconnected from G+, return to the list fragment since the
            // detail fragment would give them the ability to reconnect
            if (publicizeConnection.getService().equals(PublicizeConstants.GOOGLE_PLUS_ID)) {
                returnToListFragment();
            } else {
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PublicizeEvents.ConnectionsChanged event) {
        reloadListFragment();
    }

    /*
     * request from fragment to connect/disconnect/reconnect completed
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PublicizeEvents.ActionCompleted event) {
        if (isFinishing()) {
            return;
        }

        if (event.getAction() != ConnectAction.RECONNECT) {
            closeWebViewFragment();
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            reloadDetailFragment();
        }

        if (event.didSucceed()) {
            Map<String, Object> analyticsProperties = new HashMap<>();
            analyticsProperties.put("service", event.getService());


            if (event.getAction() == ConnectAction.CONNECT) {
                AnalyticsUtils.trackWithSiteDetails(Stat.PUBLICIZE_SERVICE_CONNECTED, mSite, analyticsProperties);
            } else if (event.getAction() == ConnectAction.DISCONNECT) {
                AnalyticsUtils.trackWithSiteDetails(Stat.PUBLICIZE_SERVICE_DISCONNECTED, mSite, analyticsProperties);
            }
        } else {
            if (event.getReasonResId() != null) {
                DialogFragment fragment = PublicizeErrorDialogFragment.newInstance(event.getReasonResId());
                fragment.show(getSupportFragmentManager(), PublicizeErrorDialogFragment.TAG);
            } else {
                ToastUtils.showToast(this, R.string.error_generic);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PublicizeEvents.ActionAccountChosen event) {
        if (isFinishing()) {
            return;
        }

        PublicizeActions.connectStepTwo(event.getSiteId(), event.getKeychainId(),
                event.getService(), event.getExternalUserId());
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.connecting_account));
        mProgressDialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PublicizeEvents.ActionRequestChooseAccount event) {
        if (isFinishing()) {
            return;
        }

        closeWebViewFragment();

        SiteModel site = mSiteStore.getSiteBySiteId(event.getSiteId());
        if (site == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            return;
        }

        PublicizeAccountChooserDialogFragment dialogFragment = new PublicizeAccountChooserDialogFragment();
        Bundle args = new Bundle();
        args.putString(PublicizeConstants.ARG_CONNECTION_ARRAY_JSON, event.getJSONObject().toString());
        args.putSerializable(WordPress.SITE, site);
        args.putString(PublicizeConstants.ARG_SERVICE_ID, event.getServiceId());
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), PublicizeAccountChooserDialogFragment.TAG);
    }

    @Override
    public void onButtonPrefsClicked() {
        AnalyticsUtils.trackWithSiteDetails(Stat.OPENED_SHARING_BUTTON_MANAGEMENT, mSite);
        Fragment fragment = PublicizeButtonPrefsFragment.newInstance(mSite);
        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.fragment_container, fragment)
                                   .addToBackStack(null)
                                   .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                   .commit();
    }

    @Override
    public void onScrollableViewInitialized(int containerId) {
        AppBarLayoutExtensionsKt.setLiftOnScrollTargetViewIdAndRequestLayout(mAppBarLayout, containerId);
    }
}
