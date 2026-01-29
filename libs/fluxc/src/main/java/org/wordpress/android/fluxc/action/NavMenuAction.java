package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.NavMenuStore.FetchMenuItemsPayload;
import org.wordpress.android.fluxc.store.NavMenuStore.FetchMenuItemsResponsePayload;
import org.wordpress.android.fluxc.store.NavMenuStore.FetchMenuLocationsPayload;
import org.wordpress.android.fluxc.store.NavMenuStore.FetchMenuLocationsResponsePayload;
import org.wordpress.android.fluxc.store.NavMenuStore.FetchMenusPayload;
import org.wordpress.android.fluxc.store.NavMenuStore.FetchMenusResponsePayload;
import org.wordpress.android.fluxc.store.NavMenuStore.RemoteMenuItemPayload;
import org.wordpress.android.fluxc.store.NavMenuStore.RemoteMenuPayload;

@ActionEnum
public enum NavMenuAction implements IAction {
    // Menu remote actions
    @Action(payloadType = FetchMenusPayload.class)
    FETCH_MENUS,
    @Action(payloadType = RemoteMenuPayload.class)
    PUSH_MENU,
    @Action(payloadType = RemoteMenuPayload.class)
    DELETE_MENU,

    // Menu remote responses
    @Action(payloadType = FetchMenusResponsePayload.class)
    FETCHED_MENUS,
    @Action(payloadType = RemoteMenuPayload.class)
    PUSHED_MENU,
    @Action(payloadType = RemoteMenuPayload.class)
    DELETED_MENU,

    // Menu item remote actions
    @Action(payloadType = FetchMenuItemsPayload.class)
    FETCH_MENU_ITEMS,
    @Action(payloadType = RemoteMenuItemPayload.class)
    PUSH_MENU_ITEM,
    @Action(payloadType = RemoteMenuItemPayload.class)
    DELETE_MENU_ITEM,

    // Menu item remote responses
    @Action(payloadType = FetchMenuItemsResponsePayload.class)
    FETCHED_MENU_ITEMS,
    @Action(payloadType = RemoteMenuItemPayload.class)
    PUSHED_MENU_ITEM,
    @Action(payloadType = RemoteMenuItemPayload.class)
    DELETED_MENU_ITEM,

    // Menu locations remote actions
    @Action(payloadType = FetchMenuLocationsPayload.class)
    FETCH_MENU_LOCATIONS,

    // Menu locations remote responses
    @Action(payloadType = FetchMenuLocationsResponsePayload.class)
    FETCHED_MENU_LOCATIONS,

    // Local actions
    @Action
    REMOVE_ALL_MENUS
}
