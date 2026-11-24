package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateTwoFactorPayload;
import org.wordpress.android.fluxc.store.AccountStore.StartWebauthnChallengePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.FinishWebauthnChallengePayload;

@ActionEnum
public enum AuthenticationAction implements IAction {
    // Remote actions
    @Action(payloadType = AuthenticatePayload.class)
    AUTHENTICATE,
    @Action(payloadType = AuthenticateTwoFactorPayload.class)
    AUTHENTICATE_TWO_FACTOR,

    // Remote responses
    @Action(payloadType = AuthenticateErrorPayload.class)
    AUTHENTICATE_ERROR,
    @Action(payloadType = StartWebauthnChallengePayload.class)
    START_SECURITY_KEY_CHALLENGE,

    @Action(payloadType = FinishWebauthnChallengePayload.class)
    FINISH_SECURITY_KEY_CHALLENGE
}
