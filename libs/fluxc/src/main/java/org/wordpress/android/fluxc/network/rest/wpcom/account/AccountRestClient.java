package org.wordpress.android.fluxc.network.rest.wpcom.account;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.DomainContactModel;
import org.wordpress.android.fluxc.model.SubscriptionModel;
import org.wordpress.android.fluxc.model.SubscriptionsModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.account.SubscriptionRestResponse.SubscriptionsResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.fluxc.store.AccountStore.AccountFetchUsernameSuggestionsError;
import org.wordpress.android.fluxc.store.AccountStore.AccountSocialError;
import org.wordpress.android.fluxc.store.AccountStore.AccountSocialErrorType;
import org.wordpress.android.fluxc.store.AccountStore.AccountUsernameActionType;
import org.wordpress.android.fluxc.store.AccountStore.AccountUsernameError;
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction;
import org.wordpress.android.fluxc.store.AccountStore.AuthOptionsError;
import org.wordpress.android.fluxc.store.AccountStore.AuthOptionsErrorType;
import org.wordpress.android.fluxc.store.AccountStore.DomainContactError;
import org.wordpress.android.fluxc.store.AccountStore.DomainContactErrorType;
import org.wordpress.android.fluxc.store.AccountStore.IsAvailableError;
import org.wordpress.android.fluxc.store.AccountStore.NewUserError;
import org.wordpress.android.fluxc.store.AccountStore.NewUserErrorType;
import org.wordpress.android.fluxc.store.AccountStore.SubscriptionError;
import org.wordpress.android.fluxc.store.AccountStore.SubscriptionResponsePayload;
import org.wordpress.android.fluxc.store.AccountStore.SubscriptionType;
import org.wordpress.android.fluxc.store.AccountStore.UpdateSubscriptionPayload.SubscriptionFrequency;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.LanguageUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class AccountRestClient extends BaseWPComRestClient {
    private static final String SOCIAL_AUTH_ENDPOINT_VERSION = "1";
    private static final String SOCIAL_LOGIN_ENDPOINT_VERSION = "1";
    private static final String SOCIAL_SMS_ENDPOINT_VERSION = "1";

    private final AppSecrets mAppSecrets;

    public static class AccountRestPayload extends Payload<WPComGsonNetworkError> {
        public AccountRestPayload(AccountModel account, WPComGsonNetworkError error) {
            this.account = account;
            this.error = error;
        }
        public AccountModel account;
    }

    public static class AccountPushSettingsResponsePayload extends Payload<BaseNetworkError> {
        public AccountPushSettingsResponsePayload(BaseNetworkError error) {
            this.error = error;
        }
        public Map<String, Object> settings;
    }

    public static class AccountPushSocialResponsePayload extends Payload<AccountSocialError> {
        public AccountPushSocialResponsePayload(AccountSocialResponse response) {
            this.bearerToken = response.bearer_token;
            this.createdAccount = response.created_account;
            this.phoneNumber = response.phone_number;
            this.twoStepNonce = response.two_step_nonce;
            this.twoStepNonceAuthenticator = response.two_step_nonce_authenticator;
            this.twoStepNonceBackup = response.two_step_nonce_backup;
            this.twoStepNonceSms = response.two_step_nonce_sms;
            this.twoStepNotificationSent = response.two_step_notification_sent;
            this.twoStepTypes = convertJsonArrayToStringList(response.two_step_supported_auth_types);
            this.userId = response.user_id;
            this.userName = response.username;
        }
        public AccountPushSocialResponsePayload() {
        }
        public List<String> twoStepTypes;
        public String bearerToken;
        public String phoneNumber;
        public String twoStepNonce;
        public String twoStepNonceAuthenticator;
        public String twoStepNonceBackup;
        public String twoStepNonceSms;
        public String twoStepNotificationSent;
        public String userId;
        public String userName;
        public boolean createdAccount;

        private List<String> convertJsonArrayToStringList(JSONArray array) {
            List<String> list = new ArrayList<>();

            if (array != null) {
                try {
                    for (int i = 0; i < array.length(); i++) {
                        list.add(array.getString(i));
                    }
                } catch (JSONException exception) {
                    AppLog.e(T.API, "Unable to parse two step types: " + exception.getMessage());
                }
            }

            return list;
        }

        public boolean hasPhoneNumber() {
            return !TextUtils.isEmpty(this.phoneNumber);
        }

        public boolean hasToken() {
            return !TextUtils.isEmpty(this.bearerToken);
        }

        public boolean hasTwoStepTypes() {
            return this.twoStepTypes != null && this.twoStepTypes.size() > 0;
        }

        public boolean hasUsername() {
            return !TextUtils.isEmpty(this.userName);
        }
    }

    public static class AccountPushUsernameResponsePayload extends Payload<AccountUsernameError> {
        public AccountUsernameActionType type;
        public String username;

        public AccountPushUsernameResponsePayload(String username, AccountUsernameActionType type) {
            this.username = username;
            this.type = type;
        }
    }

    public static class AccountFetchUsernameSuggestionsResponsePayload extends
            Payload<AccountFetchUsernameSuggestionsError> {
        public List<String> suggestions;

        public AccountFetchUsernameSuggestionsResponsePayload() {
        }

        public AccountFetchUsernameSuggestionsResponsePayload(List<String> suggestions) {
            this.suggestions = suggestions;
        }
    }

    public static class DomainContactPayload extends Payload<DomainContactError> {
        @Nullable public DomainContactModel contactModel;

        public DomainContactPayload(@NonNull DomainContactModel contactModel) {
            this.contactModel = contactModel;
        }

        public DomainContactPayload(@NonNull DomainContactError error) {
            this.error = error;
        }
    }

    public static class AuthOptionsPayload extends Payload<AuthOptionsError> {
        public boolean isPasswordless;
        public boolean isEmailVerified;
    }

    public static class NewAccountResponsePayload extends Payload<NewUserError> {
        public boolean dryRun;
    }

    public static class IsAvailableResponsePayload extends Payload<IsAvailableError> {
        public IsAvailable type;
        public String value;
        public boolean isAvailable;
    }

    public enum IsAvailable {
        EMAIL,
        USERNAME,
        BLOG
    }

    public AccountRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                             AppSecrets appSecrets, AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mAppSecrets = appSecrets;
    }

    /**
     * Performs an HTTP GET call to the v1.1 /me/ endpoint. Upon receiving a
     * response (success or error) a {@link AccountAction#FETCHED_ACCOUNT} action is dispatched
     * with a payload of type {@link AccountRestPayload}. {@link AccountRestPayload#isError()} can
     * be used to determine the result of the request.
     */
    public void fetchAccount() {
        String url = WPCOMREST.me.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, AccountResponse.class,
                new Listener<AccountResponse>() {
                    @Override
                    public void onResponse(AccountResponse response) {
                        AccountModel account = responseToAccountModel(response);
                        AccountRestPayload payload = new AccountRestPayload(account, null);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedAccountAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AccountRestPayload payload = new AccountRestPayload(null, error);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedAccountAction(payload));
                    }
                }
        ));
    }

    /**
     * Performs an HTTP GET call to the v1.1 /me/settings/ endpoint. Upon receiving
     * a response (success or error) a {@link AccountAction#FETCHED_SETTINGS} action is dispatched
     * with a payload of type {@link AccountRestPayload}. {@link AccountRestPayload#isError()} can
     * be used to determine the result of the request.
     */
    public void fetchAccountSettings() {
        String url = WPCOMREST.me.settings.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, AccountSettingsResponse.class,
                new Listener<AccountSettingsResponse>() {
                    @Override
                    public void onResponse(AccountSettingsResponse response) {
                        AccountModel settings = responseToAccountSettingsModel(response);
                        AccountRestPayload payload = new AccountRestPayload(settings, null);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedSettingsAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AccountRestPayload payload = new AccountRestPayload(null, error);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedSettingsAction(payload));
                    }
                }
        ));
    }

    /**
     * Performs an HTTP GET call to the v2 /users/usernames/suggestions endpoint.  Upon receiving a response
     * (success or error) a {@link AccountAction#FETCHED_USERNAME_SUGGESTIONS} action is dispatched with a
     * payload of type {@link AccountRestPayload}.
     *
     * {@link AccountRestPayload#isError()} can be used to check the request result.
     *
     * No HTTP GET call is made if the given parameter map is null or contains no entries.
     *
     * @param name  Text (e.g. display name) from which to create username suggestions
     */
    public void fetchUsernameSuggestions(@NonNull String name) {
        String url = WPCOMV2.users.username.suggestions.getUrl();

        Map<String, String> params = new HashMap<>();
        params.put("name", name);

        addUnauthedRequest(WPComGsonRequest.buildGetRequest(url, params, UsernameSuggestionsResponse.class,
                new Listener<UsernameSuggestionsResponse>() {
                    @Override
                    public void onResponse(UsernameSuggestionsResponse response) {
                        AccountFetchUsernameSuggestionsResponsePayload payload = new
                                AccountFetchUsernameSuggestionsResponsePayload(response.suggestions);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedUsernameSuggestionsAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AccountFetchUsernameSuggestionsResponsePayload payload =
                                new AccountFetchUsernameSuggestionsResponsePayload();
                        payload.error = new AccountFetchUsernameSuggestionsError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedUsernameSuggestionsAction(payload));
                    }
                }
        ));
    }

    public void sendVerificationEmail() {
        String url = WPCOMREST.me.send_verification_email.getUrlV1_1();
        add(WPComGsonRequest.buildPostRequest(url, null, AccountBoolResponse.class,
                new Listener<AccountBoolResponse>() {
                    @Override
                    public void onResponse(AccountBoolResponse response) {
                        NewAccountResponsePayload payload = new NewAccountResponsePayload();
                        mDispatcher.dispatch(AccountActionBuilder.newSentVerificationEmailAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        NewAccountResponsePayload payload = volleyErrorToAccountResponsePayload(error.volleyError);
                        mDispatcher.dispatch(AccountActionBuilder.newSentVerificationEmailAction(payload));
                    }
                }
        ));
    }

    /**
     * Performs an HTTP POST call to the v1.1 /me/settings/ endpoint. Upon receiving
     * a response (success or error) a {@link AccountAction#PUSHED_SETTINGS} action is dispatched
     * with a payload of type {@link AccountPushSettingsResponsePayload}.
     * {@link AccountPushSettingsResponsePayload#isError()} can be used to determine the result of the request.
     *
     * No HTTP POST call is made if the given parameter map is null or contains no entries.
     */
    public void pushAccountSettings(Map<String, Object> body) {
        if (body == null || body.isEmpty()) return;
        String url = WPCOMREST.me.settings.getUrlV1_1();
        // Note: we have to use a Map as a response here because the API response format is different depending
        // of the request we do.
        add(WPComGsonRequest.buildPostRequest(url, body, Map.class,
                new Listener<Map<String, Object>>() {
                    @Override
                    public void onResponse(Map<String, Object> response) {
                        AccountPushSettingsResponsePayload payload = new AccountPushSettingsResponsePayload(null);
                        payload.settings = response;
                        mDispatcher.dispatch(AccountActionBuilder.newPushedSettingsAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AccountPushSettingsResponsePayload payload = new AccountPushSettingsResponsePayload(error);
                        mDispatcher.dispatch(AccountActionBuilder.newPushedSettingsAction(payload));
                    }
                }
        ));
    }

    /**
     * Performs an HTTP POST call to https://wordpress.com/wp-login.php with two-step-authentication-endpoint action.
     * Upon receiving a response (success or error) a {@link AccountAction#PUSHED_SOCIAL} action is dispatched with a
     * payload of type {@link AccountPushSocialResponsePayload}.
     *
     * {@link AccountPushSocialResponsePayload#isError()} can be used to check the request result.
     *
     * No HTTP POST call is made if the given parameter map is null or contains no entries.
     *
     * @param userId    WordPress.com user identification number
     * @param type      Two-factor authentication type (e.g. authenticator, sms, backup)
     * @param nonce     One-time-use token returned in {@link #pushSocialLogin(String, String)}} response
     * @param code      Two-factor authentication code input by the user
     */
    public void pushSocialAuth(@NonNull String userId, @NonNull String type, @NonNull String nonce,
                               @NonNull String code) {
        String url = "https://wordpress.com/wp-login.php";

        Map<String, String> params = new HashMap<>();
        params.put("action", "two-step-authentication-endpoint");
        params.put("version", SOCIAL_AUTH_ENDPOINT_VERSION);
        params.put("user_id", userId);
        params.put("auth_type", type);
        params.put("two_step_nonce", nonce);
        params.put("two_step_code", code);
        params.put("get_bearer_token", "true");
        params.put("client_id", mAppSecrets.getAppId());
        params.put("client_secret", mAppSecrets.getAppSecret());

        AccountSocialRequest request = new AccountSocialRequest(url, params,
                new Listener<AccountSocialResponse>() {
                    @Override
                    public void onResponse(AccountSocialResponse response) {
                        if (response != null) {
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload(response);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        } else {
                            String message = "Received empty response to https://wordpress.com/wp-login.php"
                                             + "?action=two-step-authentication-endpoint";
                            AppLog.e(T.API, message);
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload();
                            payload.error = new AccountSocialError(AccountSocialErrorType.GENERIC_ERROR, message);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AccountPushSocialResponsePayload payload =
                                volleyErrorToAccountSocialResponsePayload(error.volleyError);
                        mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                    }
                }
        );
        request.disableRetries();
        addUnauthedRequest(request);
    }

    /**
     * Performs an HTTP POST call to the v1.1 /me/social-login/connect/ endpoint.  Upon receiving a
     * response (success or error) a {@link AccountAction#PUSHED_SOCIAL} action is dispatched with a
     * payload of type {@link AccountPushSocialResponsePayload}.
     *
     * {@link AccountPushSocialResponsePayload#isError()} can be used to check the request result.
     *
     * No HTTP POST call is made if the given parameter map is null or contains no entries.
     *
     * @param idToken       OpenID Connect Token (JWT) from the service the user is using to
     *                      authenticate their account.
     * @param service       Slug representing the service for the given token (e.g. google).
     */
    public void pushSocialConnect(@NonNull String idToken, @NonNull String service) {
        String url = WPCOMREST.me.social_login.connect.getUrlV1_1();

        Map<String, Object> params = new HashMap<>();
        params.put("id_token", idToken);
        params.put("service", service);
        params.put("client_id", mAppSecrets.getAppId());
        params.put("client_secret", mAppSecrets.getAppSecret());

        add(WPComGsonRequest.buildPostRequest(url, params, AccountSocialResponse.class,
                new Listener<AccountSocialResponse>() {
                    @Override
                    public void onResponse(AccountSocialResponse response) {
                        if (response != null) {
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload(response);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        } else {
                            String message = "Received empty response to /me/social-login/connect";
                            AppLog.e(T.API, message);
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload();
                            payload.error = new AccountSocialError(AccountSocialErrorType.GENERIC_ERROR, message);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        }
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload();
                        payload.error = new AccountSocialError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                    }
                }
        ));
    }

    /**
     * Performs an HTTP POST call to https://wordpress.com/wp-login.php with social-login-endpoint action.  Upon
     * receiving a response (success or error) a {@link AccountAction#PUSHED_SOCIAL} action is dispatched with a
     * payload of type {@link AccountPushSocialResponsePayload}.
     *
     * {@link AccountPushSocialResponsePayload#isError()} can be used to check the request result.
     *
     * No HTTP POST call is made if the given parameter map is null or contains no entries.
     *
     * @param idToken       OpenID Connect Token (JWT) from the service the user is using to
     *                      authenticate their account.
     * @param service       Slug representing the service for the given token (e.g. google).
     */
    public void pushSocialLogin(@NonNull String idToken, @NonNull String service) {
        String url = "https://wordpress.com/wp-login.php";

        Map<String, String> params = new HashMap<>();
        params.put("action", "social-login-endpoint");
        params.put("version", SOCIAL_LOGIN_ENDPOINT_VERSION);
        params.put("id_token", idToken);
        params.put("service", service);
        params.put("get_bearer_token", "true");
        params.put("client_id", mAppSecrets.getAppId());
        params.put("client_secret", mAppSecrets.getAppSecret());

        addUnauthedRequest(new AccountSocialRequest(url, params,
                new Listener<AccountSocialResponse>() {
                    @Override
                    public void onResponse(AccountSocialResponse response) {
                        if (response != null) {
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload(response);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        } else {
                            String message = "Received empty response to https://wordpress.com/wp-login.php"
                                             + "?action=social-login-endpoint";
                            AppLog.e(T.API, message);
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload();
                            payload.error = new AccountSocialError(AccountSocialErrorType.GENERIC_ERROR, message);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AccountPushSocialResponsePayload payload =
                                volleyErrorToAccountSocialResponsePayload(error.volleyError);
                        mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                    }
                }
        ));
    }

    /**
     * Performs an HTTP POST call to the v1.1 /users/social/new endpoint.  Upon receiving a response
     * (success or error) a {@link AccountAction#PUSHED_SOCIAL} action is dispatched with a payload
     * of type {@link AccountPushSocialResponsePayload}.
     *
     * {@link AccountPushSocialResponsePayload#isError()} can be used to check the request result.
     *
     * No HTTP POST call is made if the given parameter map is null or contains no entries.
     *
     * @param idToken       OpenID Connect Token (JWT) from the service the user is using to
     *                      authenticate their account.
     * @param service       Slug representing the service for the given token (e.g. google).
     */
    public void pushSocialSignup(@NonNull String idToken, @NonNull String service) {
        String url = WPCOMREST.users.social.new_.getUrlV1_1();

        Map<String, Object> params = new HashMap<>();
        params.put("id_token", idToken);
        params.put("service", service);
        params.put("signup_flow_name", "social");
        params.put("client_id", mAppSecrets.getAppId());
        params.put("client_secret", mAppSecrets.getAppSecret());

        add(WPComGsonRequest.buildPostRequest(url, params, AccountSocialResponse.class,
                new Listener<AccountSocialResponse>() {
                    @Override
                    public void onResponse(AccountSocialResponse response) {
                        if (response != null) {
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload(response);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        } else {
                            String message = "Received empty response to /users/social/new";
                            AppLog.e(T.API, message);
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload();
                            payload.error = new AccountSocialError(AccountSocialErrorType.GENERIC_ERROR, message);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        }
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload();
                        payload.error = new AccountSocialError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                    }
                }
        ));
    }

    /**
     * Performs an HTTP POST call to https://wordpress.com/wp-login.php with send-sms-code-endpoint action.  Upon
     * receiving a response (success or error) a {@link AccountAction#PUSHED_SOCIAL} action is dispatched with a
     * payload of type {@link AccountPushSocialResponsePayload}.
     *
     * {@link AccountPushSocialResponsePayload#isError()} can be used to check the request result.
     *
     * No HTTP POST call is made if the given parameter map is null or contains no entries.
     *
     * @param userId    WordPress.com user identification number
     * @param nonce     One-time-use token returned in {@link #pushSocialLogin(String, String)}} response
     */
    public void pushSocialSms(@NonNull String userId, @NonNull String nonce) {
        String url = "https://wordpress.com/wp-login.php";

        Map<String, String> params = new HashMap<>();
        params.put("action", "send-sms-code-endpoint");
        params.put("version", SOCIAL_SMS_ENDPOINT_VERSION);
        params.put("user_id", userId);
        params.put("two_step_nonce", nonce);
        params.put("client_id", mAppSecrets.getAppId());
        params.put("client_secret", mAppSecrets.getAppSecret());

        AccountSocialRequest request = new AccountSocialRequest(url, params,
                new Listener<AccountSocialResponse>() {
                    @Override
                    public void onResponse(AccountSocialResponse response) {
                        if (response != null) {
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload(response);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        } else {
                            String message = "Received empty response to https://wordpress.com/wp-login.php"
                                             + "?action=send-sms-code-endpoint";
                            AppLog.e(T.API, message);
                            AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload();
                            payload.error = new AccountSocialError(AccountSocialErrorType.GENERIC_ERROR, message);
                            mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AccountPushSocialResponsePayload payload =
                                volleyErrorToAccountSocialResponsePayload(error.volleyError);
                        mDispatcher.dispatch(AccountActionBuilder.newPushedSocialAction(payload));
                    }
                }
        );
        request.disableRetries();
        addUnauthedRequest(request);
    }

    /**
     * Performs an HTTP POST call to v1.1 /me/username endpoint.  Upon receiving a response
     * (success or error) a {@link AccountAction#PUSHED_USERNAME} action is dispatched with a
     * payload of type {@link AccountPushUsernameResponsePayload}.
     *
     * {@link AccountPushUsernameResponsePayload#isError()} can be used to check the request result.
     *
     * No HTTP POST call is made if the given parameter map is null or contains no entries.
     *
     * @param username      Alphanumeric string to save as unique WordPress.com account identifier
     * @param actionType    {@link AccountUsernameActionType} to take on WordPress.com site after username is changed
     */
    public void pushUsername(@NonNull final String username, @NonNull final AccountUsernameActionType actionType) {
        String url = WPCOMREST.me.username.getUrlV1_1();

        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("action", AccountUsernameActionType.getStringFromType(actionType));

        add(WPComGsonRequest.buildPostRequest(url, params,
                AccountBoolResponse.class,
                new Listener<AccountBoolResponse>() {
                    @Override
                    public void onResponse(AccountBoolResponse response) {
                        AccountPushUsernameResponsePayload payload = new AccountPushUsernameResponsePayload(username,
                                actionType);
                        mDispatcher.dispatch(AccountActionBuilder.newPushedUsernameAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AccountPushUsernameResponsePayload payload = new AccountPushUsernameResponsePayload(username,
                                actionType);
                        payload.error = new AccountUsernameError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newPushedUsernameAction(payload));
                    }
                }
        ));
    }

    public void newAccount(@NonNull String username, @NonNull String password, @NonNull String email,
                           final boolean dryRun) {
        String url = WPCOMREST.users.new_.getUrlV1();
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("email", email);
        body.put("validate", dryRun ? "1" : "0");
        body.put("client_id", mAppSecrets.getAppId());
        body.put("client_secret", mAppSecrets.getAppSecret());

        // backend needs locale set both the POST body _and_ the query param to fully set up the user's locale settings
        //  (messages language, followed blogs initialization)
        body.put("locale", getLocaleForUsersNewEndpoint());

        WPComGsonRequest<AccountBoolResponse> request = WPComGsonRequest.buildPostRequest(url, body,
                AccountBoolResponse.class,
                new Listener<AccountBoolResponse>() {
                    @Override
                    public void onResponse(AccountBoolResponse response) {
                        NewAccountResponsePayload payload = new NewAccountResponsePayload();
                        payload.dryRun = dryRun;
                        mDispatcher.dispatch(AccountActionBuilder.newCreatedNewAccountAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        NewAccountResponsePayload payload = volleyErrorToAccountResponsePayload(error.volleyError);
                        payload.dryRun = dryRun;
                        mDispatcher.dispatch(AccountActionBuilder.newCreatedNewAccountAction(payload));
                    }
                }
        );

        request.disableRetries();
        add(request);
    }

    /**
     * Performs an HTTP GET call to v1.2 /read/following/mine endpoint.  Upon receiving a response
     * (success or error) a {@link AccountAction#FETCHED_SUBSCRIPTIONS} action is dispatched with a
     * payload of type {@link SubscriptionsModel}.
     *
     * {@link SubscriptionsModel#isError()} can be used to check the request result.
     */
    public void fetchSubscriptions() {
        String url = WPCOMREST.read.following.mine.getUrlV1_2();
        final Map<String, String> params = new HashMap<>();
        params.put("meta", "site");
        final WPComGsonRequest<SubscriptionsResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                SubscriptionsResponse.class,
                new Listener<SubscriptionsResponse>() {
                    @Override
                    public void onResponse(SubscriptionsResponse response) {
                        if (response != null) {
                            List<SubscriptionModel> subscriptionArray = new ArrayList<>();

                            for (SubscriptionRestResponse subscriptionResponse : response.subscriptions) {
                                subscriptionArray.add(responseToSubscriptionModel(subscriptionResponse));
                            }

                            mDispatcher.dispatch(AccountActionBuilder.newFetchedSubscriptionsAction(
                                    new SubscriptionsModel(subscriptionArray)));
                        } else {
                            AppLog.e(T.API, "Received empty response from /read/following/mine");
                            SubscriptionsModel payload = new SubscriptionsModel();
                            payload.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(AccountActionBuilder.newFetchedSubscriptionsAction(payload));
                        }
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        SubscriptionsModel payload = new SubscriptionsModel();
                        payload.error = error;
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedSubscriptionsAction(payload));
                    }
                }
                                                                                                );
        add(request);
    }

    /**
     * Performs an HTTP POST call to v1.2 /read/site/$site/comment_email_subscriptions/$action endpoint.  Upon
     * receiving a response (success or error) a {@link AccountAction#UPDATED_SUBSCRIPTION} action
     * is dispatched with a payload of type {@link SubscriptionResponsePayload}.
     *
     * {@link SubscriptionResponsePayload#isError()} can be used to check the request result.
     *
     * @param siteId    Identification number of site to update comment email subscription
     * @param action    {@link SubscriptionAction} to add or remove comment email subscription
     */
    public void updateSubscriptionEmailComment(@NonNull String siteId, @NonNull SubscriptionAction action) {
        String actionLowerCase = action.toString();
        String url = WPCOMREST.read.site.item(siteId).comment_email_subscriptions.action(actionLowerCase).getUrlV1_2();
        final WPComGsonRequest<SubscriptionResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                SubscriptionResponse.class,
                new Listener<SubscriptionResponse>() {
                    @Override
                    public void onResponse(SubscriptionResponse response) {
                        SubscriptionResponsePayload payload = new SubscriptionResponsePayload(response.subscribed);
                        payload.type = SubscriptionType.EMAIL_COMMENT;
                        mDispatcher.dispatch(AccountActionBuilder.newUpdatedSubscriptionAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        SubscriptionResponsePayload payload = new SubscriptionResponsePayload();
                        payload.error = new SubscriptionError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newUpdatedSubscriptionAction(payload));
                    }
                }
        );
        add(request);
    }

    /**
     * Performs an HTTP POST call to v1.2 /read/site/$site/post_email_subscriptions/$action endpoint.  Upon
     * receiving a response (success or error) a {@link AccountAction#UPDATED_SUBSCRIPTION} action
     * is dispatched with a payload of type {@link SubscriptionResponsePayload}.
     *
     * {@link SubscriptionResponsePayload#isError()} can be used to check the request result.
     *
     * @param siteId    Identification number of site to update post email subscription
     * @param action    {@link SubscriptionAction} to add or remove post email subscription
     */
    public void updateSubscriptionEmailPost(@NonNull String siteId, @NonNull SubscriptionAction action) {
        String actionLowerCase = action.toString();
        String url = WPCOMREST.read.site.item(siteId).post_email_subscriptions.action(actionLowerCase).getUrlV1_2();
        final WPComGsonRequest<SubscriptionResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                SubscriptionResponse.class,
                new Listener<SubscriptionResponse>() {
                    @Override
                    public void onResponse(SubscriptionResponse response) {
                        SubscriptionResponsePayload payload = new SubscriptionResponsePayload(response.subscribed);
                        payload.type = SubscriptionType.EMAIL_POST;
                        mDispatcher.dispatch(AccountActionBuilder.newUpdatedSubscriptionAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        SubscriptionResponsePayload payload = new SubscriptionResponsePayload();
                        payload.error = new SubscriptionError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newUpdatedSubscriptionAction(payload));
                    }
                }
        );
        add(request);
    }

    /**
     * Performs an HTTP POST call to v1.2 /read/site/$site/post_email_subscriptions/update endpoint.  Upon
     * receiving a response (success or error) a {@link AccountAction#UPDATED_SUBSCRIPTION} action
     * is dispatched with a payload of type {@link SubscriptionResponsePayload}.
     *
     * {@link SubscriptionResponsePayload#isError()} can be used to check the request result.
     *
     * @param siteId        Identification number of site to update post email subscription
     * @param frequency     rate at which post emails are sent as {@link SubscriptionFrequency} value
     */
    public void updateSubscriptionEmailPostFrequency(@NonNull String siteId, @NonNull SubscriptionFrequency frequency) {
        String frequencyLowerCase = frequency.toString();
        String url = WPCOMREST.read.site.item(siteId).post_email_subscriptions.update.getUrlV1_2();
        Map<String, Object> body = new HashMap<>();
        body.put("delivery_frequency", frequencyLowerCase);
        final WPComGsonRequest<SubscriptionResponse> request = WPComGsonRequest.buildPostRequest(url, body,
                SubscriptionResponse.class,
                new Listener<SubscriptionResponse>() {
                    @Override
                    public void onResponse(SubscriptionResponse response) {
                        SubscriptionResponsePayload payload = new SubscriptionResponsePayload(response.subscribed);
                        payload.type = SubscriptionType.EMAIL_POST_FREQUENCY;
                        mDispatcher.dispatch(AccountActionBuilder.newUpdatedSubscriptionAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        SubscriptionResponsePayload payload = new SubscriptionResponsePayload();
                        payload.error = new SubscriptionError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newUpdatedSubscriptionAction(payload));
                    }
                }
        );
        add(request);
    }

    /**
     * Performs an HTTP POST call to v2 /read/sites/$site/notification-subscriptions/$action endpoint.  Upon
     * receiving a response (success or error) a {@link AccountAction#UPDATED_SUBSCRIPTION} action
     * is dispatched with a payload of type {@link SubscriptionResponsePayload}.
     *
     * {@link SubscriptionResponsePayload#isError()} can be used to check the request result.
     *
     * @param siteId    Identification number of site to update post notification subscription
     * @param action    {@link SubscriptionAction} to add or remove post notification subscription
     */
    public void updateSubscriptionNotificationPost(@NonNull String siteId, @NonNull SubscriptionAction action) {
        String actionLowerCase = action.toString();
        String url = WPCOMV2.read.sites.site(siteId).notification_subscriptions.action(actionLowerCase).getUrl();
        final WPComGsonRequest<SubscriptionResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                SubscriptionResponse.class,
                new Listener<SubscriptionResponse>() {
                    @Override
                    public void onResponse(SubscriptionResponse response) {
                        SubscriptionResponsePayload payload = new SubscriptionResponsePayload(response.subscribed);
                        payload.type = SubscriptionType.NOTIFICATION_POST;
                        mDispatcher.dispatch(AccountActionBuilder.newUpdatedSubscriptionAction(
                                payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        SubscriptionResponsePayload payload = new SubscriptionResponsePayload();
                        payload.error = new SubscriptionError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newUpdatedSubscriptionAction(payload));
                    }
                }
        );
        add(request);
    }

    /**
     * Performs an HTTP GET call to v1.1 /me/domain-contact-information/ endpoint.  Upon receiving a response
     * (success or error) a {@link AccountAction#FETCHED_DOMAIN_CONTACT} action is dispatched with a
     * payload of type {@link DomainContactPayload}.
     *
     * {@link DomainContactPayload#isError()} can be used to check the request result.
     */
    public void fetchDomainContact() {
        String url = WPCOMREST.me.domain_contact_information.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, DomainContactResponse.class,
                new Listener<DomainContactResponse>() {
                    @Override
                    public void onResponse(DomainContactResponse response) {
                        DomainContactPayload payload = new DomainContactPayload(responseToDomainContactModel(response));
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedDomainContactAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // Domain contact should always be available for a valid, authenticated user.
                        // Therefore, only GENERIC_ERROR is identified here.
                        DomainContactError contactError =
                                new DomainContactError(DomainContactErrorType.GENERIC_ERROR, error.message);
                        DomainContactPayload payload = new DomainContactPayload(contactError);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedDomainContactAction(payload));
                    }
                }));
    }

    /**
     * Performs an HTTP GET call to the v1.1 /users/$emailOrUsername/auth-options endpoint. Upon receiving
     * a response (success or error) a {@link AccountAction#FETCHED_AUTH_OPTIONS} action is dispatched
     * with a payload of type {@link AuthOptionsPayload}. {@link AuthOptionsPayload#isError()} can
     * be used to determine the result of the request.
     */
    public void fetchAuthOptions(@NonNull String emailOrUsername) {
        final String url = WPCOMREST.users.emailOrUsername(emailOrUsername).auth_options.getUrlV1_1();
        addUnauthedRequest(WPComGsonRequest.buildGetRequest(url, null, AuthOptionsResponse.class,
                new Listener<AuthOptionsResponse>() {
                    @Override
                    public void onResponse(AuthOptionsResponse response) {
                        AuthOptionsPayload payload = new AuthOptionsPayload();

                        try {
                            payload.isPasswordless = response.getPasswordless();
                            payload.isEmailVerified = response.getEmail_verified();
                        } catch (NullPointerException e) {
                            String message = "Received empty response to " + url;
                            AppLog.e(T.API, message, e);
                            payload.error = new AuthOptionsError(AuthOptionsErrorType.GENERIC_ERROR, message);
                        }

                        mDispatcher.dispatch(AccountActionBuilder.newFetchedAuthOptionsAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AuthOptionsPayload payload = new AuthOptionsPayload();
                        payload.error = new AuthOptionsError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newFetchedAuthOptionsAction(payload));
                    }
                }));
    }

    private SubscriptionModel responseToSubscriptionModel(SubscriptionRestResponse response) {
        SubscriptionModel subscription = new SubscriptionModel();
        subscription.setSubscriptionId(response.ID);
        subscription.setBlogId(response.blog_ID);
        subscription.setFeedId(response.feed_ID);
        subscription.setUrl(response.URL);

        if (response.delivery_methods != null) {
            if (response.delivery_methods.email != null) {
                subscription.setShouldEmailPosts(response.delivery_methods.email.send_posts);
                subscription.setShouldEmailComments(response.delivery_methods.email.send_comments);
                subscription.setEmailPostsFrequency(response.delivery_methods.email.post_delivery_frequency);
            }

            if (response.delivery_methods.notification != null) {
                subscription.setShouldNotifyPosts(response.delivery_methods.notification.send_posts);
            }
        }

        if (response.meta != null && response.meta.data != null && response.meta.data.site != null) {
            subscription.setBlogName(response.meta.data.site.name);
        }

        return subscription;
    }

    private String getLocaleForUsersNewEndpoint() {
        final Locale loc = LanguageUtils.getCurrentDeviceLanguage(mAppContext);
        final String lang = LanguageUtils.patchDeviceLanguageCode(loc.getLanguage());
        final String country = loc.getCountry().toLowerCase(Locale.ROOT); // backend needs it lowercase
        final String langMinusCountry = lang + '-' + country; // backend needs it separated by a minus

        // the `/users/new` endpoint expects only some locales to have a territory/Country, the rest being language only
        switch (langMinusCountry) {
            case "el-po":
            case "en-gb":
            case "es-mx":
            case "fr-be":
            case "fr-ca":
            case "fr-ch":
            case "pt-br":
            case "zh-cn":
            case "zh-tw":
                // return a lowercase, separated by a "minus" sign locale
                return langMinusCountry;
            default:
                // return the language part of the locale only
                return lang;
        }
    }

    public void isAvailable(@NonNull final String value, final IsAvailable type) {
        String url = "";
        switch (type) {
            case BLOG:
                url = WPCOMREST.is_available.blog.getUrlV0();
                break;
            case EMAIL:
                url = WPCOMREST.is_available.email.getUrlV0();
                break;
            case USERNAME:
                url = WPCOMREST.is_available.username.getUrlV0();
                break;
        }

        Map<String, String> params = new HashMap<>();
        params.put("q", value);

        WPComGsonRequest request = WPComGsonRequest.buildGetRequest(url, params, IsAvailableResponse.class,
                new Listener<IsAvailableResponse>() {
                    @Override
                    public void onResponse(IsAvailableResponse response) {
                        IsAvailableResponsePayload payload = new IsAvailableResponsePayload();
                        payload.value = value;
                        payload.type = type;

                        if (response == null) {
                            // The 'is-available' endpoints return either true or a JSON object representing an error
                            // The JsonObjectOrFalseDeserializer will deserialize true to null, so a null response
                            // actually means that there were no errors and the queried item (e.g., email) is available
                            payload.isAvailable = true;
                        } else {
                            if (response.error.equals("taken")) {
                                // We consider "taken" not to be an error, and we report that the item is unavailable
                                payload.isAvailable = false;
                            } else if (response.error.equals("invalid") && type.equals(IsAvailable.BLOG)
                                    && response.message.contains("reserved")) {
                                // Special case for /is-available/blog, which returns 'invalid' instead of 'taken'
                                // The messages from the server are not localized at the time of writing, but that may
                                // change in the future and cause this to be registered as a generic error
                                payload.isAvailable = false;
                            } else {
                                // Genuine error (probably a malformed item)
                                payload.error = new IsAvailableError(response.error, response.message);
                            }
                        }
                        mDispatcher.dispatch(AccountActionBuilder.newCheckedIsAvailableAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // We don't expect anything but server errors here - the API itself returns errors with a
                        // 200 status code, which will appear under Listener.onResponse instead
                        IsAvailableResponsePayload payload = new IsAvailableResponsePayload();
                        payload.value = value;
                        payload.type = type;

                        payload.error = new IsAvailableError(error.apiError, error.message);
                        mDispatcher.dispatch(AccountActionBuilder.newCheckedIsAvailableAction(payload));
                    }
                }
        );

        add(request);
    }

    private NewAccountResponsePayload volleyErrorToAccountResponsePayload(VolleyError error) {
        NewAccountResponsePayload payload = new NewAccountResponsePayload();
        payload.error = new NewUserError(NewUserErrorType.GENERIC_ERROR, "");
        if (error.networkResponse != null && error.networkResponse.data != null) {
            AppLog.e(T.API, new String(error.networkResponse.data));
            String jsonString = new String(error.networkResponse.data);
            try {
                JSONObject errorObj = new JSONObject(jsonString);
                payload.error.type = NewUserErrorType.fromString((String) errorObj.get("error"));
                payload.error.message = (String) errorObj.get("message");
            } catch (JSONException e) {
                // Do nothing (keep default error)
            }
        }
        return payload;
    }

    private AccountPushSocialResponsePayload volleyErrorToAccountSocialResponsePayload(VolleyError error) {
        AccountPushSocialResponsePayload payload = new AccountPushSocialResponsePayload();
        payload.error = new AccountSocialError(AccountSocialErrorType.GENERIC_ERROR, "");

        if (error.networkResponse != null && error.networkResponse.data != null) {
            AppLog.e(T.API, new String(error.networkResponse.data));

            try {
                String responseBody = new String(error.networkResponse.data, "UTF-8");
                JSONObject object = new JSONObject(responseBody);
                JSONObject data = object.getJSONObject("data");
                payload.error.nonce = data.optString("two_step_nonce");
                JSONArray errors = data.getJSONArray("errors");
                payload.error.type = AccountSocialErrorType.fromString(errors.getJSONObject(0).getString("code"));
                payload.error.message = errors.getJSONObject(0).getString("message");
            } catch (UnsupportedEncodingException | JSONException exception) {
                AppLog.e(T.API, "Unable to parse social error response: " + exception.getMessage());
            }
        }

        return payload;
    }

    private AccountModel responseToAccountModel(AccountResponse from) {
        AccountModel account = new AccountModel();
        account.setUserId(from.ID);
        account.setDisplayName(StringEscapeUtils.unescapeHtml4(from.display_name));
        account.setUserName(from.username);
        account.setEmail(from.email);
        account.setPrimarySiteId(from.primary_blog);
        account.setAvatarUrl(from.avatar_URL);
        account.setProfileUrl(from.profile_URL);
        account.setEmailVerified(from.email_verified);
        account.setDate(from.date);
        account.setSiteCount(from.site_count);
        account.setVisibleSiteCount(from.visible_site_count);
        account.setHasUnseenNotes(from.has_unseen_notes);
        return account;
    }

    private AccountModel responseToAccountSettingsModel(AccountSettingsResponse from) {
        AccountModel account = new AccountModel();
        account.setUserName(from.user_login);
        account.setDisplayName(StringEscapeUtils.unescapeHtml4(from.display_name));
        account.setFirstName(StringEscapeUtils.unescapeHtml4(from.first_name));
        account.setLastName(StringEscapeUtils.unescapeHtml4(from.last_name));
        account.setAboutMe(StringEscapeUtils.unescapeHtml4(from.description));
        account.setNewEmail(from.new_user_email);
        account.setAvatarUrl(from.avatar_URL);
        account.setPendingEmailChange(from.user_email_change_pending);
        account.setUsernameCanBeChanged(from.user_login_can_be_changed);
        account.setTracksOptOut(from.tracks_opt_out);
        account.setWebAddress(from.user_URL);
        account.setPrimarySiteId(from.primary_site_ID);
        return account;
    }

    public static boolean updateAccountModelFromPushSettingsResponse(AccountModel accountModel,
                Map<String, Object> from) {
        AccountModel old = new AccountModel();
        old.copyAccountAttributes(accountModel);
        old.setId(accountModel.getId());
        old.copyAccountSettingsAttributes(accountModel);
        if (from.containsKey("display_name")) {
            accountModel.setDisplayName(StringEscapeUtils.unescapeHtml4((String) from.get("display_name")));
        }
        if (from.containsKey("first_name")) {
            accountModel.setFirstName(StringEscapeUtils.unescapeHtml4((String) from.get("first_name")));
        }
        if (from.containsKey("last_name")) {
            accountModel.setLastName(StringEscapeUtils.unescapeHtml4((String) from.get("last_name")));
        }
        if (from.containsKey("description")) {
            accountModel.setAboutMe(StringEscapeUtils.unescapeHtml4((String) from.get("description")));
        }
        if (from.containsKey("user_email")) accountModel.setEmail((String) from.get("user_email"));
        if (from.containsKey("user_email_change_pending")) {
            accountModel.setPendingEmailChange((Boolean) from.get("user_email_change_pending"));
        }
        if (from.containsKey("tracks_opt_out")) {
            accountModel.setTracksOptOut((Boolean) from.get("tracks_opt_out"));
        }
        if (from.containsKey("new_user_email")) accountModel.setEmail((String) from.get("new_user_email"));
        if (from.containsKey("user_URL")) accountModel.setWebAddress((String) from.get("user_URL"));
        if (from.containsKey("primary_site_ID")) {
            accountModel.setPrimarySiteId(((Double) from.get("primary_site_ID")).longValue());
        }
        return !old.equals(accountModel);
    }

    private DomainContactModel responseToDomainContactModel(DomainContactResponse response) {
        String firstName = StringEscapeUtils.unescapeHtml4(response.getFirst_name());
        String lastName = StringEscapeUtils.unescapeHtml4(response.getLast_name());
        String organization = StringEscapeUtils.unescapeHtml4(response.getOrganization());
        String addressLine1 = response.getAddress_1();
        String addressLine2 = response.getAddress_2();
        String city = response.getCity();
        String state = response.getState();
        String postalCode = response.getPostal_code();
        String countryCode = response.getCountry_code();
        String phone = response.getPhone();
        String fax = response.getFax();
        String email = response.getEmail();
        return new DomainContactModel(firstName, lastName, organization, addressLine1, addressLine2, postalCode, city,
                state, countryCode, email, phone, fax);
    }
}
