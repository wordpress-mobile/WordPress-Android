package org.wordpress.android.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.elevation.ElevationOverlayProvider;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.FetchPrivateAtomicCookiePayload;
import org.wordpress.android.fluxc.store.SiteStore.OnPrivateAtomicCookieFetched;
import org.wordpress.android.ui.PrivateAtCookieRefreshProgressDialog.PrivateAtCookieProgressDialogOnDismissListener;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ConfigurationExtensionsKt;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.ErrorManagedWebViewClient.ErrorManagedWebViewClientListener;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.URLFilteredWebViewClient;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.WPWebViewClient;
import org.wordpress.android.util.helpers.WPWebChromeClient;
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel;
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.NavBarUiState;
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewMode;
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewModeSelectorStatus;
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState;
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenUiState;
import org.wordpress.android.widgets.WPSnackbar;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import kotlin.Unit;

/**
 * Activity for opening external WordPress links in a webview.
 * <p/>
 * Try to use one of the methods below to open the webview:
 * - openURL
 * - openUrlByUsingMainWPCOMCredentials
 * - openUrlByUsingWPCOMCredentials
 * - openUrlByUsingBlogCredentials (for self hosted sites)
 * <p/>
 * If you need to start the activity with delay, start activity with result, or none of the methods above are enough
 * for your needs,
 * you can start the activity by passing the required parameters, depending on what you need to do.
 * <p/>
 * 1. Load a simple URL (without any kind of authentication)
 * - Start the activity with the parameter URL_TO_LOAD set to the URL to load.
 * <p/>
 * 2. Load a WordPress.com URL
 * Start the activity with the following parameters:
 * - URL_TO_LOAD: target URL to load in the webview.
 * - AUTHENTICATION_URL: The address of the WordPress.com authentication endpoint. Please use WPCOM_LOGIN_URL.
 * - AUTHENTICATION_USER: username.
 * - AUTHENTICATION_PASSWD: password.
 * <p/>
 * 3. Load a WordPress.org URL with authentication
 * - URL_TO_LOAD: target URL to load in the webview.
 * - AUTHENTICATION_URL: The address of the authentication endpoint. Please use the value of getSiteLoginUrl()
 * to retrieve the correct address of the authentication endpoint.
 * - AUTHENTICATION_USER: username.
 * - AUTHENTICATION_PASSWD: password.
 * - LOCAL_BLOG_ID: local id of the blog in the app database. This is required since some blogs could have HTTP Auth,
 * or self-signed certs in place.
 * - REFERRER_URL: url to add as an HTTP referrer header, currently only used for non-authed reader posts
 */
