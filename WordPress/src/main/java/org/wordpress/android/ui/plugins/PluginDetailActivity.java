package org.wordpress.android.ui.plugins;

import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitePluginModel;
import org.wordpress.android.fluxc.model.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginUpdated;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginPayload;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.widgets.WPNetworkImageView;

import javax.inject.Inject;

import static org.wordpress.android.widgets.WPNetworkImageView.ImageType.PLUGIN_ICON;

public class PluginDetailActivity extends AppCompatActivity {
    public static final String KEY_PLUGIN_NAME = "KEY_PLUGIN_NAME";
    private static final String KEY_IS_CONFIGURING_PLUGIN = "KEY_IS_CONFIGURING_PLUGIN";
    private static final String KEY_IS_UPDATING_PLUGIN = "KEY_IS_UPDATING_PLUGIN";
    private static final String KEY_IS_REMOVING_PLUGIN = "KEY_IS_REMOVING_PLUGIN";
    private static final String KEY_IS_ACTIVE = "KEY_IS_ACTIVE";
    private static final String KEY_IS_AUTO_UPDATE_ENABLED = "KEY_IS_AUTO_UPDATE_ENABLED";
    private static final String KEY_IS_SHOWING_REMOVE_PLUGIN_CONFIRMATION_DIALOG
            = "KEY_IS_SHOWING_REMOVE_PLUGIN_CONFIRMATION_DIALOG";

    private SiteModel mSite;
    private SitePluginModel mSitePlugin;
    private WPOrgPluginModel mWPOrgPlugin;

    private ScrollView mContainer;
    private TextView mTitleTextView;
    private TextView mByLineTextView;
    private TextView mVersionTopTextView;
    private TextView mVersionBottomTextView;
    private TextView mUpdateTextView;
    private TextView mDescriptionTextView;
    private ImageView mDescriptionChevron;
    private ProgressBar mUpdateProgressBar;
    private Switch mSwitchActive;
    private Switch mSwitchAutoupdates;
    private ProgressDialog mRemovePluginProgressDialog;

    private WPNetworkImageView mImageBanner;
    private WPNetworkImageView mImageIcon;

    private boolean mIsConfiguringPlugin;
    private boolean mIsUpdatingPlugin;
    private boolean mIsRemovingPlugin;
    private boolean mIsShowingRemovePluginConfirmationDialog;

    // These flags reflects the UI state
    private boolean mIsActive;
    private boolean mIsAutoUpdateEnabled;

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

        mSitePlugin = mPluginStore.getSitePluginByName(mSite, pluginName);
        if (mSitePlugin == null) {
            ToastUtils.showToast(this, R.string.plugin_not_found, Duration.SHORT);
            finish();
            return;
        }

