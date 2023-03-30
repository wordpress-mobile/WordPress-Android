package org.wordpress.android.ui.deeplinks;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import org.wordpress.android.ui.ActivityLauncherWrapper;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayViewModel;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions.ForwardToJetpack;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource;
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource;
import org.wordpress.android.ui.utils.JetpackAppMigrationFlowUtils;
import org.wordpress.android.ui.utils.PreMigrationDeepLinkData;
import org.wordpress.android.util.PackageManagerWrapper;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UriWrapper;
import org.wordpress.android.util.extensions.CompatExtensionsKt;

import javax.inject.Inject;

import static org.wordpress.android.WordPress.getContext;
import static org.wordpress.android.ui.main.WPMainActivity.ARG_BYPASS_MIGRATION;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * An activity to handle deep linking and intercepting links like:
 * <p>
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 * <p>
 * Redirects users to the reader activity along with IDs passed in the intent
 */
@AndroidEntryPoint
public class DeepLinkingIntentReceiverActivity extends LocaleAwareActivity {
    @Inject DeepLinkNavigator mDeeplinkNavigator;
    @Inject DeepLinkUriUtils mDeepLinkUriUtils;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject PackageManagerWrapper mPackageManagerWrapper;
    @Inject ActivityLauncherWrapper mActivityLauncherWrapper;
    @Inject JetpackAppMigrationFlowUtils mJetpackAppMigrationFlowUtils;
    private DeepLinkingIntentReceiverViewModel mViewModel;
    private JetpackFeatureFullScreenOverlayViewModel mJetpackFullScreenViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                CompatExtensionsKt.onBackPressedCompat(getOnBackPressedDispatcher(), this);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        mViewModel = new ViewModelProvider(this).get(DeepLinkingIntentReceiverViewModel.class);
        mJetpackFullScreenViewModel = new ViewModelProvider(this).get(JetpackFeatureFullScreenOverlayViewModel.class);
        setupObservers();

        String action = getIntent().getAction();
        Uri data = getIntent().getData();
        boolean shouldBypassMigration = getIntent().getBooleanExtra(ARG_BYPASS_MIGRATION, false);

        // Start migration flow passing deep link data if requirements are met
        if (!shouldBypassMigration && mJetpackAppMigrationFlowUtils.shouldShowMigrationFlow()) {
            PreMigrationDeepLinkData deepLinkData = new PreMigrationDeepLinkData(action, data);
            mJetpackAppMigrationFlowUtils.startJetpackMigrationFlow(deepLinkData);
            return;
        }

        mViewModel.start(
                action,
                (data == null) ? null : new UriWrapper(data),
                extractEntryPoint(getIntent()),
                savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        mViewModel.writeToBundle(outState);
        super.onSaveInstanceState(outState);
    }

    private void setupObservers() {
        mViewModel.getNavigateAction()
                  .observe(this, navigateActionEvent -> navigateActionEvent.applyIfNotHandled(navigateAction -> {
                      mDeeplinkNavigator.handleNavigationAction(navigateAction, this);
                      return null;
                  }));
        mViewModel.getFinish()
                  .observe(this, finishEvent -> finishEvent.applyIfNotHandled(unit -> {
                      finish();
                      return null;
                  }));
        mViewModel.getToast().observe(this, toastEvent -> toastEvent.applyIfNotHandled(toastMessage -> {
            ToastUtils.showToast(getContext(), toastMessage);
            return null;
        }));
        mViewModel.getShowOpenWebLinksWithJetpackOverlay().observe(this,
                showOverlay -> showOverlay.applyIfNotHandled(unit -> {
                    showOverlay();
                    return null;
                }));

        observeOverlayEvents();
    }

    private void observeOverlayEvents() {
        mJetpackFullScreenViewModel.getAction().observe(this,
                action -> {
                    if (action instanceof ForwardToJetpack) {
                        mViewModel.forwardDeepLinkToJetpack();
                    } else {
                        mViewModel.handleRequest();
                    }
                });
    }

    private void showOverlay() {
        JetpackFeatureFullScreenOverlayFragment
                .newInstance(
                        null,
                        false,
                        true,
                        SiteCreationSource.UNSPECIFIED,
                        false,
                        JetpackFeatureCollectionOverlaySource.UNSPECIFIED)
                .show(getSupportFragmentManager(), JetpackFeatureFullScreenOverlayFragment.TAG);
    }


    private DeepLinkEntryPoint extractEntryPoint(Intent intent) {
        return DeepLinkEntryPoint.fromResId(mPackageManagerWrapper.getActivityLabelResFromIntent(intent));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // show the post if user is returning from successful login
        if (requestCode == RequestCodes.DO_LOGIN && resultCode == RESULT_OK) {
            mViewModel.onSuccessfulLogin();
        } else {
            finish();
        }
    }
}
