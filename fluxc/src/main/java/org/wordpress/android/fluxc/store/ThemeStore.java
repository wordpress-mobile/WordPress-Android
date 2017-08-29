package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.ThemeAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeRestClient;
import org.wordpress.android.fluxc.persistence.ThemeSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;

public class ThemeStore extends Store {
    // Payloads
    public static class FetchedCurrentThemePayload extends Payload {
        public SiteModel site;
        public ThemeModel theme;
        public FetchThemesError error;

        public FetchedCurrentThemePayload(FetchThemesError error) {
            this.error = error;
        }

        public FetchedCurrentThemePayload(@NonNull SiteModel site, @NonNull ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    public static class FetchedThemesPayload extends Payload {
        public SiteModel site;
        public List<ThemeModel> themes;
        public FetchThemesError error;

        public FetchedThemesPayload(FetchThemesError error) {
            this.error = error;
        }

        public FetchedThemesPayload(@NonNull SiteModel site, @NonNull List<ThemeModel> themes) {
            this.site = site;
            this.themes = themes;
        }
    }

    public enum ThemeErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE
    }

    public static class FetchThemesError implements OnChangedError {
        ThemeErrorType type;
        public String message;
        public FetchThemesError(ThemeErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class OnThemesChanged extends OnChanged<FetchThemesError> {
        public SiteModel site;

        public OnThemesChanged(SiteModel site) {
            this.site = site;
        }
    }

    public static class OnCurrentThemeFetched extends OnChanged<FetchThemesError> {
        public SiteModel site;
        public ThemeModel theme;

        public OnCurrentThemeFetched(SiteModel site, ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    private final ThemeRestClient mThemeRestClient;

    @Inject
    public ThemeStore(Dispatcher dispatcher, ThemeRestClient themeRestClient) {
        super(dispatcher);
        mThemeRestClient = themeRestClient;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof ThemeAction)) {
            return;
        }
        switch ((ThemeAction) actionType) {
            case FETCH_WP_THEMES:
                fetchWpThemes();
                break;
            case FETCHED_WP_THEMES:
                handleWpThemesFetched((FetchedThemesPayload) action.getPayload());
                break;
            case FETCH_INSTALLED_THEMES:
                fetchInstalledThemes((SiteModel) action.getPayload());
                break;
            case FETCHED_INSTALLED_THEMES:
                handleInstalledThemesFetched((FetchedThemesPayload) action.getPayload());
                break;
            case FETCH_CURRENT_THEME:
                fetchCurrentTheme((SiteModel) action.getPayload());
                break;
            case FETCHED_CURRENT_THEME:
                handleCurrentThemeFetched((FetchedCurrentThemePayload) action.getPayload());
                break;
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "ThemeStore onRegister");
    }

    public List<ThemeModel> getWpThemes() {
        return ThemeSqlUtils.getWpThemes();
    }

    public List<ThemeModel> getThemesForSite(@NonNull SiteModel site) {
        return ThemeSqlUtils.getThemesForSite(site);
    }

    private void fetchWpThemes() {
        mThemeRestClient.fetchWpComThemes();
    }

    private void handleWpThemesFetched(FetchedThemesPayload payload) {
        OnThemesChanged event = new OnThemesChanged(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeSqlUtils.insertOrReplaceWpThemes(payload.themes);
        }
        emitChange(event);
    }

    private void fetchInstalledThemes(@NonNull SiteModel site) {
    }

    private void handleInstalledThemesFetched(FetchedThemesPayload payload) {
        OnThemesChanged event = new OnThemesChanged(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        }
        emitChange(event);
    }

    private void fetchCurrentTheme(@NonNull SiteModel site) {
    }

    private void handleCurrentThemeFetched(FetchedCurrentThemePayload payload) {
        OnCurrentThemeFetched event = new OnCurrentThemeFetched(payload.site, payload.theme);
        if (payload.isError()) {
            event.error = payload.error;
        }
        emitChange(event);
    }
}
