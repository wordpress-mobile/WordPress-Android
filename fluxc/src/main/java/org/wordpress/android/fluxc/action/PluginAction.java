package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.ConfiguredSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.DeletedSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchedPluginInfoPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchedSitePluginsPayload;
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.InstalledSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginVersionPayload;
import org.wordpress.android.fluxc.store.PluginStore.UpdatedSitePluginVersionPayload;

@ActionEnum
public enum PluginAction implements IAction {
    // Remote actions
    @Action(payloadType = ConfigureSitePluginPayload.class)
    CONFIGURE_SITE_PLUGIN,
    @Action(payloadType = DeleteSitePluginPayload.class)
    DELETE_SITE_PLUGIN,
    @Action(payloadType = String.class)
    FETCH_PLUGIN_INFO,
    @Action(payloadType = SiteModel.class)
    FETCH_SITE_PLUGINS,
    @Action(payloadType = InstallSitePluginPayload.class)
    INSTALL_SITE_PLUGIN,
    @Action(payloadType = UpdateSitePluginVersionPayload.class)
    UPDATE_SITE_PLUGIN,

    // Remote responses
    @Action(payloadType = ConfiguredSitePluginPayload.class)
    CONFIGURED_SITE_PLUGIN,
    @Action(payloadType = DeletedSitePluginPayload.class)
    DELETED_SITE_PLUGIN,
    @Action(payloadType = FetchedPluginInfoPayload.class)
    FETCHED_PLUGIN_INFO,
    @Action(payloadType = FetchedSitePluginsPayload.class)
    FETCHED_SITE_PLUGINS,
    @Action(payloadType = InstalledSitePluginPayload.class)
    INSTALLED_SITE_PLUGIN,
    @Action(payloadType = UpdatedSitePluginVersionPayload.class)
    UPDATED_SITE_PLUGIN
}