public class WPWebViewActivity extends WebViewActivity implements ErrorManagedWebViewClientListener,
        PrivateAtCookieProgressDialogOnDismissListener {
    public static final String AUTHENTICATION_URL = "authenticated_url";
    public static final String AUTHENTICATION_USER = "authenticated_user";
    public static final String AUTHENTICATION_PASSWD = "authenticated_passwd";
    public static final String USE_GLOBAL_WPCOM_USER = "USE_GLOBAL_WPCOM_USER";
    public static final String URL_TO_LOAD = "url_to_load";
    public static final String WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php";
    public static final String LOCAL_BLOG_ID = "local_blog_id";
    public static final String SHAREABLE_URL = "shareable_url";
    public static final String SHARE_SUBJECT = "share_subject";
    public static final String REFERRER_URL = "referrer_url";
    public static final String DISABLE_LINKS_ON_PAGE = "DISABLE_LINKS_ON_PAGE";
    public static final String ALLOWED_URLS = "allowed_urls";
    public static final String ENCODING_UTF8 = "UTF-8";
    public static final String WEBVIEW_USAGE_TYPE = "webview_usage_type";
    public static final String ACTION_BAR_TITLE = "action_bar_title";
    public static final String SHOW_PREVIEW_MODE_TOGGLE = "SHOW_PREVIEW_MODE_TOGGLE";
    public static final String PRIVATE_AT_SITE_ID = "PRIVATE_AT_SITE_ID";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject UiHelpers mUiHelpers;
    @Inject PrivateAtomicCookie mPrivateAtomicCookie;
    @Inject Dispatcher mDispatcher;

    private ActionableEmptyView mActionableEmptyView;
    private ViewGroup mFullScreenProgressLayout;
    private WPWebViewViewModel mViewModel;
    private ListPopupWindow mPreviewModeSelector;
    private ElevationOverlayProvider mElevationOverlayProvider;
    private View mNavBarContainer;
    private LinearLayout mNavBar;
    private View mNavigateForwardButton;
    private View mNavigateBackButton;
    private View mShareButton;
    private View mExternalBrowserButton;
    private View mPreviewModeButton;
    private View mDesktopPreviewHint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((WordPress) getApplication()).component().inject(this);
        setLightStatusBar();
        super.onCreate(savedInstanceState);
    }

    private void setLightStatusBar() {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            Window window = getWindow();
            window.setStatusBarColor(ContextExtensionsKt.getColorFromAttribute(this, R.attr.colorSurface));

            if (!ConfigurationExtensionsKt.isDarkTheme(getResources().getConfiguration())) {
                window.getDecorView().setSystemUiVisibility(
                        window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    public final void configureView() {
        setContentView(R.layout.wpwebview_activity);

        mActionableEmptyView = findViewById(R.id.actionable_empty_view);
        mFullScreenProgressLayout = findViewById(R.id.progress_layout);
        mWebView = findViewById(R.id.webView);

        WPWebViewUsageCategory webViewUsageCategory = WPWebViewUsageCategory.fromInt(getIntent()
                .getIntExtra(WEBVIEW_USAGE_TYPE, 0));

        initRetryButton();
        initViewModel(webViewUsageCategory);

        mNavBarContainer = findViewById(R.id.navbar_container);

        mElevationOverlayProvider = new ElevationOverlayProvider(WPWebViewActivity.this);

        int elevatedAppbarColor =
                mElevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
                        getResources().getDimension(R.dimen.appbar_elevation));

        mNavBarContainer.setBackgroundColor(elevatedAppbarColor);

        mNavBar = findViewById(R.id.navbar);

        mNavigateBackButton = findViewById(R.id.back_button);
        mNavigateForwardButton = findViewById(R.id.forward_button);
        mShareButton = findViewById(R.id.share_button);
        mExternalBrowserButton = findViewById(R.id.external_browser_button);
        mPreviewModeButton = findViewById(R.id.preview_type_selector_button);
        mDesktopPreviewHint = findViewById(R.id.desktop_preview_hint);

        TooltipCompat.setTooltipText(mNavigateBackButton, mNavigateBackButton.getContentDescription());
        TooltipCompat.setTooltipText(mNavigateForwardButton, mNavigateForwardButton.getContentDescription());
        TooltipCompat.setTooltipText(mShareButton, mShareButton.getContentDescription());
        TooltipCompat.setTooltipText(mExternalBrowserButton, mExternalBrowserButton.getContentDescription());
        TooltipCompat.setTooltipText(mPreviewModeButton, mPreviewModeButton.getContentDescription());

        mNavigateBackButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                mViewModel.navigateBack();
            }
        });

        mNavigateForwardButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                mViewModel.navigateForward();
            }
        });

        mShareButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                mViewModel.share();
            }
        });

        mExternalBrowserButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                mViewModel.openPageInExternalBrowser();
            }
        });

        mPreviewModeButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                mViewModel.togglePreviewModeSelectorVisibility(true);
            }
        });

        final Bundle extras = getIntent().getExtras();

        if (extras != null) {
            boolean isPreviewModeChangeAllowed = extras.getBoolean(SHOW_PREVIEW_MODE_TOGGLE, false);
            if (!isPreviewModeChangeAllowed) {
                mNavBar.setWeightSum(80);
                mPreviewModeButton.setVisibility(View.GONE);
            }
        }

        setupToolbar();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                showSubtitle(actionBar);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                if (isActionableDirectUsage()) {
                    String title = getIntent().getStringExtra(ACTION_BAR_TITLE);
                    if (title != null) {
                        actionBar.setTitle(title);
                    }
                }
            }
        }
    }

    private void showSubtitle(ActionBar actionBar) {
        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            return;
        }

        String originalUrl = extras.getString(URL_TO_LOAD);
        if (originalUrl != null) {
            Uri uri = Uri.parse(originalUrl);
            actionBar.setSubtitle(uri.getHost());
        }
    }

    private void initRetryButton() {
        mActionableEmptyView.button.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                mViewModel.loadIfNecessary();
            }
        });
    }

    private void initViewModel(WPWebViewUsageCategory webViewUsageCategory) {
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(WPWebViewViewModel.class);
        mViewModel.getUiState().observe(this, new Observer<WebPreviewUiState>() {
            @Override public void onChanged(@Nullable WebPreviewUiState webPreviewUiState) {
                if (webPreviewUiState != null) {
                    mUiHelpers.updateVisibility(mActionableEmptyView,
                            webPreviewUiState.getActionableEmptyView());
                    mUiHelpers.updateVisibility(mFullScreenProgressLayout,
                            webPreviewUiState.getFullscreenProgressLayoutVisibility());

                    if (webPreviewUiState instanceof WebPreviewFullscreenUiState) {
                        WebPreviewFullscreenUiState state = (WebPreviewFullscreenUiState) webPreviewUiState;
                        mUiHelpers.setImageOrHide(mActionableEmptyView.image, state.getImageRes());
                        mUiHelpers.setTextOrHide(mActionableEmptyView.title, state.getTitleText());
                        mUiHelpers.setTextOrHide(mActionableEmptyView.subtitle, state.getSubtitleText());
                        mUiHelpers.updateVisibility(mActionableEmptyView.button, state.getButtonVisibility());
                    }

                    invalidateOptionsMenu();
                }
            }
        });

        mViewModel.getLoadNeeded().observe(this, new Observer<Boolean>() {
            @Override public void onChanged(@Nullable Boolean loadNeeded) {
                if (!isActionableDirectUsage() && loadNeeded != null && loadNeeded) {
                    loadContent();
                }
            }
        });


        mViewModel.getNavbarUiState().observe(this, new Observer<NavBarUiState>() {
            @Override
            public void onChanged(@Nullable NavBarUiState navBarUiState) {
                if (navBarUiState != null) {
                    mNavigateBackButton.setEnabled(navBarUiState.getBackNavigationEnabled());
                    mNavigateForwardButton.setEnabled(navBarUiState.getForwardNavigationEnabled());
                    AniUtils.animateBottomBar(mDesktopPreviewHint, navBarUiState.getDesktopPreviewHintVisible());
                }
            }
        });

        mViewModel.getNavigateBack().observe(this, new Observer<Unit>() {
            @Override
            public void onChanged(@Nullable Unit unit) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                    refreshBackForwardNavButtons();
                }
            }
        });

        mViewModel.getNavigateForward().observe(this, new Observer<Unit>() {
            @Override
            public void onChanged(@Nullable Unit unit) {
                if (mWebView.canGoForward()) {
                    mWebView.goForward();
                    refreshBackForwardNavButtons();
                }
            }
        });

        mViewModel.getShare().observe(this, new Observer<Unit>() {
            @Override
            public void onChanged(@Nullable Unit unit) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                // Use the preferred shareable URL or the default webview URL
                Bundle extras = getIntent().getExtras();
                String shareableUrl = extras.getString(SHAREABLE_URL, null);
                if (TextUtils.isEmpty(shareableUrl)) {
                    shareableUrl = mWebView.getUrl();
                }
                share.putExtra(Intent.EXTRA_TEXT, shareableUrl);
                String shareSubject = extras.getString(SHARE_SUBJECT, null);
                if (!TextUtils.isEmpty(shareSubject)) {
                    share.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
                }
                startActivity(Intent.createChooser(share, getText(R.string.share_link)));
            }
        });

        mViewModel.getOpenExternalBrowser().observe(this, new Observer<Unit>() {
            @Override
            public void onChanged(@Nullable Unit unit) {
                ReaderActivityLauncher.openUrl(WPWebViewActivity.this, mWebView.getUrl(),
                        ReaderActivityLauncher.OpenUrlType.EXTERNAL);
            }
        });

        mViewModel.getPreviewModeSelector().observe(this, new Observer<PreviewModeSelectorStatus>() {
            @Override
            public void onChanged(final @Nullable PreviewModeSelectorStatus previewModelSelectorStatus) {
                if (previewModelSelectorStatus != null) {
                    mPreviewModeButton.setEnabled(previewModelSelectorStatus.isEnabled());

                    if (!previewModelSelectorStatus.isVisible()) {
                        return;
                    }

                    mPreviewModeButton.post(new Runnable() {
                        @Override public void run() {
                            int popupWidth =
                                    getResources().getDimensionPixelSize(R.dimen.web_preview_mode_popup_width);
                            int popupOffset = getResources().getDimensionPixelSize(R.dimen.margin_extra_large);

                            mPreviewModeSelector = new ListPopupWindow(WPWebViewActivity.this);
                            mPreviewModeSelector.setWidth(popupWidth);
                            mPreviewModeSelector.setAdapter(new PreviewModeMenuAdapter(WPWebViewActivity.this,
                                    previewModelSelectorStatus.getSelectedPreviewMode()));
                            mPreviewModeSelector.setDropDownGravity(Gravity.END);
                            mPreviewModeSelector.setAnchorView(mPreviewModeButton);
                            mPreviewModeSelector.setHorizontalOffset(-popupOffset);
                            mPreviewModeSelector.setVerticalOffset(popupOffset);
                            mPreviewModeSelector.setModal(true);

                            int elevatedPopupBackgroundColor =
                                    mElevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
                                            getResources().getDimension(R.dimen.popup_over_toolbar_elevation));
                            mPreviewModeSelector.setBackgroundDrawable(new ColorDrawable(elevatedPopupBackgroundColor));

                            mPreviewModeSelector.setOnDismissListener(new OnDismissListener() {
                                @Override public void onDismiss() {
                                    mViewModel.togglePreviewModeSelectorVisibility(false);
                                }
                            });
                            mPreviewModeSelector.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    mPreviewModeSelector.dismiss();
                                    PreviewModeMenuAdapter adapter = (PreviewModeMenuAdapter) parent.getAdapter();
                                    PreviewMode selectedMode = adapter.getItem(position);
                                    mViewModel.selectPreviewMode(selectedMode);
                                }
                            });
                            mPreviewModeSelector.show();
                        }
                    });
                }
            }
        });

        mViewModel.getPreviewMode().observe(this, new Observer<PreviewMode>() {
            @Override
            public void onChanged(@Nullable PreviewMode previewMode) {
                mWebView.getSettings().setLoadWithOverviewMode(previewMode == PreviewMode.DESKTOP);
                mWebView.getSettings().setUseWideViewPort(previewMode != PreviewMode.DESKTOP);
                mWebView.setInitialScale(100);
                mWebView.reload();
            }
        });
        mViewModel.start(webViewUsageCategory);
    }

    public static void openUrlByUsingGlobalWPCOMCredentials(Context context, String url) {
        openWPCOMURL(context, url, null, null, false, false);
    }

    public static void openUrlByUsingGlobalWPCOMCredentials(Context context,
                                                            String url,
                                                            boolean allowPreviewModeSelection) {
        openWPCOMURL(context, url, null, null, allowPreviewModeSelection, false);
    }

    public static void openPostUrlByUsingGlobalWPCOMCredentials(Context context, String url, String shareableUrl,
                                                                String shareSubject, boolean allowPreviewModeSelection,
                                                                boolean startPreviewForResult) {
        openWPCOMURL(context, url, shareableUrl, shareSubject, allowPreviewModeSelection, startPreviewForResult);
    }

    // frameNonce is used to show drafts, without it "no page found" error would be thrown
    public static void openJetpackBlogPostPreview(Context context, String url, String shareableUrl, String shareSubject,
                                                  String frameNonce, boolean allowPreviewModeSelection,
                                                  boolean startPreviewForResult, long privateSiteId) {
        if (!TextUtils.isEmpty(frameNonce)) {
            url += "&frame-nonce=" + UrlUtils.urlEncode(frameNonce);
        }
        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.DISABLE_LINKS_ON_PAGE, false);
        intent.putExtra(WPWebViewActivity.SHOW_PREVIEW_MODE_TOGGLE, allowPreviewModeSelection);
        if (!TextUtils.isEmpty(shareableUrl)) {
            intent.putExtra(WPWebViewActivity.SHAREABLE_URL, shareableUrl);
        }
        if (!TextUtils.isEmpty(shareSubject)) {
            intent.putExtra(WPWebViewActivity.SHARE_SUBJECT, shareSubject);
        }
        if (privateSiteId > 0) {
            intent.putExtra(WPWebViewActivity.PRIVATE_AT_SITE_ID, privateSiteId);
        }
        if (startPreviewForResult) {
            intent.putExtra(WPWebViewActivity.WEBVIEW_USAGE_TYPE, WPWebViewUsageCategory.REMOTE_PREVIEWING.getValue());
            ((Activity) context).startActivityForResult(intent, RequestCodes.REMOTE_PREVIEW_POST);
        } else {
            context.startActivity(intent);
        }
    }

    public static void openUrlByUsingBlogCredentials(
            Context context,
            SiteModel site,
            PostImmutableModel post,
            String url,
            String[] listOfAllowedURLs,
            boolean disableLinks,
            boolean allowPreviewModeSelection,
            boolean startPreviewForResult) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (site == null) {
            AppLog.e(AppLog.T.UTILS, "Site is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL");
            ToastUtils.showToast(context, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
            return;
        }

        String authURL = WPWebViewActivity.getSiteLoginUrl(site);
        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, site.getUsername());
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, site.getPassword());
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
        intent.putExtra(WPWebViewActivity.LOCAL_BLOG_ID, site.getId());
        intent.putExtra(WPWebViewActivity.DISABLE_LINKS_ON_PAGE, disableLinks);
        intent.putExtra(WPWebViewActivity.SHOW_PREVIEW_MODE_TOGGLE, allowPreviewModeSelection);
        intent.putExtra(ALLOWED_URLS, listOfAllowedURLs);
        if (post != null) {
            intent.putExtra(WPWebViewActivity.SHAREABLE_URL, post.getLink());
            if (!TextUtils.isEmpty(post.getTitle())) {
                intent.putExtra(WPWebViewActivity.SHARE_SUBJECT, post.getTitle());
            }
        }

        if (startPreviewForResult) {
            intent.putExtra(WPWebViewActivity.WEBVIEW_USAGE_TYPE,
                    WPWebViewUsageCategory.REMOTE_PREVIEWING.getValue());
            ((Activity) context).startActivityForResult(intent, RequestCodes.REMOTE_PREVIEW_POST);
        } else {
            context.startActivity(intent);
        }
    }

    public static void openActionableEmptyViewDirectly(
            Context context,
            WPWebViewUsageCategory directUsageCategory,
            String postTitle
    ) {
        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.WEBVIEW_USAGE_TYPE, directUsageCategory.getValue());
        intent.putExtra(WPWebViewActivity.ACTION_BAR_TITLE, postTitle);
        context.startActivity(intent);
    }

    protected void toggleNavbarVisibility(boolean isVisible) {
        if (mNavBarContainer != null) {
            if (isVisible) {
                mNavBarContainer.setVisibility(View.VISIBLE);
            } else {
                mNavBarContainer.setVisibility(View.GONE);
            }
        }
    }

    public static void openURL(Context context, String url) {
        openURL(context, url, false, 0);
    }

    public static void openURL(Context context, String url, String referrer) {
        openURL(context, url, referrer, false, 0);
    }

    public static void openURL(Context context, String url, boolean allowPreviewModeSelection,
                               long privateSiteId) {
        openURL(context, url, null, allowPreviewModeSelection, privateSiteId);
    }

    public static void openURL(Context context, String url, String referrer,
                               boolean allowPreviewModeSelection, long privateSiteId) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL");
            ToastUtils.showToast(context, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
            return;
        }

        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.SHOW_PREVIEW_MODE_TOGGLE, allowPreviewModeSelection);
        if (privateSiteId > 0) {
            intent.putExtra(WPWebViewActivity.PRIVATE_AT_SITE_ID, privateSiteId);
        }
        if (!TextUtils.isEmpty(referrer)) {
            intent.putExtra(REFERRER_URL, referrer);
        }
        context.startActivity(intent);
    }

    protected static boolean checkContextAndUrl(Context context, String url) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return false;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL passed to openUrlByUsingMainWPCOMCredentials");
            ToastUtils.showToast(context, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
            return false;
        }
        return true;
    }

    private static void openWPCOMURL(Context context, String url, String shareableUrl, String shareSubject) {
        openWPCOMURL(context, url, shareableUrl, shareSubject, false, false);
    }

    private static void openWPCOMURL(
            Context context,
            String url,
            String shareableUrl,
            String shareSubject,
            boolean allowPreviewModeSelection,
            boolean startPreviewForResult
    ) {
        if (!checkContextAndUrl(context, url)) {
            return;
        }

        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.USE_GLOBAL_WPCOM_USER, true);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, WPCOM_LOGIN_URL);
        intent.putExtra(WPWebViewActivity.SHOW_PREVIEW_MODE_TOGGLE, allowPreviewModeSelection);
        if (!TextUtils.isEmpty(shareableUrl)) {
            intent.putExtra(WPWebViewActivity.SHAREABLE_URL, shareableUrl);
        }
        if (!TextUtils.isEmpty(shareSubject)) {
            intent.putExtra(WPWebViewActivity.SHARE_SUBJECT, shareSubject);
        }

        if (startPreviewForResult) {
            intent.putExtra(WPWebViewActivity.WEBVIEW_USAGE_TYPE,
                    WPWebViewUsageCategory.REMOTE_PREVIEWING.getValue());
            ((Activity) context).startActivityForResult(intent, RequestCodes.REMOTE_PREVIEW_POST);
        } else {
            context.startActivity(intent);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void configureWebView() {
        if (isActionableDirectUsage()) {
            return;
        }

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);

        final Bundle extras = getIntent().getExtras();

        // Configure the allowed URLs if available
        ArrayList<String> allowedURL = null;
        if (extras != null && extras.getBoolean(DISABLE_LINKS_ON_PAGE, false)) {
            String addressToLoad = extras.getString(URL_TO_LOAD);
            String authURL = extras.getString(AUTHENTICATION_URL);
            allowedURL = new ArrayList<>();
            if (!TextUtils.isEmpty(addressToLoad)) {
                allowedURL.add(addressToLoad);
            }
            if (!TextUtils.isEmpty(authURL)) {
                allowedURL.add(authURL);
            }

            if (extras.getStringArray(ALLOWED_URLS) != null) {
                String[] urls = extras.getStringArray(ALLOWED_URLS);
                for (String currentURL : urls) {
                    allowedURL.add(currentURL);
                }
            }
        }

        WebViewClient webViewClient = createWebViewClient(allowedURL);

        mWebView.setWebViewClient(webViewClient);
        mWebView.setWebChromeClient(new WPWebChromeClient(this, (ProgressBar) findViewById(R.id.progress_bar)));
    }

    protected WebViewClient createWebViewClient(List<String> allowedURL) {
        URLFilteredWebViewClient webViewClient;
        if (getIntent().hasExtra(LOCAL_BLOG_ID)) {
            SiteModel site = mSiteStore.getSiteByLocalId(getIntent().getIntExtra(LOCAL_BLOG_ID, -1));
            if (site == null) {
                AppLog.e(AppLog.T.UTILS, "No valid blog passed to WPWebViewActivity");
                setResultIfNeededAndFinish();
            }
            webViewClient = new WPWebViewClient(site, mAccountStore.getAccessToken(), allowedURL, this);
        } else {
            webViewClient = new URLFilteredWebViewClient(allowedURL, this);
        }
        return webViewClient;
    }

    @Override
    public void onWebViewPageLoaded() {
        mViewModel.onUrlLoaded();
        refreshBackForwardNavButtons();
    }

    private void refreshBackForwardNavButtons() {
        mViewModel.toggleBackNavigation(mWebView.canGoBack());
        mViewModel.toggleForwardNavigation(mWebView.canGoForward());
    }

    @Override
    public void onWebViewReceivedError() {
        mViewModel.onReceivedError();
    }

    @Override
    protected void loadContent() {
        if (isActionableDirectUsage()) {
            return;
        }

        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            AppLog.e(AppLog.T.UTILS, "No valid parameters passed to WPWebViewActivity");
            setResultIfNeededAndFinish();
            return;
        }

        // if we load content of private AT site we need to make sure we have a special cookie first
        long privateAtSiteId = extras.getLong(PRIVATE_AT_SITE_ID);
        if (privateAtSiteId > 0 && mPrivateAtomicCookie.isCookieRefreshRequired()) {
            PrivateAtCookieRefreshProgressDialog.Companion.showIfNecessary(getSupportFragmentManager());
            mDispatcher.dispatch(SiteActionBuilder.newFetchPrivateAtomicCookieAction(
                    new FetchPrivateAtomicCookiePayload(privateAtSiteId)));
            return;
        } else if (privateAtSiteId > 0 && mPrivateAtomicCookie.exists()) {
            // make sure we add cookie to the cookie manager if it exists
            CookieManager.getInstance().setCookie(
                    mPrivateAtomicCookie.getDomain(), mPrivateAtomicCookie.getCookieContent()
            );
        }

        loadWebContent();
    }

    private void loadWebContent() {
        Bundle extras = getIntent().getExtras();
        String addressToLoad = extras.getString(URL_TO_LOAD);
        String username = extras.getString(AUTHENTICATION_USER, "");
        String password = extras.getString(AUTHENTICATION_PASSWD, "");
        String authURL = extras.getString(AUTHENTICATION_URL);

        if (TextUtils.isEmpty(addressToLoad) || !UrlUtils.isValidUrlAndHostNotNull(addressToLoad)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null or invalid URL passed to WPWebViewActivity");
            ToastUtils.showToast(this, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
            setResultIfNeededAndFinish();
            return;
        }

        if (extras.getBoolean(USE_GLOBAL_WPCOM_USER, false)) {
            username = mAccountStore.getAccount().getUserName();

            // Custom domains are not properly authenticated due to a server side(?) issue, so this gets around that
            if (!addressToLoad.contains(".wordpress.com")) {
                List<SiteModel> wpComSites = mSiteStore.getWPComSites();
                for (SiteModel siteModel : wpComSites) {
                    // Only replace the url if we know the unmapped url and if it's a custom domain
                    if (!TextUtils.isEmpty(siteModel.getUnmappedUrl())
                        && !siteModel.getUrl().contains(".wordpress.com")) {
                        addressToLoad = addressToLoad.replace(siteModel.getUrl(), siteModel.getUnmappedUrl());
                    }
                }
            }
        }

        if (TextUtils.isEmpty(authURL) && TextUtils.isEmpty(username) && TextUtils.isEmpty(password)) {
            // Only the URL to load is passed to this activity. Use the normal un-authenticated
            // loader, optionally with our referrer header
            String referrerUrl = extras.getString(REFERRER_URL);
            if (!TextUtils.isEmpty(referrerUrl)) {
                Map<String, String> headers = new HashMap<>();
                headers.put("Referer", referrerUrl);
                loadUrl(addressToLoad, headers);
            } else {
                loadUrl(addressToLoad);
            }
        } else {
            if (TextUtils.isEmpty(authURL) || !UrlUtils.isValidUrlAndHostNotNull(authURL)) {
                AppLog.e(AppLog.T.UTILS, "Empty or null or invalid auth URL passed to WPWebViewActivity");
                ToastUtils.showToast(this, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
                setResultIfNeededAndFinish();
            }

            if (TextUtils.isEmpty(username)) {
                AppLog.e(AppLog.T.UTILS, "Username empty/null");
                ToastUtils.showToast(this, R.string.incorrect_credentials, ToastUtils.Duration.SHORT);
                setResultIfNeededAndFinish();
            }

            loadAuthenticatedUrl(authURL, addressToLoad, username, password);
        }
    }

    /**
     * Login to the WordPress.com and load the specified URL.
     */
    protected void loadAuthenticatedUrl(String authenticationURL, String urlToLoad, String username, String password) {
        String postData = getAuthenticationPostData(authenticationURL, urlToLoad, username, password,
                mAccountStore.getAccessToken());

        mWebView.postUrl(authenticationURL, postData.getBytes());
    }

    public static String getAuthenticationPostData(String authenticationUrl, String urlToLoad, String username,
                                                   String password, String token) {
        if (TextUtils.isEmpty(authenticationUrl)) {
            return "";
        }

        try {
            String postData = String.format(
                    "log=%s&pwd=%s&redirect_to=%s",
                    URLEncoder.encode(StringUtils.notNullStr(username), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(password), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(urlToLoad), ENCODING_UTF8)
            );

            // Add token authorization when signing in to WP.com
            if (WPUrlUtils.safeToAddWordPressComAuthToken(authenticationUrl)
                && authenticationUrl.contains("wordpress.com/wp-login.php") && !TextUtils.isEmpty(token)) {
                postData += "&authorization=Bearer " + URLEncoder.encode(token, ENCODING_UTF8);
            }

            return postData;
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        return "";
    }

    /**
     * Get the URL of the WordPress login page.
     *
     * @return URL of the login page.
     */
    public static String getSiteLoginUrl(SiteModel site) {
        String loginURL = site.getLoginUrl();

        // Try to guess the login URL if blogOptions is null (blog not added to the app), or WP version is < 3.6
        if (loginURL == null) {
            if (site.getUrl() != null) {
                return site.getUrl() + "/wp-login.php";
            } else {
                return site.getXmlRpcUrl().replace("xmlrpc.php", "wp-login.php");
            }
        }

        return loginURL;
    }

    private boolean isActionableDirectUsage() {
        return mViewModel.isActionableDirectUsage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mWebView == null) {
            return false;
        }

        int itemID = item.getItemId();

        if (itemID == android.R.id.home) {
            setResultIfNeeded();
        } else if (itemID == R.id.menu_refresh) {
            mWebView.reload();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Since currently we are going to look at the request code and not at the result code
    // in the onActivityResult callbacks, this method is actually redundant, but wanted
    // to be explicit in case of future expansions on this.
    private void setResultIfNeeded() {
        if (getCallingActivity() != null) {
            setResult(RESULT_OK);
        }
    }

    private void setResultIfNeededAndFinish() {
        setResultIfNeeded();
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (mPreviewModeSelector != null && mPreviewModeSelector.isShowing()) {
            mPreviewModeSelector.setOnDismissListener(null);
            mPreviewModeSelector.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            refreshBackForwardNavButtons();
        } else {
            super.onBackPressed();
            setResultIfNeeded();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPrivateAtomicCookieFetched(OnPrivateAtomicCookieFetched event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.MAIN,
                    "Failed to load private AT cookie. " + event.error.type + " - " + event.error.message);
            WPSnackbar.make(findViewById(R.id.snackbar_anchor), R.string.media_accessing_failed, Snackbar.LENGTH_LONG)
                      .show();
        } else {
            CookieManager.getInstance().setCookie(mPrivateAtomicCookie.getDomain(),
                    mPrivateAtomicCookie.getCookieContent());
        }

        // if the dialog is not showing by the time cookie fetched it means that it was dismissed and content was loaded
        if (PrivateAtCookieRefreshProgressDialog.Companion.isShowing(getSupportFragmentManager())) {
            loadWebContent();
            PrivateAtCookieRefreshProgressDialog.Companion.dismissIfNecessary(getSupportFragmentManager());
        }
    }

    @Override
    public void onCookieProgressDialogCancelled() {
        WPSnackbar.make(findViewById(R.id.snackbar_anchor), R.string.media_accessing_failed, Snackbar.LENGTH_LONG)
                  .show();
        loadWebContent();
    }
}
