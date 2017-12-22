package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryResultPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator.AuthEmailResponsePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;

@ActionEnum
public enum AuthenticationAction implements IAction {
    // Remote actions
    @Action(payloadType = AuthenticatePayload.class)
    AUTHENTICATE,
    @Action(payloadType = String.class)
    DISCOVER_ENDPOINT,
    @Action(payloadType = AuthEmailPayload.class)
    SEND_AUTH_EMAIL,

    // Remote responses
    @Action(payloadType = AuthenticateErrorPayload.class)
    AUTHENTICATE_ERROR,
    @Action(payloadType = DiscoveryResultPayload.class)
    DISCOVERY_RESULT,
    @Action(payloadType = AuthEmailResponsePayload.class)
    SENT_AUTH_EMAIL
}
