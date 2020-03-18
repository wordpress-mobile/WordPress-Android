package org.wordpress.android.ui.plugins;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.ElevationOverlayProvider;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectoryFetched;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled;
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginUpdated;
import org.wordpress.android.fluxc.store.PluginStore.OnWPOrgPluginFetched;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.InitiateAutomatedTransferPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferEligibilityChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferInitiated;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferStatusChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose;
import org.wordpress.android.ui.domains.DomainRegistrationResultFragment;
import org.wordpress.android.ui.posts.BasicFragmentDialog;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;
import org.wordpress.android.widgets.WPSnackbar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;

import static org.wordpress.android.ui.plans.PlanUtilsKt.isDomainCreditAvailable;
import static org.wordpress.android.util.DomainRegistrationUtilsKt.requestEmailValidation;

public class PluginDetailActivity extends LocaleAwareActivity implements OnDomainRegistrationRequestedListener,
        BasicDialogPositiveClickInterface {
    public static final String KEY_PLUGIN_SLUG = "KEY_PLUGIN_SLUG";
    private static final String KEY_IS_CONFIGURING_PLUGIN = "KEY_IS_CONFIGURING_PLUGIN";
    private static final String KEY_IS_INSTALLING_PLUGIN = "KEY_IS_INSTALLING_PLUGIN";
    private static final String KEY_IS_UPDATING_PLUGIN = "KEY_IS_UPDATING_PLUGIN";
    private static final String KEY_IS_REMOVING_PLUGIN = "KEY_IS_REMOVING_PLUGIN";
    private static final String KEY_IS_ACTIVE = "KEY_IS_ACTIVE";
    private static final String KEY_IS_AUTO_UPDATE_ENABLED = "KEY_IS_AUTO_UPDATE_ENABLED";
    private static final String KEY_IS_SHOWING_REMOVE_PLUGIN_CONFIRMATION_DIALOG
            = "KEY_IS_SHOWING_REMOVE_PLUGIN_CONFIRMATION_DIALOG";
    private static final String KEY_IS_SHOWING_INSTALL_FIRST_PLUGIN_CONFIRMATION_DIALOG
            = "KEY_IS_SHOWING_INSTALL_FIRST_PLUGIN_CONFIRMATION_DIALOG";
    private static final String KEY_IS_SHOWING_AUTOMATED_TRANSFER_PROGRESS
            = "KEY_IS_SHOWING_AUTOMATED_TRANSFER_PROGRESS";
    private static final String KEY_IS_SHOWING_DOMAIN_CREDIT_CHECK_PROGRESS
            = "KEY_IS_SHOWING_DOMAIN_CREDIT_CHECK_PROGRESS";
    private static final String KEY_PLUGIN_RECHECKED_TIMES = "KEY_PLUGIN_RECHECKED_TIMES";
    private static final String TAG_ERROR_DIALOG = "ERROR_DIALOG";

    private static final int MAX_PLUGIN_CHECK_TRIES = 10;
    private static final int DEFAULT_RETRY_DELAY_MS = 3000;
    private static final int PLUGIN_RETRY_DELAY_MS = 10000;

    private SiteModel mSite;
    private String mSlug;
    protected ImmutablePluginModel mPlugin;
    private Handler mHandler;

    private ViewGroup mContainer;
    private TextView mTitleTextView;
    private TextView mByLineTextView;
    private TextView mVersionTopTextView;
    private TextView mVersionBottomTextView;
    private TextView mInstalledText;
    private AppCompatButton mUpdateButton;
    private AppCompatButton mInstallButton;
    private SwitchCompat mSwitchActive;
    private SwitchCompat mSwitchAutoupdates;
    private ProgressDialog mRemovePluginProgressDialog;
    private ProgressDialog mAutomatedTransferProgressDialog;
    private ProgressDialog mCheckingDomainCreditsProgressDialog;

    private CardView mWPOrgPluginDetailsContainer;
    private RelativeLayout mRatingsSectionContainer;

    protected TextView mDescriptionTextView;
    protected ImageView mDescriptionChevron;
    protected TextView mInstallationTextView;
    protected ImageView mInstallationChevron;
    protected TextView mWhatsNewTextView;
    protected ImageView mWhatsNewChevron;
    protected TextView mFaqTextView;
    protected ImageView mFaqChevron;

    private ImageView mImageBanner;
    private ImageView mImageIcon;

    private boolean mIsConfiguringPlugin;
    private boolean mIsInstallingPlugin;
    private boolean mIsUpdatingPlugin;
    private boolean mIsRemovingPlugin;
    protected boolean mIsShowingRemovePluginConfirmationDialog;
    protected boolean mIsShowingInstallFirstPluginConfirmationDialog;
    protected boolean mIsShowingAutomatedTransferProgress;

    private int mPluginReCheckTimer = 0;

    // These flags reflects the UI state
    protected boolean mIsActive;
    protected boolean mIsAutoUpdateEnabled;

    @Inject PluginStore mPluginStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;
    @Inject ImageManager mImageManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mSlug = getIntent().getStringExtra(KEY_PLUGIN_SLUG);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mSlug = savedInstanceState.getString(KEY_PLUGIN_SLUG);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found);
            finish();
            return;
        }

        refreshPluginFromStore();

        if (mPlugin == null) {
            ToastUtils.showToast(this, R.string.plugin_not_found);
            finish();
            return;
        }

        boolean isShowingDomainCreditCheckProgress = false;

        if (savedInstanceState == null) {
            mIsActive = mPlugin.isActive();
            mIsAutoUpdateEnabled = mPlugin.isAutoUpdateEnabled();
            // Refresh the wporg plugin which should also fetch fields such as descriptionAsHtml if it's missing
            mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(mSlug));
        } else {
            mIsConfiguringPlugin = savedInstanceState.getBoolean(KEY_IS_CONFIGURING_PLUGIN);
            mIsInstallingPlugin = savedInstanceState.getBoolean(KEY_IS_INSTALLING_PLUGIN);
            mIsUpdatingPlugin = savedInstanceState.getBoolean(KEY_IS_UPDATING_PLUGIN);
            mIsRemovingPlugin = savedInstanceState.getBoolean(KEY_IS_REMOVING_PLUGIN);
            mIsActive = savedInstanceState.getBoolean(KEY_IS_ACTIVE);
            mIsAutoUpdateEnabled = savedInstanceState.getBoolean(KEY_IS_AUTO_UPDATE_ENABLED);
            mIsShowingRemovePluginConfirmationDialog =
                    savedInstanceState.getBoolean(KEY_IS_SHOWING_REMOVE_PLUGIN_CONFIRMATION_DIALOG);
            mIsShowingInstallFirstPluginConfirmationDialog = savedInstanceState
                    .getBoolean(KEY_IS_SHOWING_INSTALL_FIRST_PLUGIN_CONFIRMATION_DIALOG);
            mIsShowingAutomatedTransferProgress = savedInstanceState
                    .getBoolean(KEY_IS_SHOWING_AUTOMATED_TRANSFER_PROGRESS);
            isShowingDomainCreditCheckProgress = savedInstanceState
                    .getBoolean(KEY_IS_SHOWING_DOMAIN_CREDIT_CHECK_PROGRESS);
            mPluginReCheckTimer = savedInstanceState.getInt(KEY_PLUGIN_RECHECKED_TIMES, 0);
        }

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


        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(this);

        float appbarElevation = getResources().getDimension(R.dimen.appbar_elevation);
        int elevatedColor = elevationOverlayProvider
                .compositeOverlayIfNeeded(ContextExtensionsKt.getColorFromAttribute(this, R.attr.wpColorAppBar),
                        appbarElevation);
        collapsingToolbarLayout.setContentScrimColor(elevatedColor);

        mHandler = new Handler();
        setupViews();

        if (mIsShowingRemovePluginConfirmationDialog) {
            // Show remove plugin confirmation dialog if it's dismissed while activity is re-created
            confirmRemovePlugin();
        } else if (mIsRemovingPlugin) {
            // Show remove plugin progress dialog if it's dismissed while activity is re-created
            showRemovePluginProgressDialog();
        }

        if (mIsShowingInstallFirstPluginConfirmationDialog) {
            confirmInstallPluginForAutomatedTransfer();
        }

        if (isShowingDomainCreditCheckProgress) {
            showDomainCreditsCheckProgressDialog();
        }

        if (mIsShowingAutomatedTransferProgress) {
            showAutomatedTransferProgressDialog();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlansFetched(OnPlansFetched event) {
        if (mCheckingDomainCreditsProgressDialog == null || !mCheckingDomainCreditsProgressDialog.isShowing()) {
            AppLog.w(T.PLANS, "User cancelled domain credit checking. Ignoring the result.");
            return;
        }

        cancelDomainCreditsCheckProgressDialog();
        if (event.isError()) {
            AppLog.e(T.PLANS, PluginDetailActivity.class.getSimpleName() + ".onPlansFetched: "
                              + event.error.type + " - " + event.error.message);
            WPSnackbar.make(mContainer, getString(R.string.plugin_check_domain_credit_error), Snackbar.LENGTH_LONG)
                      .show();
        } else {
            // This should not happen
            if (event.plans == null) {
                String errorMessage = "Failed to fetch user Plans. The result is null.";
                if (BuildConfig.DEBUG) {
                    throw new IllegalStateException(errorMessage);
                }
                WPSnackbar.make(mContainer, getString(R.string.plugin_check_domain_credit_error), Snackbar.LENGTH_LONG)
                          .show();
                AppLog.e(T.PLANS, errorMessage);
                return;
            }

            if (isDomainCreditAvailable(event.plans)) {
                showDomainRegistrationDialog();
            } else {
                dispatchInstallPluginAction();
            }
        }
    }

    @Override
    public void onDomainRegistrationRequested() {
        ActivityLauncher.viewDomainRegistrationActivityForResult(this, mSite,
                DomainRegistrationPurpose.AUTOMATED_TRANSFER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.DOMAIN_REGISTRATION) {
            if (resultCode != Activity.RESULT_OK || isFinishing()) {
                return;
            }
            if (data != null) {
                String email = data.getStringExtra(DomainRegistrationResultFragment.RESULT_REGISTERED_DOMAIN_EMAIL);
                requestEmailValidation(this, email);
            }
            AnalyticsTracker.track(Stat.AUTOMATED_TRANSFER_CUSTOM_DOMAIN_PURCHASED);
            confirmInstallPluginForAutomatedTransfer();
        }
    }

    @Override
    public void onPositiveClicked(@NotNull String instanceTag) {
        // do nothing
    }

    public static class DomainRegistrationPromptDialog extends DialogFragment {
        static final String DOMAIN_REGISTRATION_PROMPT_DIALOG_TAG = "DOMAIN_REGISTRATION_PROMPT_DIALOG";

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
            builder.setTitle(R.string.plugin_install_custom_domain_required_dialog_title);
            builder.setMessage(R.string.plugin_install_custom_domain_required_dialog_message);
            builder.setPositiveButton(R.string.plugin_install_custom_domain_required_dialog_register_btn,
                    (dialogInterface, i) -> {
                        if (isAdded() && getActivity() instanceof OnDomainRegistrationRequestedListener) {
                            ((OnDomainRegistrationRequestedListener) getActivity()).onDomainRegistrationRequested();
                        }
                    });
            builder.setNegativeButton(R.string.cancel,
                    (dialogInterface, i) -> {
                    });

            builder.setCancelable(true);
            return builder.create();
        }
    }

    private void showDomainRegistrationDialog() {
        DialogFragment dialogFragment = new DomainRegistrationPromptDialog();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialogFragment, DomainRegistrationPromptDialog.DOMAIN_REGISTRATION_PROMPT_DIALOG_TAG);
        ft.commitAllowingStateLoss();
    }

    @Override
    protected void onDestroy() {
        // Even though the progress dialog will be destroyed, when it's re-created sometimes the spinner
        // would get stuck. This seems to be helping with that.
        cancelRemovePluginProgressDialog();

        cancelDomainCreditsCheckProgressDialog();

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
            if (NetworkUtils.checkConnection(this)) {
                confirmRemovePlugin();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putString(KEY_PLUGIN_SLUG, mSlug);
        outState.putBoolean(KEY_IS_CONFIGURING_PLUGIN, mIsConfiguringPlugin);
        outState.putBoolean(KEY_IS_INSTALLING_PLUGIN, mIsInstallingPlugin);
        outState.putBoolean(KEY_IS_UPDATING_PLUGIN, mIsUpdatingPlugin);
        outState.putBoolean(KEY_IS_REMOVING_PLUGIN, mIsRemovingPlugin);
        outState.putBoolean(KEY_IS_ACTIVE, mIsActive);
        outState.putBoolean(KEY_IS_AUTO_UPDATE_ENABLED, mIsAutoUpdateEnabled);
        outState.putBoolean(KEY_IS_SHOWING_REMOVE_PLUGIN_CONFIRMATION_DIALOG, mIsShowingRemovePluginConfirmationDialog);
        outState.putBoolean(KEY_IS_SHOWING_INSTALL_FIRST_PLUGIN_CONFIRMATION_DIALOG,
                mIsShowingInstallFirstPluginConfirmationDialog);
        outState.putBoolean(KEY_IS_SHOWING_AUTOMATED_TRANSFER_PROGRESS, mIsShowingAutomatedTransferProgress);
        outState.putBoolean(KEY_IS_SHOWING_DOMAIN_CREDIT_CHECK_PROGRESS,
                mCheckingDomainCreditsProgressDialog != null && mCheckingDomainCreditsProgressDialog.isShowing());
        outState.putInt(KEY_PLUGIN_RECHECKED_TIMES, mPluginReCheckTimer);
    }

    // UI Helpers

    private void setupViews() {
        mContainer = findViewById(R.id.plugin_detail_container);
        mTitleTextView = findViewById(R.id.text_title);
        mByLineTextView = findViewById(R.id.text_byline);
        mVersionTopTextView = findViewById(R.id.plugin_version_top);
        mVersionBottomTextView = findViewById(R.id.plugin_version_bottom);
        mInstalledText = findViewById(R.id.plugin_installed);
        mUpdateButton = findViewById(R.id.plugin_btn_update);
        mInstallButton = findViewById(R.id.plugin_btn_install);
        mSwitchActive = findViewById(R.id.plugin_state_active);
        mSwitchAutoupdates = findViewById(R.id.plugin_state_autoupdates);
        mImageBanner = findViewById(R.id.image_banner);
        mImageIcon = findViewById(R.id.image_icon);

        mWPOrgPluginDetailsContainer = findViewById(R.id.plugin_wp_org_details_container);
        mRatingsSectionContainer = findViewById(R.id.plugin_ratings_section_container);

        mDescriptionTextView = findViewById(R.id.plugin_description_text);
        mDescriptionChevron = findViewById(R.id.plugin_description_chevron);
        findViewById(R.id.plugin_description_container).setOnClickListener(
                v -> toggleText(mDescriptionTextView, mDescriptionChevron));

        mInstallationTextView = findViewById(R.id.plugin_installation_text);
        mInstallationChevron = findViewById(R.id.plugin_installation_chevron);
        findViewById(R.id.plugin_installation_container).setOnClickListener(
                v -> toggleText(mInstallationTextView, mInstallationChevron));

        mWhatsNewTextView = findViewById(R.id.plugin_whatsnew_text);
        mWhatsNewChevron = findViewById(R.id.plugin_whatsnew_chevron);
        findViewById(R.id.plugin_whatsnew_container).setOnClickListener(
                v -> toggleText(mWhatsNewTextView, mWhatsNewChevron));

        // expand description if this plugin isn't installed, otherwise expand "what's new" if
        // this is an installed plugin and there's an update available
        if (mPlugin.isInstalled()) {
            toggleText(mDescriptionTextView, mDescriptionChevron);
        } else if (PluginUtils.isUpdateAvailable(mPlugin)) {
            toggleText(mWhatsNewTextView, mWhatsNewChevron);
        }

        mFaqTextView = findViewById(R.id.plugin_faq_text);
        mFaqChevron = findViewById(R.id.plugin_faq_chevron);
        findViewById(R.id.plugin_faq_container).setOnClickListener(v -> toggleText(mFaqTextView, mFaqChevron));

        findViewById(R.id.plugin_version_layout).setOnClickListener(v -> showPluginInfoPopup());

        mSwitchActive.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (compoundButton.isPressed()) {
                if (NetworkUtils.checkConnection(PluginDetailActivity.this)) {
                    mIsActive = isChecked;
                    dispatchConfigurePluginAction(false);
                } else {
                    compoundButton.setChecked(mIsActive);
                }
            }
        });

        mSwitchAutoupdates.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (compoundButton.isPressed()) {
                if (NetworkUtils.checkConnection(PluginDetailActivity.this)) {
                    mIsAutoUpdateEnabled = isChecked;
                    dispatchConfigurePluginAction(false);
                } else {
                    compoundButton.setChecked(mIsAutoUpdateEnabled);
                }
            }
        });

        mUpdateButton.setOnClickListener(view -> dispatchUpdatePluginAction());

        mInstallButton.setOnClickListener(v -> {
            if (isCustomDomainRequired()) {
                showDomainCreditsCheckProgressDialog();
                mDispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(mSite));
            } else {
                dispatchInstallPluginAction();
            }
        });

        View settingsView = findViewById(R.id.plugin_settings_page);
        if (canShowSettings()) {
            settingsView.setVisibility(View.VISIBLE);
            settingsView.setOnClickListener(v -> openUrl(mPlugin.getSettingsUrl()));
        } else {
            settingsView.setVisibility(View.GONE);
        }

        findViewById(R.id.plugin_wp_org_page).setOnClickListener(view -> openUrl(getWpOrgPluginUrl()));

        findViewById(R.id.plugin_home_page).setOnClickListener(view -> openUrl(mPlugin.getHomepageUrl()));

        findViewById(R.id.read_reviews_container).setOnClickListener(view -> openUrl(getWpOrgReviewsUrl()));

        // set the height of the gradient scrim that appears atop the banner image
        int toolbarHeight = DisplayUtils.getActionBarHeight(this);
        ImageView imgScrim = findViewById(R.id.image_gradient_scrim);
        imgScrim.getLayoutParams().height = toolbarHeight * 2;

        refreshViews();
    }

    private boolean isCustomDomainRequired() {
        return mSite.getUrl().contains(".wordpress.com");
    }

    private void refreshViews() {
        View scrollView = findViewById(R.id.scroll_view);
        if (scrollView.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(scrollView, AniUtils.Duration.MEDIUM);
        }

        mTitleTextView.setText(mPlugin.getDisplayName());
        mImageManager.load(mImageBanner, ImageType.PHOTO, StringUtils.notNullStr(mPlugin.getBanner()),
                ScaleType.CENTER_CROP);
        mImageManager.load(mImageIcon, ImageType.PLUGIN, StringUtils.notNullStr(mPlugin.getIcon()));
        if (mPlugin.doesHaveWPOrgPluginDetails()) {
            mWPOrgPluginDetailsContainer.setVisibility(View.VISIBLE);
            setCollapsibleHtmlText(mDescriptionTextView, mPlugin.getDescriptionAsHtml());
            setCollapsibleHtmlText(mInstallationTextView, mPlugin.getInstallationInstructionsAsHtml());
            setCollapsibleHtmlText(mWhatsNewTextView, mPlugin.getWhatsNewAsHtml());
            setCollapsibleHtmlText(mFaqTextView, mPlugin.getFaqAsHtml());
        } else {
            mWPOrgPluginDetailsContainer.setVisibility(View.GONE);
        }
        mByLineTextView.setMovementMethod(WPLinkMovementMethod.getInstance());
        if (!TextUtils.isEmpty(mPlugin.getAuthorAsHtml())) {
            mByLineTextView.setText(Html.fromHtml(mPlugin.getAuthorAsHtml()));
        } else {
            String authorName = mPlugin.getAuthorName();
            String authorUrl = mPlugin.getAuthorUrl();
            if (TextUtils.isEmpty(authorUrl)) {
                mByLineTextView.setText(String.format(getString(R.string.plugin_byline), authorName));
            } else {
                String authorLink = "<a href='" + authorUrl + "'>" + authorName + "</a>";
                String byline = String.format(getString(R.string.plugin_byline), authorLink);
                mByLineTextView.setMovementMethod(WPLinkMovementMethod.getInstance());
                mByLineTextView.setText(Html.fromHtml(byline));
            }
        }

        findViewById(R.id.plugin_card_site)
                .setVisibility(mPlugin.isInstalled() && isNotAutoManaged() ? View.VISIBLE : View.GONE);
        findViewById(R.id.plugin_state_active_container)
                .setVisibility(canPluginBeDisabledOrRemoved() ? View.VISIBLE : View.GONE);
        findViewById(R.id.plugin_state_autoupdates_container)
                .setVisibility(mSite.isAutomatedTransfer() ? View.GONE : View.VISIBLE);
        mSwitchActive.setChecked(mIsActive);
        mSwitchAutoupdates.setChecked(mIsAutoUpdateEnabled);

        refreshPluginVersionViews();
        refreshRatingsViews();
    }

    private void setCollapsibleHtmlText(@NonNull TextView textView, @Nullable String htmlText) {
        if (!TextUtils.isEmpty(htmlText)) {
            textView.setTextColor(ContextExtensionsKt.getColorFromAttribute(this, R.attr.colorOnSurface));
            textView.setMovementMethod(WPLinkMovementMethod.getInstance());
            textView.setText(Html.fromHtml(htmlText));
        } else {
            textView.setTextColor(
                    ContextExtensionsKt.getColorStateListFromAttribute(this, R.attr.wpColorOnSurfaceMedium));
            textView.setText(R.string.plugin_empty_text);
        }
    }

    private void refreshPluginVersionViews() {
        String pluginVersion = TextUtils.isEmpty(mPlugin.getInstalledVersion()) ? "?" : mPlugin.getInstalledVersion();
        String availableVersion = mPlugin.getWPOrgPluginVersion();
        String versionTopText = "";
        String versionBottomText = "";
        if (mPlugin.isInstalled() && isNotAutoManaged()) {
            if (PluginUtils.isUpdateAvailable(mPlugin)) {
                versionTopText = String.format(getString(R.string.plugin_available_version), availableVersion);
                versionBottomText = String.format(getString(R.string.plugin_installed_version), pluginVersion);
            } else {
                versionTopText = String.format(getString(R.string.plugin_version), pluginVersion);
            }
        } else if (!TextUtils.isEmpty(availableVersion)) {
            versionTopText = String.format(getString(R.string.plugin_version), availableVersion);
        }
        mVersionTopTextView.setText(versionTopText);
        mVersionBottomTextView.setVisibility(TextUtils.isEmpty(versionBottomText) ? View.GONE : View.VISIBLE);
        mVersionBottomTextView.setText(versionBottomText);

        refreshUpdateVersionViews();
    }

    private void refreshUpdateVersionViews() {
        if (mPlugin.isInstalled()) {
            mInstallButton.setVisibility(View.GONE);
            if (isNotAutoManaged()) {
                boolean isUpdateAvailable = PluginUtils.isUpdateAvailable(mPlugin);
                boolean canUpdate = isUpdateAvailable && !mIsUpdatingPlugin;
                mUpdateButton.setVisibility(canUpdate ? View.VISIBLE : View.GONE);
                mInstalledText.setVisibility(isUpdateAvailable || mIsUpdatingPlugin ? View.GONE : View.VISIBLE);
            } else {
                mUpdateButton.setVisibility(View.GONE);
                mInstalledText.setVisibility(View.GONE);
            }
        } else {
            mUpdateButton.setVisibility(View.GONE);
            mInstalledText.setVisibility(View.GONE);
            mInstallButton.setVisibility(mIsInstallingPlugin ? View.GONE : View.VISIBLE);
        }

        findViewById(R.id.plugin_update_progress_bar).setVisibility(mIsUpdatingPlugin || mIsInstallingPlugin
                ? View.VISIBLE : View.GONE);
    }

    private void refreshRatingsViews() {
        if (!mPlugin.doesHaveWPOrgPluginDetails()) {
            mRatingsSectionContainer.setVisibility(View.GONE);
            return;
        }
        mRatingsSectionContainer.setVisibility(View.VISIBLE);
        int numRatingsTotal = mPlugin.getNumberOfRatings();

        TextView txtNumRatings = findViewById(R.id.text_num_ratings);
        String numRatings = FormatUtils.formatInt(numRatingsTotal);
        txtNumRatings.setText(String.format(getString(R.string.plugin_num_ratings), numRatings));

        TextView txtNumDownloads = findViewById(R.id.text_num_downloads);
        if (mPlugin.getDownloadCount() > 0) {
            String numDownloads = FormatUtils.formatInt(mPlugin.getDownloadCount());
            txtNumDownloads.setText(String.format(getString(R.string.plugin_num_downloads), numDownloads));
        } else {
            txtNumDownloads.setText("");
        }

        setRatingsProgressBar(R.id.progress5, mPlugin.getNumberOfRatingsOfFive(), numRatingsTotal);
        setRatingsProgressBar(R.id.progress4, mPlugin.getNumberOfRatingsOfFour(), numRatingsTotal);
        setRatingsProgressBar(R.id.progress3, mPlugin.getNumberOfRatingsOfThree(), numRatingsTotal);
        setRatingsProgressBar(R.id.progress2, mPlugin.getNumberOfRatingsOfTwo(), numRatingsTotal);
        setRatingsProgressBar(R.id.progress1, mPlugin.getNumberOfRatingsOfOne(), numRatingsTotal);

        RatingBar ratingBar = findViewById(R.id.rating_bar);
        ratingBar.setRating(mPlugin.getAverageStarRating());
    }

    private void setRatingsProgressBar(@IdRes int progressResId, int numRatingsForStar, int numRatingsTotal) {
        ProgressBar bar = findViewById(progressResId);
        bar.setMax(numRatingsTotal);
        bar.setProgress(numRatingsForStar);
    }

    private static final String KEY_LABEL = "label";
    private static final String KEY_TEXT = "text";

    private String timespanFromUpdateDate(@NonNull String lastUpdated) {
        // ex: 2017-12-13 2:55pm GMT
        if (lastUpdated.endsWith(" GMT")) {
            lastUpdated = lastUpdated.substring(0, lastUpdated.length() - 4);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        try {
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = formatter.parse(lastUpdated);
            return DateTimeUtils.javaDateToTimeSpan(date, this);
        } catch (ParseException var2) {
            return "?";
        }
    }

    protected void showPluginInfoPopup() {
        if (!mPlugin.doesHaveWPOrgPluginDetails()) {
            return;
        }

        List<Map<String, String>> data = new ArrayList<>();
        int[] to = {R.id.text1, R.id.text2};
        String[] from = {KEY_LABEL, KEY_TEXT};
        String[] labels = {
                getString(R.string.plugin_info_version),
                getString(R.string.plugin_info_lastupdated),
                getString(R.string.plugin_info_requires_version),
                getString(R.string.plugin_info_your_version)
        };

        Map<String, String> mapVersion = new HashMap<>();
        mapVersion.put(KEY_LABEL, labels[0]);
        mapVersion.put(KEY_TEXT, StringUtils.notNullStr(mPlugin.getWPOrgPluginVersion()));
        data.add(mapVersion);

        Map<String, String> mapUpdated = new HashMap<>();
        mapUpdated.put(KEY_LABEL, labels[1]);
        mapUpdated
                .put(KEY_TEXT, timespanFromUpdateDate(StringUtils.notNullStr(mPlugin.getLastUpdatedForWPOrgPlugin())));
        data.add(mapUpdated);

        Map<String, String> mapRequiredVer = new HashMap<>();
        mapRequiredVer.put(KEY_LABEL, labels[2]);
        mapRequiredVer.put(KEY_TEXT, StringUtils.notNullStr(mPlugin.getRequiredWordPressVersion()));
        data.add(mapRequiredVer);

        Map<String, String> mapThisVer = new HashMap<>();
        mapThisVer.put(KEY_LABEL, labels[3]);
        mapThisVer.put(KEY_TEXT, !TextUtils.isEmpty(mSite.getSoftwareVersion()) ? mSite.getSoftwareVersion() : "?");
        data.add(mapThisVer);

        SimpleAdapter adapter = new SimpleAdapter(this,
                data,
                R.layout.plugin_info_row,
                from,
                to);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setCancelable(true);
        builder.setAdapter(adapter, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    protected void toggleText(@NonNull final TextView textView, @NonNull ImageView chevron) {
        AniUtils.Duration duration = AniUtils.Duration.SHORT;
        boolean isExpanded = textView.getVisibility() == View.VISIBLE;
        if (isExpanded) {
            AniUtils.fadeOut(textView, duration);
        } else {
            AniUtils.fadeIn(textView, duration);
        }

        float startRotate = isExpanded ? -180f : 0f;
        float endRotate = isExpanded ? 0f : -180f;
        ObjectAnimator animRotate = ObjectAnimator.ofFloat(chevron, View.ROTATION, startRotate, endRotate);
        animRotate.setDuration(duration.toMillis(this));
        animRotate.start();
    }

    protected void openUrl(@Nullable String url) {
        if (url != null) {
            ActivityLauncher.openUrlExternal(this, url);
        }
    }

    private void confirmRemovePlugin() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getResources().getText(R.string.plugin_remove_dialog_title));
        String confirmationMessage = getString(R.string.plugin_remove_dialog_message,
                mPlugin.getDisplayName(),
                SiteUtils.getSiteNameOrHomeURL(mSite));
        builder.setMessage(confirmationMessage);
        builder.setPositiveButton(R.string.remove, (dialogInterface, i) -> {
            mIsShowingRemovePluginConfirmationDialog = false;
            disableAndRemovePlugin();
        });
        builder.setNegativeButton(R.string.cancel,
                (dialogInterface, i) -> mIsShowingRemovePluginConfirmationDialog = false);
        builder.setOnCancelListener(dialogInterface -> mIsShowingRemovePluginConfirmationDialog = false);
        builder.setCancelable(true);
        builder.create();
        mIsShowingRemovePluginConfirmationDialog = true;
        builder.show();
    }

    private void showSuccessfulUpdateSnackbar() {
        WPSnackbar.make(mContainer,
                getString(R.string.plugin_updated_successfully, mPlugin.getDisplayName()),
                Snackbar.LENGTH_LONG)
                  .show();
    }

    private void showSuccessfulInstallSnackbar() {
        WPSnackbar.make(mContainer,
                getString(R.string.plugin_installed_successfully, mPlugin.getDisplayName()),
                Snackbar.LENGTH_LONG)
                  .show();
    }

    private void showSuccessfulPluginRemovedSnackbar() {
        WPSnackbar.make(mContainer,
                getString(R.string.plugin_removed_successfully, mPlugin.getDisplayName()),
                Snackbar.LENGTH_LONG)
                  .show();
    }

    private void showUpdateFailedSnackbar() {
        WPSnackbar.make(mContainer,
                getString(R.string.plugin_updated_failed, mPlugin.getDisplayName()), Snackbar.LENGTH_LONG)
                  .setAction(R.string.retry, view -> dispatchUpdatePluginAction())
                  .show();
    }

    private void showInstallFailedSnackbar() {
        WPSnackbar.make(mContainer,
                getString(R.string.plugin_installed_failed, mPlugin.getDisplayName()), Snackbar.LENGTH_LONG)
                  .setAction(R.string.retry, view -> dispatchInstallPluginAction())
                  .show();
    }

    private void showPluginRemoveFailedSnackbar() {
        WPSnackbar.make(mContainer,
                getString(R.string.plugin_remove_failed, mPlugin.getDisplayName()),
                Snackbar.LENGTH_LONG)
                  .show();
    }

    private void showDomainCreditsCheckProgressDialog() {
        if (mCheckingDomainCreditsProgressDialog == null) {
            mCheckingDomainCreditsProgressDialog = new ProgressDialog(this);
            mCheckingDomainCreditsProgressDialog.setCancelable(true);
            mCheckingDomainCreditsProgressDialog.setIndeterminate(true);

            mCheckingDomainCreditsProgressDialog
                    .setMessage(getString(R.string.plugin_check_domain_credits_progress_dialog_message));
        }
        if (!mCheckingDomainCreditsProgressDialog.isShowing()) {
            mCheckingDomainCreditsProgressDialog.show();
        }
    }

    private void cancelDomainCreditsCheckProgressDialog() {
        if (mCheckingDomainCreditsProgressDialog != null && mCheckingDomainCreditsProgressDialog.isShowing()) {
            mCheckingDomainCreditsProgressDialog.cancel();
        }
    }

    private void showRemovePluginProgressDialog() {
        if (mRemovePluginProgressDialog == null) {
            mRemovePluginProgressDialog = new ProgressDialog(this);
            mRemovePluginProgressDialog.setCancelable(false);
            mRemovePluginProgressDialog.setIndeterminate(true);
            // Even though we are deactivating the plugin to make sure it's disabled on the server side, since the user
            // sees that the plugin is disabled, it'd be confusing to say we are disabling the plugin
            String message = mIsActive
                    ? getString(R.string.plugin_disable_progress_dialog_message, mPlugin.getDisplayName())
                    : getRemovingPluginMessage();
            mRemovePluginProgressDialog.setMessage(message);
        }
        if (!mRemovePluginProgressDialog.isShowing()) {
            mRemovePluginProgressDialog.show();
        }
    }

    private void cancelRemovePluginProgressDialog() {
        if (mRemovePluginProgressDialog != null && mRemovePluginProgressDialog.isShowing()) {
            mRemovePluginProgressDialog.cancel();
        }
    }

    // Network Helpers

    protected void dispatchConfigurePluginAction(boolean forceUpdate) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }
        if (!forceUpdate && mIsConfiguringPlugin) {
            return;
        }
        if (!mPlugin.isInstalled()) {
            return;
        }
        mIsConfiguringPlugin = true;
        mDispatcher.dispatch(PluginActionBuilder.newConfigureSitePluginAction(
                new ConfigureSitePluginPayload(mSite, mPlugin.getName(), mPlugin.getSlug(),
                        mIsActive, mIsAutoUpdateEnabled)));
    }

    protected void dispatchUpdatePluginAction() {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }
        if (!PluginUtils.isUpdateAvailable(mPlugin) || mIsUpdatingPlugin) {
            return;
        }

        mIsUpdatingPlugin = true;
        refreshUpdateVersionViews();
        UpdateSitePluginPayload payload = new UpdateSitePluginPayload(mSite, mPlugin.getName(), mPlugin.getSlug());
        mDispatcher.dispatch(PluginActionBuilder.newUpdateSitePluginAction(payload));
    }

    protected void dispatchInstallPluginAction() {
        if (!NetworkUtils.checkConnection(this) || mPlugin.isInstalled() || mIsInstallingPlugin) {
            return;
        }

        if (SiteUtils.isNonAtomicBusinessPlanSite(mSite)) {
            confirmInstallPluginForAutomatedTransfer();
        } else {
            mIsInstallingPlugin = true;
            refreshUpdateVersionViews();

            InstallSitePluginPayload payload = new InstallSitePluginPayload(mSite, mSlug);
            mDispatcher.dispatch(PluginActionBuilder.newInstallSitePluginAction(payload));
        }
    }

    protected void dispatchRemovePluginAction() {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }
        mRemovePluginProgressDialog.setMessage(getRemovingPluginMessage());
        DeleteSitePluginPayload payload = new DeleteSitePluginPayload(mSite, mPlugin.getName(), mSlug);
        mDispatcher.dispatch(PluginActionBuilder.newDeleteSitePluginAction(payload));
    }

    protected void disableAndRemovePlugin() {
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

        if (!shouldHandleFluxCSitePluginEvent(event.site, event.pluginName)) {
            return;
        }

        mIsConfiguringPlugin = false;
        if (event.isError()) {
            // The plugin was already removed in remote, there is no need to show an error to the user
            if (mIsRemovingPlugin
                && event.error.type == PluginStore.ConfigureSitePluginErrorType.UNKNOWN_PLUGIN) {
                // We still need to dispatch the remove plugin action to remove the local copy
                // and complete the flow gracefully. We can ignore `!mSitePlugin.isActive()` check here since the
                // plugin is not installed anymore on remote
                dispatchRemovePluginAction();
                return;
            }

            ToastUtils.showToast(this, getString(R.string.plugin_configuration_failed, event.error.message));

            // Refresh the UI to plugin's last known state
            refreshPluginFromStore();
            mIsActive = mPlugin.isActive();
            mIsAutoUpdateEnabled = mPlugin.isAutoUpdateEnabled();
            refreshViews();

            if (mIsRemovingPlugin) {
                mIsRemovingPlugin = false;
                cancelRemovePluginProgressDialog();
                showPluginRemoveFailedSnackbar();
            }
            return;
        }

        // Sanity check
        ImmutablePluginModel configuredPlugin = mPluginStore.getImmutablePluginBySlug(mSite, mSlug);
        if (configuredPlugin == null) {
            ToastUtils.showToast(this, R.string.plugin_not_found);
            finish();
            return;
        }
        // Before refreshing the plugin from store, check the changes and track them
        if (mPlugin.isActive() != configuredPlugin.isActive()) {
            Stat stat = configuredPlugin.isActive() ? Stat.PLUGIN_ACTIVATED : Stat.PLUGIN_DEACTIVATED;
            AnalyticsUtils.trackWithSiteDetails(stat, mSite);
        }
        if (mPlugin.isAutoUpdateEnabled() != configuredPlugin.isAutoUpdateEnabled()) {
            Stat stat = configuredPlugin.isAutoUpdateEnabled()
                    ? Stat.PLUGIN_AUTOUPDATE_ENABLED
                    : Stat.PLUGIN_AUTOUPDATE_DISABLED;
            AnalyticsUtils.trackWithSiteDetails(stat, mSite);
        }
        // Now we can update the plugin with the new one from store
        mPlugin = configuredPlugin;

        // The plugin state has been changed while a configuration network call is going on, we need to dispatch another
        // configure plugin action since we don't allow multiple configure actions to happen at the same time
        // This might happen either because user changed the state or a remove plugin action has started
        if (isPluginStateChangedSinceLastConfigurationDispatch()) {
            // The plugin's state in UI has priority over the one in DB as we'll dispatch another configuration change
            // to make sure UI is reflected correctly in network and DB
            dispatchConfigurePluginAction(false);
        } else if (mIsRemovingPlugin && !mPlugin.isActive()) {
            // We don't want to trigger the remove plugin action before configuration changes are reflected in network
            dispatchRemovePluginAction();

            // The plugin should be disabled if it was active, we should show that to the user
            mIsActive = mPlugin.isActive();
            mSwitchActive.setChecked(mIsActive);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWPOrgPluginFetched(OnWPOrgPluginFetched event) {
        if (isFinishing()) {
            return;
        }

        if (!mSlug.equals(event.pluginSlug)) {
            // another plugin fetched, no need to handle it
            return;
        }

        if (event.isError()) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching wporg plugin" + event.pluginSlug
                                + " with type: " + event.error.type);
        } else {
            refreshPluginFromStore();
            refreshViews();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginUpdated(OnSitePluginUpdated event) {
        if (isFinishing()) {
            return;
        }

        if (!shouldHandleFluxCSitePluginEvent(event.site, event.pluginName)) {
            return;
        }

        mIsUpdatingPlugin = false;
        if (event.isError()) {
            AppLog.e(T.PLUGINS, "An error occurred while updating the plugin with type: "
                                + event.error.type + " and message: " + event.error.message);
            refreshPluginVersionViews();
            showUpdateFailedSnackbar();
            return;
        }

        refreshPluginFromStore();
        refreshViews();
        showSuccessfulUpdateSnackbar();

        AnalyticsUtils.trackWithSiteDetails(Stat.PLUGIN_UPDATED, mSite);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginInstalled(OnSitePluginInstalled event) {
        if (isFinishing()) {
            return;
        }

        if (mSite.getId() != event.site.getId() || !mSlug.equals(event.slug)) {
            // Not the event we are interested in
            return;
        }

        mIsInstallingPlugin = false;
        if (event.isError()) {
            AppLog.e(T.PLUGINS, "An error occurred while installing the plugin with type: "
                                + event.error.type + " and message: " + event.error.message);
            refreshPluginVersionViews();
            showInstallFailedSnackbar();
            return;
        }

        mIsInstallingPlugin = false;

        refreshPluginFromStore();

        // TODO: Handle activation and enabling auto-updates for AT first plugin
        // FluxC will try to activate and enable autoupdates for the plugin after it's installed, let's assume that
        // it'll be successful.
        mIsActive = true;
        mIsAutoUpdateEnabled = true;

        refreshViews();
        showSuccessfulInstallSnackbar();
        invalidateOptionsMenu();

        AnalyticsUtils.trackWithSiteDetails(Stat.PLUGIN_INSTALLED, mSite);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginDeleted(OnSitePluginDeleted event) {
        if (isFinishing()) {
            return;
        }

        if (!shouldHandleFluxCSitePluginEvent(event.site, event.pluginName)) {
            return;
        }

        mIsRemovingPlugin = false;
        cancelRemovePluginProgressDialog();
        if (event.isError()) {
            AppLog.e(T.PLUGINS, "An error occurred while removing the plugin with type: "
                                + event.error.type + " and message: " + event.error.message);
            String toastMessage = getString(R.string.plugin_updated_failed_detailed,
                    mPlugin.getDisplayName(), event.error.message);
            ToastUtils.showToast(this, toastMessage, Duration.LONG);
            return;
        }
        AnalyticsUtils.trackWithSiteDetails(Stat.PLUGIN_REMOVED, mSite);

        refreshPluginFromStore();
        if (mPlugin == null) {
            // A plugin that doesn't exist in the directory is removed, go back to plugin list
            finish();
        } else {
            // Refresh the views to show wporg plugin details
            refreshViews();
            invalidateOptionsMenu();
        }
        showSuccessfulPluginRemovedSnackbar();
    }

    // This check should only handle events for already installed plugins - onSitePluginConfigured,
    // onSitePluginUpdated, onSitePluginDeleted
    private boolean shouldHandleFluxCSitePluginEvent(SiteModel eventSite, String eventPluginName) {
        return mSite.getId() == eventSite.getId() // correct site
               && mPlugin.isInstalled() // needs plugin to be already installed
               && mPlugin.getName() != null // sanity check for NPE since if plugin is installed it'll have the name
               && mPlugin.getName().equals(eventPluginName); // event is for the plugin we are showing
    }

    // Utils

    private void refreshPluginFromStore() {
        mPlugin = mPluginStore.getImmutablePluginBySlug(mSite, mSlug);
    }

    protected String getWpOrgPluginUrl() {
        return "https://wordpress.org/plugins/" + mSlug;
    }

    protected String getWpOrgReviewsUrl() {
        return "https://wordpress.org/plugins/" + mSlug + "/#reviews";
    }

    private String getRemovingPluginMessage() {
        return getString(R.string.plugin_remove_progress_dialog_message, mPlugin.getDisplayName());
    }

    private boolean canPluginBeDisabledOrRemoved() {
        if (!mPlugin.isInstalled()) {
            return false;
        }

        // Disable removing jetpack as the site will stop working in the client
        if (PluginUtils.isJetpack(mPlugin)) {
            return false;
        }
        // Disable removing for auto-managed AT sites
        return isNotAutoManaged();
    }

    // only show settings for active plugins on .org sites
    private boolean canShowSettings() {
        return mPlugin.isInstalled()
               && isNotAutoManaged()
               && mPlugin.isActive()
               && !TextUtils.isEmpty(mPlugin.getSettingsUrl());
    }

    private boolean isPluginStateChangedSinceLastConfigurationDispatch() {
        if (!mPlugin.isInstalled()) {
            return false;
        }
        return mPlugin.isActive() != mIsActive || mPlugin.isAutoUpdateEnabled() != mIsAutoUpdateEnabled;
    }

    // Automated Transfer

    /**
     * Automated Transfer starts by confirming that the user will not be able to use their site. We'll need to block the
     * UI for it, so we get a confirmation first in this step.
     */
    private void confirmInstallPluginForAutomatedTransfer() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getResources().getText(R.string.plugin_install_first_plugin_confirmation_dialog_title));
        builder.setMessage(R.string.plugin_install_first_plugin_confirmation_dialog_message);
        builder.setPositiveButton(R.string.plugin_install_first_plugin_confirmation_dialog_install_btn,
                (dialogInterface, i) -> {
                    mIsShowingInstallFirstPluginConfirmationDialog = false;
                    startAutomatedTransfer();
                });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_CONFIRM_DIALOG_CANCELLED, mSite);
            mIsShowingInstallFirstPluginConfirmationDialog = false;
        });
        builder.setOnCancelListener(dialogInterface -> {
            AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_CONFIRM_DIALOG_CANCELLED, mSite);
            mIsShowingInstallFirstPluginConfirmationDialog = false;
        });
        builder.setCancelable(true);
        builder.create();

        AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_CONFIRM_DIALOG_SHOWN, mSite);
        mIsShowingInstallFirstPluginConfirmationDialog = true;
        builder.show();
    }

    /**
     * We'll trigger an eligibility check for the site for Automated Transfer and show a determinate progress bar.
     * Check out `OnAutomatedTransferEligibilityChecked` for its callback.
     */
    private void startAutomatedTransfer() {
        AppLog.v(T.PLUGINS, "Starting the Automated Transfer for '" + mSite.getDisplayName()
                            + "' by checking its eligibility");
        showAutomatedTransferProgressDialog();

        AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_CHECK_ELIGIBILITY, mSite);
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferEligibilityAction(mSite));
    }

    /**
     * The reason we are using a blocking progress bar is that if the user changes anything about the site, adds a post,
     * updates site settings etc, it'll be lost when the Automated Transfer is completed. The process takes about 1 min
     * on average, and we'll be able to update the progress by checking the status of the transfer.
     */
    private void showAutomatedTransferProgressDialog() {
        if (mAutomatedTransferProgressDialog == null) {
            mAutomatedTransferProgressDialog = new ProgressDialog(this);
            mAutomatedTransferProgressDialog.setCancelable(false);
            mAutomatedTransferProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mAutomatedTransferProgressDialog.setIndeterminate(false);
            String message = getString(R.string.plugin_install_first_plugin_progress_dialog_message);
            mAutomatedTransferProgressDialog.setMessage(message);
        }
        if (!mAutomatedTransferProgressDialog.isShowing()) {
            mIsShowingAutomatedTransferProgress = true;
            mAutomatedTransferProgressDialog.show();
        }
    }

    /**
     * Either Automated Transfer is completed or an error occurred.
     */
    private void cancelAutomatedTransferDialog() {
        if (mAutomatedTransferProgressDialog != null && mAutomatedTransferProgressDialog.isShowing()) {
            mAutomatedTransferProgressDialog.cancel();
            mIsShowingAutomatedTransferProgress = false;
        }
    }

    /**
     * Automated Transfer successfully completed, the site has been refreshed and site plugins has been fetched. We can
     * close the progress dialog, get the new version of the plugin from Store and refresh the views
     */
    private void automatedTransferCompleted() {
        AppLog.v(T.PLUGINS, "Automated Transfer successfully completed!");
        AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_FLOW_COMPLETE, mSite);
        cancelAutomatedTransferDialog();
        refreshPluginFromStore();
        dispatchConfigurePluginAction(true);
        refreshViews();
        showSuccessfulInstallSnackbar();
        invalidateOptionsMenu();
    }

    /**
     * Helper for if any of the FluxC Automated Transfer events fail. We are using a Toast for now, but the only likely
     * error is the site missing a domain which will be implemented later on and will be handled differently.
     */
    private void handleAutomatedTransferFailed(String errorMessage) {
        cancelAutomatedTransferDialog();
        BasicFragmentDialog errorDialog = new BasicFragmentDialog();
        errorDialog.initialize(TAG_ERROR_DIALOG, null, errorMessage,
                getString(R.string.dialog_button_ok), null, null);
        errorDialog.show(getSupportFragmentManager(), TAG_ERROR_DIALOG);
    }

    /**
     * This is the first Automated Transfer FluxC event. It returns whether the site is eligible or not with a set of
     * errors for why it's not eligible. We are handling a single error at a time, but all the likely errors should be
     * pre-handled by preventing the access of plugins page.
     * <p>
     * If the site is eligible, we'll initiate the Automated Transfer. Check out `onAutomatedTransferInitiated` for next
     * step.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAutomatedTransferEligibilityChecked(OnAutomatedTransferEligibilityChecked event) {
        if (isFinishing()) {
            return;
        }
        // Checking isEligible handles `event.isError()` implicitly. In this case we won't have access to the specific
        // error code, so we'll show the generic error message.
        if (!event.isEligible) {
            AppLog.e(T.PLUGINS, "Automated Transfer has failed because the site is not eligible!");
            AppLog.e(T.PLUGINS, "Eligibility error codes: " + event.eligibilityErrorCodes);
            if (event.isError()) {
                // This error shouldn't happen under normal circumstances. Instead the call will succeed and return
                // error codes.
                AppLog.e(T.PLUGINS, "Eligibility API error with type: " + event.error.type + " and message: "
                                    + event.error.message);
            }
            String errorCode = event.eligibilityErrorCodes.isEmpty() ? "" : event.eligibilityErrorCodes.get(0);
            if (errorCode.equalsIgnoreCase("transfer_already_exists")) {
                AppLog.v(T.PLUGINS, "Automated Transfer eligibility check resulted in `transfer_already_exists` "
                                    + "error, checking its status...");
                mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferStatusAction(mSite));
            } else {
                AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_NOT_ELIGIBLE, mSite);
                handleAutomatedTransferFailed(getEligibilityErrorMessage(errorCode));
            }
        } else {
            AppLog.v(T.PLUGINS, "The site is eligible for Automated Transfer. Initiating the transfer...");
            AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_INITIATE, mSite);
            mDispatcher.dispatch(SiteActionBuilder
                    .newInitiateAutomatedTransferAction(new InitiateAutomatedTransferPayload(mSite, mSlug)));
        }
    }

    /**
     * After we check the eligibility of a site, the Automated Transfer will be initiated. This is its callback and it
     * should be a fairly quick one, that's why we are not updating the progress bar. The event contains the plugin that
     * will be installed after Automated Transfer is completed, but we don't need to handle anything about that.
     * <p>
     * We don't know if there is any specific errors we might need to handle, so we are just showing a message about it
     * for now.
     * <p>
     * Once the transfer is initiated, we need to start checking the status of it. Check out
     * `onAutomatedTransferStatusChecked` for the callback.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAutomatedTransferInitiated(OnAutomatedTransferInitiated event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(T.PLUGINS, "Automated Transfer failed during initiation with error type " + event.error.type
                                + " and message: " + event.error.message);
            AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_INITIATION_FAILED, mSite);
            handleAutomatedTransferFailed(event.error.message);
        } else {
            AppLog.v(T.PLUGINS, "Automated Transfer is successfully initiated. Checking the status of it...");
            AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_INITIATED, mSite);
            mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferStatusAction(mSite));
        }
    }

    /**
     * After Automated Transfer is initiated, we'll need to check for the status of it several times as the process
     * takes about 1 minute on average. We don't know if there are any specific errors we can handle, so for now we are
     * simply showing the message.
     * <p>
     * We'll get an `isCompleted` flag from the event and when that's `true` we'll need to re-fetch the site. It'll
     * become a Jetpack site at that point and we'll need the updated site to be able to fetch the plugins and refresh
     * this page. If the transfer is not completed, we use the current step and total steps to update the progress bar
     * and check the status again after waiting for a second.
     * <p>
     * Unfortunately we can't close the progress dialog until both the site and its plugins are fetched. Check out
     * `onSiteChanged` for the next step.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAutomatedTransferStatusChecked(OnAutomatedTransferStatusChecked event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(T.PLUGINS, "Automated Transfer failed after initiation with error type " + event.error.type
                                + " and message: " + event.error.message);
            AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_STATUS_FAILED, mSite);
            handleAutomatedTransferFailed(event.error.message);
        } else {
            if (event.isCompleted) {
                AppLog.v(T.PLUGINS, "Automated Transfer is successfully completed. Fetching the site...");
                // The flow is almost complete, we can show 99% complete to give us a second or so to fetch the site
                // and its plugins
                mAutomatedTransferProgressDialog.setProgress(99);
                mAutomatedTransferProgressDialog.setMessage(
                        getString(R.string.plugin_install_first_plugin_almost_finished_dialog_message));
                AnalyticsUtils.trackWithSiteDetails(Stat.AUTOMATED_TRANSFER_STATUS_COMPLETE, mSite);
                mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(mSite));
            } else {
                AppLog.v(T.PLUGINS, "Automated Transfer is still in progress: " + event.currentStep + "/"
                                    + event.totalSteps);
                mAutomatedTransferProgressDialog.setProgress(event.currentStep * 100 / event.totalSteps);
                mHandler.postDelayed(() -> {
                    AppLog.v(T.PLUGINS, "Checking the Automated Transfer status...");
                    // Wait 3 seconds before checking the status again
                    mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferStatusAction(mSite));
                }, DEFAULT_RETRY_DELAY_MS);
            }
        }
    }

    /**
     * Once the Automated Transfer is completed, we'll trigger a fetch for the site since it'll become a Jetpack site.
     * Whenever the site is updated we update `mSite` property. If the Automated Transfer progress dialog is
     * showing and we make sure that the updated site has the correct `isAutomatedTransfer` flag, we fetch the site
     * plugins so we can refresh this page.
     * <p>
     * Check out `onPluginDirectoryFetched` for the last step of a successful Automated Transfer.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (isFinishing()) {
            return;
        }

        if (!event.isError()) {
            mSite = mSiteStore.getSiteBySiteId(mSite.getSiteId());
        } else if (mIsShowingAutomatedTransferProgress) {
            AppLog.e(T.PLUGINS, "Fetching the site after Automated Transfer has failed with error type "
                                + event.error.type + " and message: " + event.error.message);
        }

        if (mIsShowingAutomatedTransferProgress) {
            // We try to fetch the site after Automated Transfer is completed so that we can fetch its plugins. If
            // we are still showing the AT progress and the site is AT site, we can continue with plugins fetch
            if (mSite.isAutomatedTransfer()) {
                AppLog.v(T.PLUGINS, "Site is successfully fetched after Automated Transfer, fetching"
                                    + " the site plugins to complete the process...");
                fetchPluginDirectory(0);
            } else {
                // Either an error occurred while fetching the site or Automated Transfer is not yet reflected in the
                // API response. We need to keep fetching the site until we get the updated site. Otherwise, any changes
                // the user will make after this point will not be done on the correct `SiteModel`. If we don't get the
                // correct site information, it's actually safer if the user force quits the app, because they will
                // start from the my site page and the site will be refreshed.
                mHandler.postDelayed(() -> {
                    AppLog.v(T.PLUGINS, "Fetching the site again after Automated Transfer since the changes "
                                        + "are not yet reflected");
                    // Wait 3 seconds before fetching the site again
                    mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(mSite));
                }, DEFAULT_RETRY_DELAY_MS);
            }
        }
    }

    /**
     * Completing an Automated Transfer will trigger a site fetch which then will trigger a fetch for the site plugins.
     * We'll complete the Automated Transfer if the progress dialog is showing and only update the plugin and the views
     * if it's not.
     * <p>
     * This event is unlikely to happen outside of Automated Transfer process, and it is even less likely that the views
     * will need to be updated because of it, but they are both still possible and we try to handle it with a refresh.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginDirectoryFetched(OnPluginDirectoryFetched event) {
        if (isFinishing()) {
            return;
        }

        refreshPluginFromStore();

        if (event.isError()) {
            if (mIsShowingAutomatedTransferProgress) {
                AppLog.e(T.PLUGINS, "Fetching the plugin directory after Automated Transfer has failed with error type"
                                    + event.error.type + " and message: " + event.error.message);
                // Although unlikely, fetching the plugins after a successful Automated Transfer can result in an error.
                // This should hopefully be an edge case and fetching the plugins again should
                AppLog.v(T.PLUGINS, "Fetching the site plugins again after Automated Transfer since the"
                                    + " changes are not yet reflected");
                fetchPluginDirectory(PLUGIN_RETRY_DELAY_MS);
            }
            // We are safe to ignore the errors for this event unless it's for Automated Transfer since that's the only
            // one triggered in this page and only one we care about.
            return;
        } else if (!mPlugin.isInstalled()) {
            // it sometimes take a bit of time for plugin to get marked as installed, especially when
            // Automated Transfer is performed right after domain registration
            if (mIsShowingAutomatedTransferProgress) {
                if (mPluginReCheckTimer < MAX_PLUGIN_CHECK_TRIES) {
                    AppLog.v(T.PLUGINS, "Targeted plugin is not marked as installed after Automated Transfer."
                                        + " Fetching the site plugins to reflect the changes.");
                    fetchPluginDirectory(PLUGIN_RETRY_DELAY_MS);
                    mPluginReCheckTimer++;
                    return;
                } else {
                    // if plugin is still not marked as installed, we ask user to check back later, and proceed to
                    // finish Automated Transfer
                    ToastUtils.showToast(this, R.string.plugin_fetching_error_after_at, Duration.LONG);
                }
            }
        }
        if (event.type == PluginDirectoryType.SITE && mIsShowingAutomatedTransferProgress) {
            // After Automated Transfer flow is completed, we fetch the site and then it's plugins. The only way site's
            // plugins could be fetched without an error is if the AT is completed and now that we have it's plugins
            // we can finish the whole flow
            automatedTransferCompleted();
        } else {
            // Although it's unlikely that a directory might be fetched while we are in the plugin detail page, we
            // should be safe to refresh the plugin and the view in case the plugin we are showing has changed
            refreshViews();
        }
    }

    private void fetchPluginDirectory(int delay) {
        mHandler.postDelayed(
                () -> mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(new PluginStore
                        .FetchPluginDirectoryPayload(PluginDirectoryType.SITE, mSite, false))), delay);
    }

    private String getEligibilityErrorMessage(String errorCode) {
        int errorMessageRes;
        switch (errorCode) {
            case "email_unverified":
                errorMessageRes = R.string.plugin_install_site_ineligible_email_unverified;
                break;
            case "excessive_disk_space":
                errorMessageRes = R.string.plugin_install_site_ineligible_excessive_disk_space;
                break;
            case "no_business_plan":
                errorMessageRes = R.string.plugin_install_site_ineligible_no_business_plan;
                break;
            case "no_vip_sites":
                errorMessageRes = R.string.plugin_install_site_ineligible_no_vip_sites;
                break;
            case "non_admin_user":
                errorMessageRes = R.string.plugin_install_site_ineligible_non_admin_user;
                break;
            case "not_domain_owner":
                errorMessageRes = R.string.plugin_install_site_ineligible_not_domain_owner;
                break;
            case "not_using_custom_domain":
                errorMessageRes = R.string.plugin_install_site_ineligible_not_using_custom_domain;
                break;
            case "site_graylisted":
                errorMessageRes = R.string.plugin_install_site_ineligible_site_graylisted;
                break;
            case "site_private":
                errorMessageRes = R.string.plugin_install_site_ineligible_site_private;
                break;
            default:
                // no_jetpack_sites, no_ssl_certificate, no_wpcom_nameservers, not_resolving_to_wpcom
                errorMessageRes = R.string.plugin_install_site_ineligible_default_error;
                break;
        }
        return getString(errorMessageRes);
    }

    private boolean isNotAutoManaged() {
        return !PluginUtils.isAutoManaged(mSite, mPlugin);
    }
}
