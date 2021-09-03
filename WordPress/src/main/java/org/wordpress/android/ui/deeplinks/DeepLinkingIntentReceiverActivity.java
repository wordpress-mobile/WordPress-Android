package org.wordpress.android.ui.deeplinks;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
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
    private static final String URI_KEY = "uri_key";

    @Inject DeepLinkNavigator mDeeplinkNavigator;
    @Inject DeepLinkUriUtils mDeepLinkUriUtils;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    private DeepLinkingIntentReceiverViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(DeepLinkingIntentReceiverViewModel.class);
        String action = null;
        Uri uri;
        if (savedInstanceState == null) {
            action = getIntent().getAction();
            uri = getIntent().getData();
        } else {
            uri = savedInstanceState.getParcelable(URI_KEY);
        }

        setupObservers();

        UriWrapper uriWrapper = null;
        if (uri != null) {
            uriWrapper = new UriWrapper(uri);
        }
        mViewModel.start(action, uriWrapper);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        UriWrapper cachedUri = mViewModel.getCachedUri();
        if (cachedUri != null) {
            outState.putParcelable(URI_KEY, cachedUri.getUri());
        }
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