        if (savedInstanceState == null) {
            mIsActive = mSitePlugin.isActive();
            mIsAutoUpdateEnabled = mSitePlugin.isAutoUpdateEnabled();

            // Refresh the plugin information to check if there is a newer version
            mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(mSitePlugin.getSlug()));
        } else {
            mIsConfiguringPlugin = savedInstanceState.getBoolean(KEY_IS_CONFIGURING_PLUGIN);
            mIsUpdatingPlugin = savedInstanceState.getBoolean(KEY_IS_UPDATING_PLUGIN);
            mIsRemovingPlugin = savedInstanceState.getBoolean(KEY_IS_REMOVING_PLUGIN);
            mIsActive = savedInstanceState.getBoolean(KEY_IS_ACTIVE);
            mIsAutoUpdateEnabled = savedInstanceState.getBoolean(KEY_IS_AUTO_UPDATE_ENABLED);
            mIsShowingRemovePluginConfirmationDialog =
                    savedInstanceState.getBoolean(KEY_IS_SHOWING_REMOVE_PLUGIN_CONFIRMATION_DIALOG);
        }

        mWPOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, mSitePlugin);

        setContentView(R.layout.plugin_detail_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(null);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
        }

        setupViews();

        // Show remove plugin confirmation dialog if it's dismissed while activity is re-created
        if (mIsShowingRemovePluginConfirmationDialog) {
            confirmRemovePlugin();
        }

        // Show remove plugin progress dialog if it's dismissed while activity is re-created
        if (mIsRemovingPlugin) {
            showRemovePluginProgressDialog();
        }
    }

    @Override
    protected void onDestroy() {
        // Even though the progress dialog will be destroyed, when it's re-created sometimes the spinner
        // would get stuck. This seems to be helping with that.
        if (mRemovePluginProgressDialog != null && mRemovePluginProgressDialog.isShowing()) {
            mRemovePluginProgressDialog.cancel();
        }
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.plugin_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean showTrash = canPluginBeDisabledOrRemoved();
        menu.findItem(R.id.menu_trash).setVisible(showTrash);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (isPluginStateChangedSinceLastConfigurationDispatch()) {
                // It looks like we have some unsaved changes, we need to force a configuration dispatch since the
                // user is leaving the page
                dispatchConfigurePluginAction(true);
            }
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_trash) {
            confirmRemovePlugin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putString(KEY_PLUGIN_NAME, mSitePlugin.getName());
        outState.putBoolean(KEY_IS_CONFIGURING_PLUGIN, mIsConfiguringPlugin);
        outState.putBoolean(KEY_IS_UPDATING_PLUGIN, mIsUpdatingPlugin);
        outState.putBoolean(KEY_IS_REMOVING_PLUGIN, mIsRemovingPlugin);
        outState.putBoolean(KEY_IS_ACTIVE, mIsActive);
        outState.putBoolean(KEY_IS_AUTO_UPDATE_ENABLED, mIsAutoUpdateEnabled);
        outState.putBoolean(KEY_IS_SHOWING_REMOVE_PLUGIN_CONFIRMATION_DIALOG, mIsShowingRemovePluginConfirmationDialog);
    }

    // UI Helpers

    private void setupViews() {
        mContainer = findViewById(R.id.plugin_detail_container);
        mTitleTextView = findViewById(R.id.text_title);
        mByLineTextView = findViewById(R.id.text_byline);
        mVersionTopTextView = findViewById(R.id.plugin_version_top);
        mVersionBottomTextView = findViewById(R.id.plugin_version_bottom);
        mUpdateTextView = findViewById(R.id.plugin_btn_update);
        mUpdateProgressBar = findViewById(R.id.plugin_update_progress_bar);
        mSwitchActive = findViewById(R.id.plugin_state_active);
        mSwitchAutoupdates = findViewById(R.id.plugin_state_autoupdates);

        mImageBanner = findViewById(R.id.image_banner);
        mImageIcon = findViewById(R.id.image_icon);

        mDescriptionTextView = findViewById(R.id.plugin_description);
        mDescriptionChevron = findViewById(R.id.plugin_description_chevron);
        findViewById(R.id.plugin_description_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleText(mDescriptionTextView, mDescriptionChevron);
            }
        });

        mSwitchActive.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isPressed()) {
                    mIsActive = b;
                    dispatchConfigurePluginAction(false);
                }
            }
        });

        mSwitchAutoupdates.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isPressed()) {
                    mIsAutoUpdateEnabled = b;
                    dispatchConfigurePluginAction(false);
                }
            }
        });

        mUpdateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchUpdatePluginAction();
            }
        });

        View settingsView = findViewById(R.id.plugin_settings_page);
        if (canShowSettings()) {
            settingsView.setVisibility(View.VISIBLE);
            settingsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO
                }
            });
        } else {
            settingsView.setVisibility(View.GONE);
        }

        findViewById(R.id.plugin_wp_org_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.openUrlExternal(PluginDetailActivity.this, getWpOrgPluginUrl());
            }
        });

        findViewById(R.id.plugin_home_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.openUrlExternal(PluginDetailActivity.this, mSitePlugin.getPluginUrl());
            }
        });

        refreshViews();
    }

    private void refreshViews() {
        mTitleTextView.setText(mSitePlugin.getDisplayName());

        if (TextUtils.isEmpty(mSitePlugin.getAuthorUrl())) {
            mByLineTextView.setText(String.format(getString(R.string.plugin_byline), mSitePlugin.getAuthorName()));
        } else {
            String authorLink = "<a href='" + mSitePlugin.getAuthorUrl() + "'>" + mSitePlugin.getAuthorName() + "</a>";
            String byline = String.format(getString(R.string.plugin_byline), authorLink);
            mByLineTextView.setMovementMethod(WPLinkMovementMethod.getInstance());
            mByLineTextView.setText(Html.fromHtml(byline));
        }

        mSwitchActive.setChecked(mIsActive);
        mSwitchAutoupdates.setChecked(mIsAutoUpdateEnabled);

        mImageIcon.setImageUrl(mWPOrgPlugin.getIcon(), PLUGIN_ICON);
        // TODO mDescriptionTextView.setText(Html.fromHtml(mWPOrgPlugin.getDescription()));
        mDescriptionTextView.setText("sakjha kdhasjdhd akjdhkja dghjhadghajhsdg jahsdg ajdgjahdg jahsdg ahdgaj sgdja");

        refreshPluginVersionViews();
    }

    private void refreshPluginVersionViews() {
        String pluginVersion = TextUtils.isEmpty(mSitePlugin.getVersion()) ? "?" : mSitePlugin.getVersion();
        String installedVersion;

        if (PluginUtils.isUpdateAvailable(mSitePlugin, mWPOrgPlugin)) {
            installedVersion = String.format(getString(R.string.plugin_installed_version), pluginVersion);
            String availableVersion = String.format(getString(R.string.plugin_available_version), mWPOrgPlugin.getVersion());
            mVersionTopTextView.setText(availableVersion);
            mVersionBottomTextView.setText(installedVersion);
            mVersionBottomTextView.setVisibility(View.VISIBLE);
        } else {
            installedVersion = String.format(getString(R.string.plugin_version), pluginVersion);
            mVersionTopTextView.setText(installedVersion);
            mVersionBottomTextView.setVisibility(View.GONE);
        }

        refreshUpdateVersionViews();
    }

    private void refreshUpdateVersionViews() {
        boolean isUpdateAvailable = PluginUtils.isUpdateAvailable(mSitePlugin, mWPOrgPlugin);
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

    private void toggleText(@NonNull final TextView textView, @NonNull ImageView chevron) {
        AniUtils.Duration duration = AniUtils.Duration.SHORT;
        boolean isExpanded = textView.getVisibility() == View.VISIBLE;
        if (isExpanded) {
            AniUtils.fadeOut(textView, duration);
        } else {
            AniUtils.fadeIn(textView, duration);
        }

        float endRotate = isExpanded ? 360f : -180f;
        ObjectAnimator animRotate = ObjectAnimator.ofFloat(chevron, View.ROTATION, 0f, endRotate);
        animRotate.setDuration(duration.toMillis(this));
        animRotate.start();
    }

    private void confirmRemovePlugin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Calypso_AlertDialog);
        builder.setTitle(getResources().getText(R.string.plugin_remove_dialog_title));
        String confirmationMessage = getString(R.string.plugin_remove_dialog_message,
                mSitePlugin.getDisplayName(),
                SiteUtils.getSiteNameOrHomeURL(mSite));
        builder.setMessage(confirmationMessage);
        builder.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mIsShowingRemovePluginConfirmationDialog = false;
                disableAndRemovePlugin();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mIsShowingRemovePluginConfirmationDialog = false;
            }
        });
        builder.setCancelable(true);
        builder.create();
        mIsShowingRemovePluginConfirmationDialog = true;
        builder.show();
    }

    private void showSuccessfulUpdateSnackbar() {
        Snackbar.make(mContainer,
                getString(R.string.plugin_updated_successfully, mSitePlugin.getDisplayName()),
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void showUpdateFailedSnackbar() {
        Snackbar.make(mContainer,
                getString(R.string.plugin_updated_failed, mSitePlugin.getDisplayName()),
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
                getString(R.string.plugin_remove_failed, mSitePlugin.getDisplayName()),
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void showRemovePluginProgressDialog() {
        mRemovePluginProgressDialog = new ProgressDialog(this);
        mRemovePluginProgressDialog.setCancelable(false);
        mRemovePluginProgressDialog.setIndeterminate(true);
        // Even though we are deactivating the plugin to make sure it's disabled on the server side, since the user
        // sees that the plugin is disabled, it'd be confusing to say we are disabling the plugin
        String message = mIsActive
                ? getString(R.string.plugin_disable_progress_dialog_message, mSitePlugin.getDisplayName())
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

    private void dispatchConfigurePluginAction(boolean forceUpdate) {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }
        if (!forceUpdate && mIsConfiguringPlugin) {
            return;
        }
        mIsConfiguringPlugin = true;
        mSitePlugin.setIsActive(mIsActive);
        mSitePlugin.setIsAutoUpdateEnabled(mIsAutoUpdateEnabled);
        mDispatcher.dispatch(PluginActionBuilder.newConfigureSitePluginAction(
                new ConfigureSitePluginPayload(mSite, mSitePlugin)));
    }

    private void dispatchUpdatePluginAction() {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }
        if (!PluginUtils.isUpdateAvailable(mSitePlugin, mWPOrgPlugin) || mIsUpdatingPlugin) {
            return;
        }

        mIsUpdatingPlugin = true;
        refreshUpdateVersionViews();
        UpdateSitePluginPayload payload = new UpdateSitePluginPayload(mSite, mSitePlugin);
        mDispatcher.dispatch(PluginActionBuilder.newUpdateSitePluginAction(payload));
    }

    private void dispatchRemovePluginAction() {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }
        mRemovePluginProgressDialog.setMessage(getRemovingPluginMessage());
        DeleteSitePluginPayload payload = new DeleteSitePluginPayload(mSite, mSitePlugin);
        mDispatcher.dispatch(PluginActionBuilder.newDeleteSitePluginAction(payload));
    }

    private void disableAndRemovePlugin() {
        // This is only a sanity check as the remove button should not be visible. It's important to disable removing
        // plugins in certain cases, so we should still make this sanity check
        if (!canPluginBeDisabledOrRemoved()) {
            return;
        }
        // We need to make sure that plugin is disabled before attempting to remove it
        mIsRemovingPlugin = true;
        showRemovePluginProgressDialog();
        mIsActive = false;
        dispatchConfigurePluginAction(false);
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
            // The plugin was already removed in remote, there is no need to show an error to the user
            if (mIsRemovingPlugin &&
                    event.error.type == PluginStore.ConfigureSitePluginErrorType.UNKNOWN_PLUGIN) {
                // We still need to dispatch the remove plugin action to remove the local copy
                // and complete the flow gracefully
                // We can ignore `!mSitePlugin.isActive()` check here since the plugin is not installed anymore on remote
                dispatchRemovePluginAction();
                return;
            }

            ToastUtils.showToast(this, getString(R.string.plugin_configuration_failed, event.error.message));

            // Refresh the UI to plugin's last known state
            if (refreshPluginFromStoreAndCheckForNull()) {
                return;
            }
            mIsActive = mSitePlugin.isActive();
            mIsAutoUpdateEnabled = mSitePlugin.isAutoUpdateEnabled();
            refreshViews();

            if (mIsRemovingPlugin) {
                mIsRemovingPlugin = false;
                cancelRemovePluginProgressDialog();
                showPluginRemoveFailedSnackbar();
            }
            return;
        }

        if (refreshPluginFromStoreAndCheckForNull()) {
            return;
        }

        // The plugin state has been changed while a configuration network call is going on, we need to dispatch another
        // configure plugin action since we don't allow multiple configure actions to happen at the same time
        // This might happen either because user changed the state or a remove plugin action has started
        if (isPluginStateChangedSinceLastConfigurationDispatch()) {
            // The plugin's state in UI has priority over the one in DB as we'll dispatch another configuration change
            // to make sure UI is reflected correctly in network and DB
            dispatchConfigurePluginAction(false);
        } else if (mIsRemovingPlugin && !mSitePlugin.isActive()) {
            // We don't want to trigger the remove plugin action before configuration changes are reflected in network
            dispatchRemovePluginAction();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWPOrgPluginFetched(PluginStore.OnWPOrgPluginFetched event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "An error occurred while fetching wporg plugin with type: "
                    + event.error.type);
            return;
        }
        if (!TextUtils.isEmpty(mSitePlugin.getSlug()) && mSitePlugin.getSlug().equals(event.pluginSlug)) {
            mWPOrgPlugin = mPluginStore.getWPOrgPluginBySlug(event.pluginSlug);
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
        if (refreshPluginFromStoreAndCheckForNull()) {
            return;
        }

        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.PLUGIN_UPDATED, mSite);
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
                    mSitePlugin.getDisplayName(), event.error.message);
            ToastUtils.showToast(this, toastMessage, Duration.LONG);
            return;
        }
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.PLUGIN_REMOVED, mSite);

        // Plugin removed we need to go back to the plugin list
        String toastMessage = getString(R.string.plugin_removed_successfully, mSitePlugin.getDisplayName());
        ToastUtils.showToast(this, toastMessage, Duration.LONG);
        finish();
    }

    // Utils

    private String getWpOrgPluginUrl() {
        return "https://wordpress.org/plugins/" + mSitePlugin.getSlug();
    }

    private String getWpOrgReviewsUrl() {
        return "https://wordpress.org/plugins/" + mSitePlugin.getSlug() + "/reviews/";
    }

    private String getRemovingPluginMessage() {
        return getString(R.string.plugin_remove_progress_dialog_message, mSitePlugin.getDisplayName());
    }

    private boolean canPluginBeDisabledOrRemoved() {
        String pluginName = mSitePlugin.getName();
        // Disable removing jetpack as the site will stop working in the client
        if (pluginName.equals("jetpack/jetpack")) {
            return false;
        }
        // Disable removing akismet and vaultpress for AT sites
        return !mSite.isAutomatedTransfer()
                || (!pluginName.equals("akismet/akismet") && !pluginName.equals("vaultpress/vaultpress"));
    }

    // only show settings for active plugins on .org sites
    private boolean canShowSettings() {
        // TODO: also verify that there *is* a settings URL
        return mSitePlugin.isActive() && !mSite.isJetpackConnected();
    }

    private boolean isPluginStateChangedSinceLastConfigurationDispatch() {
        return mSitePlugin.isActive() != mIsActive || mSitePlugin.isAutoUpdateEnabled() != mIsAutoUpdateEnabled;
    }

    private boolean refreshPluginFromStoreAndCheckForNull() {
        mSitePlugin = mPluginStore.getSitePluginByName(mSite, mSitePlugin.getName());
        if (mSitePlugin == null) {
            ToastUtils.showToast(this, R.string.plugin_not_found);
            finish();
            return true;
        }
        return false;
    }
}
