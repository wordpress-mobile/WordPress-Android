package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountPushSettingsResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountRestPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.IsAvailableResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.NewAccountResponsePayload;
import org.wordpress.android.fluxc.store.AccountStore.NewAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload;

@ActionEnum
public enum AccountAction implements IAction {
    // Remote actions
    @Action
    FETCH_ACCOUNT,          // request fetch of Account information
    @Action
    FETCH_SETTINGS,         // request fetch of Account Settings
    @Action(payloadType = PushAccountSettingsPayload.class)
    PUSH_SETTINGS,          // request saving Account Settings remotely
    @Action(payloadType = NewAccountPayload.class)
    CREATE_NEW_ACCOUNT,     // create a new account (can be used to validate the account before creating it)
    @Action(payloadType = String.class)
    IS_AVAILABLE_BLOG,
    @Action(payloadType = String.class)
    IS_AVAILABLE_DOMAIN,
    @Action(payloadType = String.class)
    IS_AVAILABLE_EMAIL,
    @Action(payloadType = String.class)
    IS_AVAILABLE_USERNAME,

    // Remote responses
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_ACCOUNT,        // response received from Account fetch request
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_SETTINGS,       // response received from Account Settings fetch
    @Action(payloadType = AccountPushSettingsResponsePayload.class)
    PUSHED_SETTINGS,        // response received from Account Settings post
    @Action(payloadType = NewAccountResponsePayload.class)
    CREATED_NEW_ACCOUNT,    // create a new account response
    @Action(payloadType = IsAvailableResponsePayload.class)
    CHECKED_IS_AVAILABLE,

    // Local actions
    @Action(payloadType = AccountModel.class)
    UPDATE_ACCOUNT,         // update in-memory and persisted Account in AccountStore
    @Action(payloadType = UpdateTokenPayload.class)
    UPDATE_ACCESS_TOKEN,    // update in-memory and persisted Access Token
    @Action
    SIGN_OUT                // delete persisted Account, reset in-memory Account, delete access token
}
