package org.wordpress.android.ui.plugins;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginUpdated;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginPayload;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import javax.inject.Inject;

public class PluginDetailActivity extends AppCompatActivity {
    public static final String KEY_PLUGIN_NAME = "KEY_PLUGIN_NAME";

    private SiteModel mSite;
    private PluginModel mPlugin;
    private PluginInfoModel mPluginInfo;

    private LinearLayout mContainer;
    private TextView mInstalledVersionTextView;
    private TextView mAvailableVersionTextView;
    private TextView mUpdateTextView;
    private ProgressBar mUpdateProgressBar;
    private Switch mSwitchActive;
    private Switch mSwitchAutoupdates;
    private ProgressDialog mRemovePluginProgressDialog;

    private boolean mIsConfiguringPlugin;
    private boolean mIsUpdatingPlugin;
    private boolean mIsRemovingPlugin;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);

        String pluginName;

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            pluginName = getIntent().getStringExtra(KEY_PLUGIN_NAME);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            pluginName = savedInstanceState.getString(KEY_PLUGIN_NAME);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, Duration.SHORT);
            finish();
            return;
        }

        mPlugin = mPluginStore.getSitePluginByName(mSite, pluginName);
        if (mPlugin == null) {
            ToastUtils.showToast(this, R.string.plugin_not_found, Duration.SHORT);
            finish();
            return;
        }

        mPluginInfo = PluginUtils.getPluginInfo(mPluginStore, mPlugin);
        if (mPluginInfo == null) {
            mDispatcher.dispatch(PluginActionBuilder.newFetchPluginInfoAction(mPlugin.getSlug()));
        }

        setContentView(R.layout.plugin_detail_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mPlugin.getDisplayName());
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
        }

        setupViews();
    }

    @Override
    protected void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putString(KEY_PLUGIN_NAME, mPlugin.getName());
    }

    // UI Helpers

    private void setupViews() {
        mContainer = findViewById(R.id.plugin_detail_container);
        mInstalledVersionTextView = findViewById(R.id.plugin_installed_version);
        mAvailableVersionTextView = findViewById(R.id.plugin_available_version);
        mUpdateTextView = findViewById(R.id.plugin_btn_update);
        mUpdateProgressBar = findViewById(R.id.plugin_update_progress_bar);
        mSwitchActive = findViewById(R.id.plugin_state_active);
        mSwitchAutoupdates = findViewById(R.id.plugin_state_autoupdates);

        mSwitchActive.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isPressed()) {
                    mPlugin.setIsActive(b);
                    dispatchConfigurePluginAction();
                }
            }
        });

        mSwitchAutoupdates.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isPressed()) {
                    mPlugin.setIsAutoUpdateEnabled(b);
                    dispatchConfigurePluginAction();
                }
            }
        });

        mUpdateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchUpdatePluginAction();
            }
        });

        findViewById(R.id.plugin_wp_org_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.openUrlExternal(PluginDetailActivity.this, getWpOrgPluginUrl());
            }
        });

        findViewById(R.id.plugin_home_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.openUrlExternal(PluginDetailActivity.this, mPlugin.getPluginUrl());
            }
        });

        Button removeBtn = findViewById(R.id.plugin_btn_remove);
        removeBtn.setVisibility(isRemovePluginFeatureAvailable() ? View.VISIBLE : View.GONE);
        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmRemovePlugin();
            }
        });

        refreshViews();
    }

    private void refreshViews() {
        mSwitchActive.setChecked(mPlugin.isActive());
        mSwitchAutoupdates.setChecked(mPlugin.isAutoUpdateEnabled());

        refreshPluginVersionViews();
    }

    private void refreshPluginVersionViews() {
        if (TextUtils.isEmpty(mPlugin.getVersion())) {
            mInstalledVersionTextView.setVisibility(View.GONE);
        } else {
            mInstalledVersionTextView.setVisibility(View.VISIBLE);
            mInstalledVersionTextView.setText(getString(R.string.plugin_installed_version,
                    mPlugin.getVersion()));
        }

        if (!PluginUtils.isUpdateAvailable(mPlugin, mPluginInfo)) {
            mAvailableVersionTextView.setVisibility(View.GONE);
        } else {
            mAvailableVersionTextView.setVisibility(View.VISIBLE);
            mAvailableVersionTextView.setText(getString(R.string.plugin_available_version,
                    mPluginInfo.getVersion()));
        }

        refreshUpdateVersionViews();
    }

    private void refreshUpdateVersionViews() {
        boolean isUpdateAvailable = PluginUtils.isUpdateAvailable(mPlugin, mPluginInfo);
        if (isUpdateAvailable && !mIsUpdatingPlugin) {
            mUpdateTextView.setVisibility(View.VISIBLE);
        } else {
            mUpdateTextView.setVisibility(View.GONE);
        }

        if (mIsUpdatingPlugin) {
            mUpdateProgressBar.setVisibility(View.VISIBLE);
        } else {
            mUpdateProgressBar.setVisibility(View.GONE);
        }
    }

    private void confirmRemovePlugin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Calypso_AlertDialog);
        builder.setTitle(getResources().getText(R.string.plugin_remove_dialog_title));
        String confirmationMessage = getString(R.string.plugin_remove_dialog_message,
                mPlugin.getDisplayName(),
                SiteUtils.getSiteNameOrHomeURL(mSite));
        builder.setMessage(confirmationMessage);
        builder.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                disableAndRemovePlugin();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    private void showSuccessfulUpdateSnackbar() {
        Snackbar.make(mContainer,
                getString(R.string.plugin_updated_successfully, mPlugin.getDisplayName()),
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void showUpdateFailedSnackbar() {
        Snackbar.make(mContainer,
                getString(R.string.plugin_updated_failed, mPlugin.getDisplayName()),
                Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dispatchUpdatePluginAction();
                    }
                })
                .show();
    }

    private void showPluginRemoveFailedSnackbar() {
        Snackbar.make(mContainer,
                getString(R.string.plugin_remove_failed, mPlugin.getDisplayName()),
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void showRemovePluginProgressDialog() {
        mRemovePluginProgressDialog = new ProgressDialog(this);
        mRemovePluginProgressDialog.setCancelable(false);
        mRemovePluginProgressDialog.setIndeterminate(true);
        // Even though we are deactivating the plugin to make sure it's disabled on the server side, since the user
        // sees that the plugin is disabled, it'd be confusing to say we are disabling the plugin
        String message = mPlugin.isActive()
                ? getString(R.string.plugin_disable_progress_dialog_message, mPlugin.getDisplayName())
                : getRemovingPluginMessage();
        mRemovePluginProgressDialog.setMessage(message);
        mRemovePluginProgressDialog.show();
    }

    private void cancelRemovePluginProgressDialog() {
        if (mRemovePluginProgressDialog != null && mRemovePluginProgressDialog.isShowing()) {
            mRemovePluginProgressDialog.cancel();
        }
    }

    // Network Helpers

    private void dispatchConfigurePluginAction() {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }
        if (mIsConfiguringPlugin) {
            return;
        }
        mIsConfiguringPlugin = true;
        mDispatcher.dispatch(PluginActionBuilder.newConfigureSitePluginAction(
                new ConfigureSitePluginPayload(mSite, mPlugin)));
    }

    private void dispatchUpdatePluginAction() {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }
        if (!PluginUtils.isUpdateAvailable(mPlugin, mPluginInfo) || mIsUpdatingPlugin) {
            return;
        }

        mIsUpdatingPlugin = true;
        refreshUpdateVersionViews();
        UpdateSitePluginPayload payload = new UpdateSitePluginPayload(mSite, mPlugin);
        mDispatcher.dispatch(PluginActionBuilder.newUpdateSitePluginAction(payload));
    }

    private void dispatchRemovePluginAction() {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }
        mRemovePluginProgressDialog.setMessage(getRemovingPluginMessage());
        DeleteSitePluginPayload payload = new DeleteSitePluginPayload(mSite, mPlugin);
        mDispatcher.dispatch(PluginActionBuilder.newDeleteSitePluginAction(payload));
    }

    private void disableAndRemovePlugin() {
        // This is only a sanity check as the remove button should not be visible. It's important to disable removing
        // plugins in certain cases, so we should still make this sanity check
        if (!isRemovePluginFeatureAvailable()) {
            return;
        }
        // We need to make sure that plugin is disabled before attempting to remove it
        mIsRemovingPlugin = true;
        showRemovePluginProgressDialog();
        mPlugin.setIsActive(false);
        dispatchConfigurePluginAction();
    }

    // FluxC callbacks

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginConfigured(OnSitePluginConfigured event) {
        if (isFinishing()) {
            return;
        }
        mIsConfiguringPlugin = false;
        if (event.isError()) {
            ToastUtils.showToast(this, getString(R.string.plugin_configuration_failed, event.error.message));
            if (mIsRemovingPlugin) {
                mIsRemovingPlugin = false;
                cancelRemovePluginProgressDialog();
                showPluginRemoveFailedSnackbar();
            }
            return;
        }

        PluginModel pluginFromDB = mPluginStore.getSitePluginByName(mSite, mPlugin.getName());
        if (pluginFromDB == null) {
            ToastUtils.showToast(this, R.string.plugin_not_found, Duration.SHORT);
            finish();
            return;
        }

        boolean isConfigurePluginActionNecessary = false;

        // The plugin state has been changed while a configuration network call is going on, we need to dispatch another
        // configure plugin action since we don't allow multiple configure actions to happen at the same time
        // This might happen either because user changed the state or a remove plugin action has started
        if (mPlugin.isActive() != pluginFromDB.isActive()
                || mPlugin.isAutoUpdateEnabled() != pluginFromDB.isAutoUpdateEnabled()) {
            // The plugin's state in UI has priority over the one in DB as we'll dispatch another configuration change
            // to make sure UI is reflected correctly in network and DB
            pluginFromDB.setIsActive(mPlugin.isActive());
            pluginFromDB.setIsAutoUpdateEnabled(mPlugin.isAutoUpdateEnabled());
            isConfigurePluginActionNecessary = true;
        }
        mPlugin = pluginFromDB;
        refreshViews();

        // We don't want to trigger the remove plugin action before configuration changes are reflected in network
        if (isConfigurePluginActionNecessary) {
            dispatchConfigurePluginAction();
        } else if (mIsRemovingPlugin && !mPlugin.isActive()) {
            // We need to check that plugin is disabled first because we might be dealing with a different callback
            // than what remove plugin has started
            dispatchRemovePluginAction();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginInfoChanged(PluginStore.OnPluginInfoChanged event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "An error occurred while fetching the plugin info with type: "
                    + event.error.type);
            return;
        }
        if (event.pluginInfo != null && mPlugin.getSlug().equals(event.pluginInfo.getSlug())) {
            mPluginInfo = event.pluginInfo;
            refreshPluginVersionViews();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginUpdated(OnSitePluginUpdated event) {
        if (isFinishing()) {
            return;
        }
        mIsUpdatingPlugin = false;
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "An error occurred while updating the plugin with type: "
                    + event.error.type);
            refreshPluginVersionViews();
            showUpdateFailedSnackbar();
            return;
        }
        mPlugin = mPluginStore.getSitePluginByName(mSite, mPlugin.getName());
        if (mPlugin == null) {
            ToastUtils.showToast(this, R.string.plugin_not_found, Duration.SHORT);
            finish();
            return;
        }

        refreshViews();
        showSuccessfulUpdateSnackbar();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginDeleted(OnSitePluginDeleted event) {
        if (isFinishing()) {
            return;
        }
        mIsRemovingPlugin = false;
        cancelRemovePluginProgressDialog();
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "An error occurred while removing the plugin with type: "
                    + event.error.type);
            String toastMessage = getString(R.string.plugin_updated_failed_detailed,
                    mPlugin.getDisplayName(), event.error.message);
            ToastUtils.showToast(this, toastMessage, Duration.LONG);
            return;
        }
        // Plugin removed we need to go back to the plugin list
        String toastMessage = getString(R.string.plugin_removed_successfully, mPlugin.getDisplayName());
        ToastUtils.showToast(this, toastMessage, Duration.LONG);
        finish();
    }

    // Utils

    private String getWpOrgPluginUrl() {
        return "https://wordpress.org/plugins/" + mPlugin.getSlug();
    }

    private String getRemovingPluginMessage() {
        return getString(R.string.plugin_remove_progress_dialog_message, mPlugin.getDisplayName());
    }

    private boolean isRemovePluginFeatureAvailable() {
        String pluginName = mPlugin.getName();
        // Disable removing jetpack as the site will stop working in the client
        if (pluginName.equals("jetpack/jetpack")) {
            return false;
        }
        // Disable removing akismet and vaultpress for AT sites
        return !mSite.isAutomatedTransfer()
                || (!pluginName.equals("akismet/akismet") && !pluginName.equals("vaultpress/vaultpress"));
    }
}
