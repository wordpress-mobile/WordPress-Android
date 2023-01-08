package org.wordpress.android.ui.deeplinks;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import org.wordpress.android.ui.ActivityLauncherWrapper;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayViewModel;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions.ForwardToJetpack;
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackAllFeaturesOverlaySource;
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource;
import org.wordpress.android.util.PackageManagerWrapper;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UriWrapper;

import javax.inject.Inject;

import static org.wordpress.android.WordPress.getContext;

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
    private DeepLinkingIntentReceiverViewModel mViewModel;
    private JetpackFeatureFullScreenOverlayViewModel mJetpackFullScreenViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(DeepLinkingIntentReceiverViewModel.class);
        mJetpackFullScreenViewModel = new ViewModelProvider(this).get(JetpackFeatureFullScreenOverlayViewModel.class);
        setupObservers();

        mViewModel.start(
                getIntent().getAction(),
                (getIntent().getData() == null) ? null : new UriWrapper(getIntent().getData()),
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
                        JetpackAllFeaturesOverlaySource.UNSPECIFIED)
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
