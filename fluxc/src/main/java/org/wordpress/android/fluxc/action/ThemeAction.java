package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedCurrentThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedSiteThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedWpComThemesPayload;

@ActionEnum
public enum ThemeAction implements IAction {
    // Remote actions
    @Action
    FETCH_WP_COM_THEMES,
    @Action(payloadType = SiteModel.class)
    FETCH_INSTALLED_THEMES, // Jetpack only
    @Action(payloadType = SiteModel.class)
    FETCH_CURRENT_THEME,
    @Action(payloadType = SiteThemePayload.class)
    ACTIVATE_THEME,
    @Action(payloadType = SiteThemePayload.class)
    INSTALL_THEME,
    @Action(payloadType = SiteThemePayload.class)
    DELETE_THEME,

    // Remote responses
    @Action(payloadType = FetchedWpComThemesPayload.class)
    FETCHED_WP_COM_THEMES,
    @Action(payloadType = FetchedSiteThemesPayload.class)
    FETCHED_INSTALLED_THEMES,
    @Action(payloadType = FetchedCurrentThemePayload.class)
    FETCHED_CURRENT_THEME,
    @Action(payloadType = SiteThemePayload.class)
    ACTIVATED_THEME,
    @Action(payloadType = SiteThemePayload.class)
    INSTALLED_THEME,
    @Action(payloadType = SiteThemePayload.class)
    DELETED_THEME,

    // Local actions
    @Action(payloadType = ThemeModel.class)
    REMOVE_THEME,
    @Action(payloadType = SiteModel.class)
    REMOVE_SITE_THEMES
}
