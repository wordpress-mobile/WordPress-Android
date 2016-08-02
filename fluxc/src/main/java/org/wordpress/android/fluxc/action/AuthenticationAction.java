package org.wordpress.android.fluxc.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder.DiscoveryResultPayload;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator.AuthenticateErrorPayload;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.stores.store.SiteStore.RefreshSitesXMLRPCPayload;

@ActionEnum
public enum AuthenticationAction implements IAction {
    @Action(payloadType = AuthenticatePayload.class)
    AUTHENTICATE,
    @Action(payloadType = AuthenticateErrorPayload.class)
    AUTHENTICATE_ERROR,
    @Action(payloadType = RefreshSitesXMLRPCPayload.class)
    DISCOVER_ENDPOINT,
    @Action(payloadType = DiscoveryResultPayload.class)
    DISCOVERY_RESULT,
    UNAUTHORIZED,
}
