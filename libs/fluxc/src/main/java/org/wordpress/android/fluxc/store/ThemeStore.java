package org.wordpress.android.fluxc.store;

import android.database.Cursor;
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
import javax.inject.Singleton;

@Singleton
public class ThemeStore extends Store {
    // Payloads
    public static class FetchedCurrentThemePayload extends Payload<ThemesError> {
        public SiteModel site;
        public ThemeModel theme;

        public FetchedCurrentThemePayload(ThemesError error) {
            this.error = error;
        }

        public FetchedCurrentThemePayload(@NonNull SiteModel site, @NonNull ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    public static class FetchedThemesPayload extends Payload<ThemesError> {
        public SiteModel site;
        public List<ThemeModel> themes;

        public FetchedThemesPayload(ThemesError error) {
            this.error = error;
        }

        public FetchedThemesPayload(@NonNull SiteModel site, @NonNull List<ThemeModel> themes) {
            this.site = site;
            this.themes = themes;
        }
    }

    public static class SearchThemesPayload extends Payload<ThemesError> {
        public String searchTerm;

        public SearchThemesPayload(@NonNull String searchTerm) {
            this.searchTerm = searchTerm;
        }
    }

    public static class SearchedThemesPayload extends Payload<ThemesError> {
        public String searchTerm;
        public List<ThemeModel> themes;

        public SearchedThemesPayload(@NonNull String searchTerm, List<ThemeModel> themes) {
            this.searchTerm = searchTerm;
            this.themes = themes;
        }

        public SearchedThemesPayload(ThemesError error) {
            this.error = error;
        }
    }

    public static class ActivateThemePayload extends Payload<ThemesError> {
        public SiteModel site;
        public ThemeModel theme;

        public ActivateThemePayload(SiteModel site, ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    public enum ThemeErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE,
        THEME_NOT_FOUND,
        THEME_ALREADY_INSTALLED,
        UNKNOWN_THEME,
        MISSING_THEME;

        public static ThemeErrorType fromString(String type) {
            if (type != null) {
                for (ThemeErrorType v : ThemeErrorType.values()) {
                    if (type.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public static class ThemesError implements OnChangedError {
        public ThemeErrorType type;
        public String message;

        public ThemesError(String type, String message) {
            this.type = ThemeErrorType.fromString(type);
            this.message = message;
        }

        public ThemesError(ThemeErrorType type) {
            this.type = type;
        }
    }

    // OnChanged events
    public static class OnThemesChanged extends OnChanged<ThemesError> {
        public SiteModel site;
        public ThemeAction origin;

        public OnThemesChanged(SiteModel site) {
            this.site = site;
        }
    }

    public static class OnCurrentThemeFetched extends OnChanged<ThemesError> {
        public SiteModel site;
        public ThemeModel theme;

        public OnCurrentThemeFetched(SiteModel site, ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    public static class OnThemesSearched extends OnChanged<ThemesError> {
        public List<ThemeModel> searchResults;

        public OnThemesSearched(List<ThemeModel> searchResults) {
            this.searchResults = searchResults;
        }
    }

    public static class OnThemeActivated extends OnChanged<ThemesError> {
        public SiteModel site;
        public ThemeModel theme;

        public OnThemeActivated(SiteModel site, ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    public static class OnThemeDeleted extends OnChanged<ThemesError> {
        public SiteModel site;
        public ThemeModel theme;

        public OnThemeDeleted(SiteModel site, ThemeModel theme) {
            this.site = site;
            this.theme = theme;
        }
    }

    public static class OnThemeInstalled extends OnChanged<ThemesError> {
        public SiteModel site;
        public ThemeModel theme;

        public OnThemeInstalled(SiteModel site, ThemeModel theme) {
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
            case FETCH_WP_COM_THEMES:
                fetchWpComThemes();
                break;
            case FETCHED_WP_COM_THEMES:
                handleWpComThemesFetched((FetchedThemesPayload) action.getPayload());
                break;
            case FETCH_INSTALLED_THEMES:
                fetchInstalledThemes((SiteModel) action.getPayload());
                break;
            case FETCHED_INSTALLED_THEMES:
                handleInstalledThemesFetched((FetchedThemesPayload) action.getPayload());
                break;
            case FETCH_PURCHASED_THEMES:
                break;
            case FETCHED_PURCHASED_THEMES:
                break;
            case FETCH_CURRENT_THEME:
                fetchCurrentTheme((SiteModel) action.getPayload());
                break;
            case FETCHED_CURRENT_THEME:
                handleCurrentThemeFetched((FetchedCurrentThemePayload) action.getPayload());
                break;
            case SEARCH_THEMES:
                SearchThemesPayload searchPayload = (SearchThemesPayload) action.getPayload();
                searchThemes(searchPayload.searchTerm);
                break;
            case SEARCHED_THEMES:
                handleSearchedThemes((SearchedThemesPayload) action.getPayload());
                break;
            case ACTIVATE_THEME:
                activateTheme((ActivateThemePayload) action.getPayload());
                break;
            case ACTIVATED_THEME:
                handleThemeActivated((ActivateThemePayload) action.getPayload());
                break;
            case INSTALL_THEME:
                installTheme((ActivateThemePayload) action.getPayload());
                break;
            case INSTALLED_THEME:
                handleThemeInstalled((ActivateThemePayload) action.getPayload());
                break;
            case DELETE_THEME:
                deleteTheme((ActivateThemePayload) action.getPayload());
                break;
            case DELETED_THEME:
                handleThemeDeleted((ActivateThemePayload) action.getPayload());
                break;
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "ThemeStore onRegister");
    }

    public List<ThemeModel> getWpComThemes() {
        return ThemeSqlUtils.getWpComThemes();
    }

    public Cursor getWpComThemesCursor() {
        return ThemeSqlUtils.getWpComThemesCursor();
    }

    public Cursor getThemesCursorForSite(@NonNull SiteModel site) {
        return ThemeSqlUtils.getThemesForSiteAsCursor(site);
    }

    public List<ThemeModel> getThemesForSite(@NonNull SiteModel site) {
        return ThemeSqlUtils.getThemesForSite(site);
    }

    public ThemeModel getInstalledThemeByThemeId(String themeId) {
        if (themeId == null || themeId.isEmpty()) {
            return null;
        }
        return ThemeSqlUtils.getThemeByThemeId(themeId, false);
    }

    public ThemeModel getWpComThemeByThemeId(String themeId) {
        if (themeId == null || themeId.isEmpty()) {
            return null;
        }
        return ThemeSqlUtils.getThemeByThemeId(themeId, true);
    }

    public ThemeModel getActiveThemeForSite(@NonNull SiteModel site) {
        List<ThemeModel> activeTheme = ThemeSqlUtils.getActiveThemeForSite(site);
        return activeTheme.isEmpty() ? null : activeTheme.get(0);
    }

    public void setActiveThemeForSite(@NonNull SiteModel site, @NonNull ThemeModel theme) {
        ThemeSqlUtils.insertOrReplaceActiveThemeForSite(site, theme);
    }

    private void fetchWpComThemes() {
        mThemeRestClient.fetchWpComThemes();
    }

    private void handleWpComThemesFetched(@NonNull FetchedThemesPayload payload) {
        OnThemesChanged event = new OnThemesChanged(payload.site);
        event.origin = ThemeAction.FETCH_WP_COM_THEMES;
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeSqlUtils.insertOrReplaceWpComThemes(payload.themes);
        }
        emitChange(event);
    }

    private void fetchInstalledThemes(@NonNull SiteModel site) {
        if (site.isJetpackConnected() && site.isUsingWpComRestApi()) {
            mThemeRestClient.fetchJetpackInstalledThemes(site);
        } else {
            ThemesError error = new ThemesError(ThemeErrorType.NOT_AVAILABLE);
            FetchedThemesPayload payload = new FetchedThemesPayload(error);
            handleInstalledThemesFetched(payload);
        }
    }

    private void handleInstalledThemesFetched(@NonNull FetchedThemesPayload payload) {
        OnThemesChanged event = new OnThemesChanged(payload.site);
        event.origin = ThemeAction.FETCH_INSTALLED_THEMES;
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeSqlUtils.insertOrReplaceInstalledThemes(payload.site, payload.themes);
        }
        emitChange(event);
    }

    private void fetchCurrentTheme(@NonNull SiteModel site) {
        if (site.isUsingWpComRestApi()) {
            mThemeRestClient.fetchCurrentTheme(site);
        } else {
            ThemesError error = new ThemesError(ThemeErrorType.NOT_AVAILABLE);
            FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(error);
            handleCurrentThemeFetched(payload);
        }
    }

    private void handleCurrentThemeFetched(@NonNull FetchedCurrentThemePayload payload) {
        OnCurrentThemeFetched event = new OnCurrentThemeFetched(payload.site, payload.theme);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeSqlUtils.insertOrReplaceActiveThemeForSite(payload.site, payload.theme);
        }
        emitChange(event);
    }

    private void searchThemes(@NonNull String searchTerm) {
        mThemeRestClient.searchThemes(searchTerm);
    }

    private void handleSearchedThemes(@NonNull SearchedThemesPayload payload) {
        OnThemesSearched event = new OnThemesSearched(payload.themes);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            for (ThemeModel theme : payload.themes) {
                ThemeSqlUtils.insertOrUpdateThemeForSite(theme);
            }
        }
        emitChange(event);
    }

    private void installTheme(@NonNull ActivateThemePayload payload) {
        if (payload.site.isJetpackConnected() && payload.site.isUsingWpComRestApi()) {
            mThemeRestClient.installTheme(payload.site, payload.theme);
        } else {
            payload.error = new ThemesError(ThemeErrorType.NOT_AVAILABLE);
            handleThemeInstalled(payload);
        }
    }

    private void handleThemeInstalled(@NonNull ActivateThemePayload payload) {
        OnThemeInstalled event = new OnThemeInstalled(payload.site, payload.theme);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeSqlUtils.insertOrUpdateThemeForSite(payload.theme);
        }
        emitChange(event);
    }

    private void activateTheme(@NonNull ActivateThemePayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mThemeRestClient.activateTheme(payload.site, payload.theme);
        } else {
            payload.error = new ThemesError(ThemeErrorType.NOT_AVAILABLE);
            handleThemeActivated(payload);
        }
    }

    private void handleThemeActivated(@NonNull ActivateThemePayload payload) {
        OnThemeActivated event = new OnThemeActivated(payload.site, payload.theme);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeModel activatedTheme;
            // payload theme doesn't have all the data so we grab a copy of the database theme and update active flag
            if (payload.site.isJetpackConnected()) {
                activatedTheme = getInstalledThemeByThemeId(payload.theme.getThemeId());
            } else {
                activatedTheme = getWpComThemeByThemeId(payload.theme.getThemeId());
                activatedTheme.setIsWpComTheme(false);
            }
            if (activatedTheme != null) {
                setActiveThemeForSite(payload.site, activatedTheme);
            }
        }
        emitChange(event);
    }

    private void deleteTheme(@NonNull ActivateThemePayload payload) {
        if (payload.site.isJetpackConnected() && payload.site.isUsingWpComRestApi()) {
            mThemeRestClient.deleteTheme(payload.site, payload.theme);
        } else {
            payload.error = new ThemesError(ThemeErrorType.NOT_AVAILABLE);
            handleThemeDeleted(payload);
        }
    }

    private void handleThemeDeleted(@NonNull ActivateThemePayload payload) {
        OnThemeDeleted event = new OnThemeDeleted(payload.site, payload.theme);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            ThemeSqlUtils.removeTheme(payload.theme);
        }
        emitChange(event);
    }
}
