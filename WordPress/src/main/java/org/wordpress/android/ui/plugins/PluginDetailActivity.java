package org.wordpress.android.ui.plugins;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.OnPluginChanged;
import org.wordpress.android.fluxc.store.PluginStore.UpdatePluginErrorType;
import org.wordpress.android.fluxc.store.PluginStore.UpdatePluginPayload;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import javax.inject.Inject;

public class PluginDetailActivity extends AppCompatActivity {
    public static final String KEY_PLUGIN_NAME = "KEY_PLUGIN_NAME";

    private SiteModel mSite;
    private PluginModel mPlugin;

    private Switch mSwitchActive;
    private Switch mSwitchAutoupdates;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);

        setContentView(R.layout.plugin_detail_activity);

        String pluginName;

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            pluginName = getIntent().getStringExtra(KEY_PLUGIN_NAME);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            pluginName = savedInstanceState.getString(KEY_PLUGIN_NAME);
        }

        mPlugin = mPluginStore.getPluginByName(mSite, pluginName);

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, Duration.SHORT);
            finish();
            return;
        }

        if (mPlugin == null) {
            ToastUtils.showToast(this, R.string.plugin_not_found, Duration.SHORT);
            finish();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
        mSwitchActive = (Switch) findViewById(R.id.plugin_state_active);
        mSwitchAutoupdates = (Switch) findViewById(R.id.plugin_state_autoupdates);

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

        refreshStates();
    }

    private void refreshStates() {
        mSwitchActive.setChecked(mPlugin.isActive());
        mSwitchAutoupdates.setChecked(mPlugin.isAutoUpdateEnabled());
    }

    private void dispatchUpdateAction() {
        mDispatcher.dispatch(PluginActionBuilder.newUpdatePluginAction(
                new UpdatePluginPayload(mSite, mPlugin)));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginChanged(OnPluginChanged event) {
        if (event.isError()) {
            if (event.error.type == UpdatePluginErrorType.ACTIVATION_ERROR
                    || event.error.type == UpdatePluginErrorType.DEACTIVATION_ERROR) {
                // these errors are thrown when the plugin is already active and we try to activate it and vice versa.
                return;
            }
            ToastUtils.showToast(this, "An error occurred while fetching the plugins: " + event.error.message);
            return;
        }
        if (event.plugin != null) {
            mPlugin = event.plugin;
            refreshStates();
        }
    }
}
