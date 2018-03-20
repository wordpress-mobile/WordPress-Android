package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.DeleteSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.ExportSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.FetchWPComSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.IsWPComResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.AutomatedTransferEligibilityResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedUserRolesPayload;
import org.wordpress.android.fluxc.store.SiteStore.InitiateAutomatedTransferResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.InitiateAutomatedTransferPayload;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsResponsePayload;

@ActionEnum
public enum SiteAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteModel.class)
    FETCH_PROFILE_XML_RPC,
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
    FETCH_USER_ROLES,
    @Action(payloadType = SiteModel.class)
    DELETE_SITE,
    @Action(payloadType = SiteModel.class)
    EXPORT_SITE,
    @Action(payloadType = String.class)
    IS_WPCOM_URL,
    @Action(payloadType = SuggestDomainsPayload.class)
    SUGGEST_DOMAINS,
    @Action(payloadType = String.class)
    FETCH_CONNECT_SITE_INFO,
    @Action(payloadType = String.class)
    FETCH_WPCOM_SITE_BY_URL,
    @Action(payloadType = SiteModel.class)
    CHECK_AUTOMATED_TRANSFER_ELIGIBILITY,
    @Action(payloadType = InitiateAutomatedTransferPayload.class)
    INITIATE_AUTOMATED_TRANSFER,
    @Action(payloadType = SiteModel.class)
    CHECK_AUTOMATED_TRANSFER_STATUS,

    // Remote responses
    @Action(payloadType = SiteModel.class)
    FETCHED_PROFILE_XML_RPC,
    @Action(payloadType = SitesModel.class)
    FETCHED_SITES,
    @Action(payloadType = SitesModel.class)
    FETCHED_SITES_XML_RPC,
    @Action(payloadType = NewSiteResponsePayload.class)
    CREATED_NEW_SITE,
    @Action(payloadType = FetchedPostFormatsPayload.class)
    FETCHED_POST_FORMATS,
    @Action(payloadType = FetchedUserRolesPayload.class)
    FETCHED_USER_ROLES,
    @Action(payloadType = DeleteSiteResponsePayload.class)
    DELETED_SITE,
    @Action(payloadType = ExportSiteResponsePayload.class)
    EXPORTED_SITE,
    @Action(payloadType = ConnectSiteInfoPayload.class)
    FETCHED_CONNECT_SITE_INFO,
    @Action(payloadType = FetchWPComSiteResponsePayload.class)
    FETCHED_WPCOM_SITE_BY_URL,
    @Action(payloadType = AutomatedTransferEligibilityResponsePayload.class)
    CHECKED_AUTOMATED_TRANSFER_ELIGIBILITY,
    @Action(payloadType = InitiateAutomatedTransferResponsePayload.class)
    INITIATED_AUTOMATED_TRANSFER,

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
    REMOVE_WPCOM_AND_JETPACK_SITES,
    @Action(payloadType = SitesModel.class)
    SHOW_SITES,
    @Action(payloadType = SitesModel.class)
    HIDE_SITES,
    @Action(payloadType = IsWPComResponsePayload.class)
    CHECKED_IS_WPCOM_URL,
    @Action(payloadType = SuggestDomainsResponsePayload.class)
    SUGGESTED_DOMAINS,
}
