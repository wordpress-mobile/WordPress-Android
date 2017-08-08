package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PluginStore.FetchedPluginsPayload;

@ActionEnum
public enum PluginAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteModel.class)
    FETCH_PLUGINS,

    // Remote responses
    @Action(payloadType = FetchedPluginsPayload.class)
    FETCHED_PLUGINS,
    @Action(payloadType = PluginInfoModel.class)
    FETCHED_PLUGIN_INFO,
}
