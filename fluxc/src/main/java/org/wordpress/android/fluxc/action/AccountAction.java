package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountPushSettingsResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountRestPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.NewAccountResponsePayload;
import org.wordpress.android.fluxc.store.AccountStore.NewAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload;
import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.model.AccountModel;

@ActionEnum
public enum AccountAction implements org.wordpress.android.fluxc.annotations.action.IAction {
    @Action
    FETCH_ACCOUNT,          // request fetch of Account information
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_ACCOUNT,        // response received from Account fetch request
    @Action
    FETCH_SETTINGS,         // request fetch of Account Settings
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_SETTINGS,       // response received from Account Settings fetch
    @Action(payloadType = PushAccountSettingsPayload.class)
    PUSH_SETTINGS,          // request saving Account Settings remotely
    @Action(payloadType = AccountPushSettingsResponsePayload.class)
    PUSHED_SETTINGS,        // response received from Account Settings post
    @Action(payloadType = AccountModel.class)
    UPDATE_ACCOUNT,                 // update in-memory and persisted Account in AccountStore
    @Action(payloadType = UpdateTokenPayload.class)
    UPDATE_ACCESS_TOKEN,    // update in-memory and persisted Access Token
    @Action
    SIGN_OUT,               // delete persisted Account, reset in-memory Account, delete access token
    @Action(payloadType = NewAccountPayload.class)
    CREATE_NEW_ACCOUNT,     // create a new account (can be used to validate the account before creating it)
    @Action(payloadType = NewAccountResponsePayload.class)
    CREATED_NEW_ACCOUNT,    // create a new account response
}
