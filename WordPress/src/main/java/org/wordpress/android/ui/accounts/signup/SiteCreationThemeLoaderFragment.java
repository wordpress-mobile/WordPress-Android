package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.ThemeAction;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AutoForeground;

import javax.inject.Inject;

public class SiteCreationThemeLoaderFragment extends Fragment {
    public static final String TAG = "site_creation_theme_loader_fragment_tag";

    public enum ThemesUpdateState {
        UPDATING,
        FINISHED,
        ERROR
    }

    public static class OnThemeLoadingUpdated implements AutoForeground.ServiceEvent<ThemesUpdateState> {
        private final ThemesUpdateState state;

        OnThemeLoadingUpdated(ThemesUpdateState state) {
            this.state = state;
        }

        @Override
        public ThemesUpdateState getState() {
            return state;
        }
    }

    @Inject Dispatcher mDispatcher;
    @Inject ThemeStore mThemeStore;

    private void postUpdate(ThemesUpdateState state) {
        EventBus.getDefault().postSticky(new OnThemeLoadingUpdated(state));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        postUpdate(ThemesUpdateState.UPDATING);

        mDispatcher.register(this);
        mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDispatcher.unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemesChanged(ThemeStore.OnThemesChanged event) {
        if (event.origin != ThemeAction.FETCH_WP_COM_THEMES) {
            // just bail. This is not the response to the action we initiated
            return;
        }

        mDispatcher.unregister(this);

        if (event.isError()) {
            AppLog.e(AppLog.T.THEMES, "Error fetching themes: " + event.error.message);
            postUpdate(ThemesUpdateState.ERROR);
        } else {
            AppLog.d(AppLog.T.THEMES, "WordPress.com Theme fetch successful!");
            postUpdate(ThemesUpdateState.FINISHED);
        }
    }
}
