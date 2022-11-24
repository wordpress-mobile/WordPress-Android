package org.wordpress.android.ui.deeplinks;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.PackageManagerWrapper;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UriWrapper;

import javax.inject.Inject;

import static org.wordpress.android.WordPress.getContext;

/**
 * An activity to handle deep linking and intercepting links like:
 * <p>
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 * <p>
 * Redirects users to the reader activity along with IDs passed in the intent
 */
public class DeepLinkingIntentReceiverActivity extends LocaleAwareActivity {
    @Inject DeepLinkNavigator mDeeplinkNavigator;
    @Inject DeepLinkUriUtils mDeepLinkUriUtils;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject PackageManagerWrapper mPackageManagerWrapper;
    private DeepLinkingIntentReceiverViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(DeepLinkingIntentReceiverViewModel.class);

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
