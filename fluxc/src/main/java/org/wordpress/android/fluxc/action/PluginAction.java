package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PluginStore.UpdatePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchedPluginInfoPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchedPluginsPayload;

@ActionEnum
public enum PluginAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteModel.class)
    FETCH_PLUGINS,
    @Action(payloadType = String.class)
    FETCH_PLUGIN_INFO,
    @Action(payloadType = UpdatePluginPayload.class)
    UPDATE_PLUGIN,

    // Remote responses
    @Action(payloadType = FetchedPluginsPayload.class)
    FETCHED_PLUGINS,
    @Action(payloadType = FetchedPluginInfoPayload.class)
    FETCHED_PLUGIN_INFO,
}
