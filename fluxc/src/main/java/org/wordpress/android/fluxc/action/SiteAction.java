package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.DeleteSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.IsWPComResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.ExportSiteResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;

@ActionEnum
public enum SiteAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteModel.class)
    FETCH_SITE,
    @Action
    FETCH_SITES,
    @Action(payloadType = RefreshSitesXMLRPCPayload.class)
    FETCH_SITES_XML_RPC,
    @Action(payloadType = NewSitePayload.class)
    CREATE_NEW_SITE,
    @Action(payloadType = SiteModel.class)
    FETCH_POST_FORMATS,
    @Action(payloadType = SiteModel.class)
    DELETE_SITE,
    @Action(payloadType = SiteModel.class)
    EXPORT_SITE,
    @Action(payloadType = String.class)
    IS_WPCOM_URL,

    // Remote responses
    @Action(payloadType = NewSiteResponsePayload.class)
    CREATED_NEW_SITE,
    @Action(payloadType = FetchedPostFormatsPayload.class)
    FETCHED_POST_FORMATS,
    @Action(payloadType = DeleteSiteResponsePayload.class)
    DELETED_SITE,
    @Action(payloadType = ExportSiteResponsePayload.class)
    EXPORTED_SITE,

    // Local actions
    @Action(payloadType = SiteModel.class)
    UPDATE_SITE,
    @Action(payloadType = SitesModel.class)
    UPDATE_SITES,
    @Action(payloadType = SiteModel.class)
    REMOVE_SITE,
    @Action
    REMOVE_ALL_SITES,
    @Action
    REMOVE_WPCOM_SITES,
    @Action(payloadType = SitesModel.class)
    SHOW_SITES,
    @Action(payloadType = SitesModel.class)
    HIDE_SITES,
    @Action(payloadType = IsWPComResponsePayload.class)
    CHECKED_IS_WPCOM_URL
}
