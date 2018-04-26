package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.ThemeStore.OnWpComThemesChanged;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;

import javax.inject.Inject;

public class SiteCreationThemeLoaderFragment extends Fragment {
    public static final String TAG = "site_creation_theme_loader_fragment_tag";

    public enum ThemesUpdateState {
        UPDATING,
        FINISHED,
        ERROR,
        ERROR_NO_CONNECTIVITY
    }

    static class OnThemeLoadingUpdated {
        private final ThemesUpdateState mState;

        OnThemeLoadingUpdated(ThemesUpdateState state) {
            mState = state;
        }

        ThemesUpdateState getPhase() {
            return mState;
        }
    }

    @Inject Dispatcher mDispatcher;

    // need to inject it even though we're not using it directly, otherwise we can't listen for its event responses
    @Inject ThemeStore mThemeStore;

    @Nullable
    private OnThemeLoadingUpdated getState() {
        return EventBus.getDefault().getStickyEvent(OnThemeLoadingUpdated.class);
    }

    private void postUpdate(ThemesUpdateState state) {
        EventBus.getDefault().postSticky(new OnThemeLoadingUpdated(state));
    }

    private void update() {
        postUpdate(ThemesUpdateState.UPDATING);
        mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        ConnectionChangeReceiver.getEventBus().register(this);
        mDispatcher.register(this);

        update();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDispatcher.unregister(this);
        ConnectionChangeReceiver.getEventBus().unregister(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ConnectionChangeEvent event) {
        OnThemeLoadingUpdated onThemeLoadingUpdated = getState();
        if (isAdded()
            && event.isConnected()
            && onThemeLoadingUpdated != null
            && onThemeLoadingUpdated.getPhase() == ThemesUpdateState.ERROR_NO_CONNECTIVITY) {
            update();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemesChanged(OnWpComThemesChanged event) {
        if (event.isError()) {
            if (NetworkUtils.isNetworkAvailable(getContext())) {
                mDispatcher.unregister(this);
                ConnectionChangeReceiver.getEventBus().unregister(this);

                AppLog.e(AppLog.T.THEMES, "Error fetching themes: " + event.error.message);
                postUpdate(ThemesUpdateState.ERROR);
            } else {
                AppLog.e(AppLog.T.THEMES, "Error fetching themes: " + event.error.message
                                          + ". Seems connectivity is off though so, will try again when back online");
                postUpdate(ThemesUpdateState.ERROR_NO_CONNECTIVITY);
            }
        } else {
            mDispatcher.unregister(this);
            ConnectionChangeReceiver.getEventBus().unregister(this);

            AppLog.d(AppLog.T.THEMES, "WordPress.com Theme fetch successful!");
            postUpdate(ThemesUpdateState.FINISHED);
        }
    }
}
