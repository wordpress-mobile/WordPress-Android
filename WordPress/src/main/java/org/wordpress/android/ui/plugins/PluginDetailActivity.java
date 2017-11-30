package org.wordpress.android.ui.plugins;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginUpdated;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginErrorType;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginVersionPayload;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import javax.inject.Inject;

public class PluginDetailActivity extends AppCompatActivity {
    public static final String KEY_PLUGIN_NAME = "KEY_PLUGIN_NAME";

    private SiteModel mSite;
    private PluginModel mPlugin;
    private PluginInfoModel mPluginInfo;

    private TextView mInstalledVersionTextView;
    private TextView mAvailableVersionTextView;
    private TextView mUpdateVersionTextView;
    private Switch mSwitchActive;
    private Switch mSwitchAutoupdates;

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
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
        }

        setupViews();
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

    private void setupViews() {
        mInstalledVersionTextView = findViewById(R.id.plugin_installed_version);
        mAvailableVersionTextView = findViewById(R.id.plugin_available_version);
        mUpdateVersionTextView = findViewById(R.id.plugin_btn_update);
        mSwitchActive = findViewById(R.id.plugin_state_active);
        mSwitchAutoupdates = findViewById(R.id.plugin_state_autoupdates);

        mSwitchActive.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isPressed()) {
                    mPlugin.setIsActive(b);
                    dispatchUpdateAction();
                }
            }
        });

        mSwitchAutoupdates.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isPressed()) {
                    mPlugin.setIsAutoUpdateEnabled(b);
                    dispatchUpdateAction();
                }
            }
        });

        mUpdateVersionTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isUpdateAvailable()) {
                    UpdateSitePluginVersionPayload payload = new UpdateSitePluginVersionPayload(mSite, mPlugin);
                    mDispatcher.dispatch(PluginActionBuilder.newUpdateSitePluginVersionAction(payload));
                }
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

        refreshViews();
    }

    private void refreshViews() {
        if (isFinishing()) {
            return;
        }
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

        if (!isUpdateAvailable()) {
            mAvailableVersionTextView.setVisibility(View.GONE);
            mUpdateVersionTextView.setVisibility(View.GONE);
        } else {
            mAvailableVersionTextView.setVisibility(View.VISIBLE);
            mAvailableVersionTextView.setText(getString(R.string.plugin_available_version,
                    mPluginInfo.getVersion()));
            mUpdateVersionTextView.setVisibility(View.VISIBLE);
        }
    }

    private void dispatchUpdateAction() {
        mDispatcher.dispatch(PluginActionBuilder.newUpdateSitePluginAction(
                new UpdateSitePluginPayload(mSite, mPlugin)));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginUpdated(OnSitePluginUpdated event) {
        if (event.isError()) {
            if (event.error.type == UpdateSitePluginErrorType.ACTIVATION_ERROR
                    || event.error.type == UpdateSitePluginErrorType.DEACTIVATION_ERROR) {
                // these errors are thrown when the plugin is already active and we try to activate it and vice versa.
                return;
            }
            ToastUtils.showToast(this, "An error occurred while fetching the plugins: "
                    + event.error.message);
            return;
        }
        mPlugin = mPluginStore.getSitePluginByName(mSite, mPlugin.getName());
        refreshViews();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginInfoChanged(PluginStore.OnPluginInfoChanged event) {
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
    public void onSitePluginVersionUpdated(PluginStore.OnSitePluginVersionUpdated event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "An error occurred while updating the plugin version with type: "
                    + event.error.type);
            return;
        }
        mPlugin = mPluginStore.getSitePluginByName(mSite, mPlugin.getName());
        refreshViews();
    }

    // Helpers

    private String getWpOrgPluginUrl() {
        return "https://wordpress.org/plugins/" + mPlugin.getSlug();
    }

    private boolean isUpdateAvailable() {
        return mPluginInfo != null && !TextUtils.isEmpty(mPluginInfo.getVersion())
                && !mPlugin.getVersion().equals(mPluginInfo.getVersion());
    }
}
