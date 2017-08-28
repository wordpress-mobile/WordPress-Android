package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedCurrentThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedThemesPayload;

@ActionEnum
public enum ThemeAction implements IAction {
    // Remote actions
    @Action
    FETCH_WP_THEMES,
    @Action(payloadType = SiteModel.class)
    FETCH_INSTALLED_THEMES, // Jetpack only
    @Action(payloadType = SiteModel.class)
    FETCH_PURCHASED_THEMES,
    @Action(payloadType = SiteModel.class)
    FETCH_CURRENT_THEME,

    // Remote responses
    @Action(payloadType = FetchedThemesPayload.class)
    FETCHED_WP_THEMES,
    @Action(payloadType = FetchedThemesPayload.class)
    FETCHED_INSTALLED_THEMES,
    @Action(payloadType = FetchedThemesPayload.class)
    FETCHED_PURCHASED_THEMES,
    @Action(payloadType = FetchedCurrentThemePayload.class)
    FETCHED_CURRENT_THEME,
}
