package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.SubscriptionsModel;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient
        .AccountFetchUsernameSuggestionsResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountPushSettingsResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountPushSocialResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountPushUsernameResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountRestPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.FetchAuthOptionsResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.DomainContactPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.IsAvailableResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.NewAccountResponsePayload;
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload;
import org.wordpress.android.fluxc.store.AccountStore.FetchAuthOptionsPayload;
import org.wordpress.android.fluxc.store.AccountStore.FetchUsernameSuggestionsPayload;
import org.wordpress.android.fluxc.store.AccountStore.NewAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialAuthPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialSmsPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushUsernamePayload;
import org.wordpress.android.fluxc.store.AccountStore.SubscriptionResponsePayload;
import org.wordpress.android.fluxc.store.AccountStore.UpdateSubscriptionPayload;
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload;

@ActionEnum
public enum AccountAction implements IAction {
    // Remote actions
    @Action
    FETCH_ACCOUNT,          // request fetch of Account information
    @Action
    FETCH_SETTINGS,         // request fetch of Account Settings
    @Action(payloadType = FetchUsernameSuggestionsPayload.class)
    FETCH_USERNAME_SUGGESTIONS,  // request fetch of Username Suggestions
    @Action
    SEND_VERIFICATION_EMAIL, // request verification email for unverified accounts
    @Action(payloadType = PushAccountSettingsPayload.class)
    PUSH_SETTINGS,          // request saving Account Settings remotely
    @Action(payloadType = PushSocialAuthPayload.class)
    PUSH_SOCIAL_AUTH,      // request social auth remotely
    @Action(payloadType = PushSocialPayload.class)
    PUSH_SOCIAL_CONNECT,    // request social connect remotely
    @Action(payloadType = PushSocialPayload.class)
    PUSH_SOCIAL_LOGIN,      // request social login remotely
    @Action(payloadType = PushSocialPayload.class)
    PUSH_SOCIAL_SIGNUP,     // request social signup remotely
    @Action(payloadType = PushSocialSmsPayload.class)
    PUSH_SOCIAL_SMS,      // request social sms remotely
    @Action(payloadType = PushUsernamePayload.class)
    PUSH_USERNAME,        // request username remotely
    @Action(payloadType = NewAccountPayload.class)
    CREATE_NEW_ACCOUNT,     // create a new account (can be used to validate the account before creating it)
    @Action(payloadType = String.class)
    IS_AVAILABLE_BLOG,
    @Action(payloadType = String.class)
    IS_AVAILABLE_EMAIL,
    @Action(payloadType = String.class)
    IS_AVAILABLE_USERNAME,
    @Action
    FETCH_SUBSCRIPTIONS,
    @Action(payloadType = AddOrDeleteSubscriptionPayload.class)
    UPDATE_SUBSCRIPTION_EMAIL_COMMENT,
    @Action(payloadType = AddOrDeleteSubscriptionPayload.class)
    UPDATE_SUBSCRIPTION_EMAIL_POST,
    @Action(payloadType = UpdateSubscriptionPayload.class)
    UPDATE_SUBSCRIPTION_EMAIL_POST_FREQUENCY,
    @Action(payloadType = AddOrDeleteSubscriptionPayload.class)
    UPDATE_SUBSCRIPTION_NOTIFICATION_POST,
    @Action
    FETCH_DOMAIN_CONTACT,
    @Action(payloadType = FetchAuthOptionsPayload.class)
    FETCH_AUTH_OPTIONS,

    // Remote responses
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_ACCOUNT,        // response received from Account fetch request
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_SETTINGS,       // response received from Account Settings fetch
    @Action(payloadType = AccountFetchUsernameSuggestionsResponsePayload.class)
    FETCHED_USERNAME_SUGGESTIONS,  // response received from Username Suggestions fetch
    @Action(payloadType = NewAccountResponsePayload.class)
    SENT_VERIFICATION_EMAIL,
    @Action(payloadType = AccountPushSettingsResponsePayload.class)
    PUSHED_SETTINGS,        // response received from Account Settings post
    @Action(payloadType = AccountPushSocialResponsePayload.class)
    PUSHED_SOCIAL,          // response received from social login post
    @Action(payloadType = AccountPushUsernameResponsePayload.class)
    PUSHED_USERNAME,        // response received from username post
    @Action(payloadType = NewAccountResponsePayload.class)
    CREATED_NEW_ACCOUNT,    // create a new account response
    @Action(payloadType = IsAvailableResponsePayload.class)
    CHECKED_IS_AVAILABLE,
    @Action(payloadType = SubscriptionsModel.class)
    FETCHED_SUBSCRIPTIONS,
    @Action(payloadType = SubscriptionResponsePayload.class)
    UPDATED_SUBSCRIPTION,
    @Action(payloadType = DomainContactPayload.class)
    FETCHED_DOMAIN_CONTACT,
    @Action(payloadType = FetchAuthOptionsResponsePayload.class)
    FETCHED_AUTH_OPTIONS,

    // Local actions
    @Action(payloadType = AccountModel.class)
    UPDATE_ACCOUNT,         // update in-memory and persisted Account in AccountStore
    @Action(payloadType = UpdateTokenPayload.class)
    UPDATE_ACCESS_TOKEN,    // update in-memory and persisted Access Token
    @Action
    SIGN_OUT                // delete persisted Account, reset in-memory Account, delete access token
}
