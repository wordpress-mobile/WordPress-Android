package org.wordpress.android.fluxc.store;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.VolleyError;
import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.action.AuthenticationAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.DomainContactModel;
import org.wordpress.android.fluxc.model.SubscriptionModel;
import org.wordpress.android.fluxc.model.SubscriptionsModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryResultPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountFetchUsernameSuggestionsResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountPushSettingsResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountPushSocialResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountPushUsernameResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.AccountRestPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.FetchAuthOptionsResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.DomainContactPayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.IsAvailable;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.IsAvailableResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.NewAccountResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator.AuthEmailResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator.Token;
import org.wordpress.android.fluxc.persistence.AccountSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * In-memory based and persisted in SQLite.
 */
@Singleton
public class AccountStore extends Store {
    // Payloads
    public static class AuthenticatePayload extends Payload<BaseNetworkError> {
        public String username;
        public String password;
        public String twoStepCode;
        public boolean shouldSendTwoStepSms;
        public Action nextAction;
        public AuthenticatePayload(@NonNull String username, @NonNull String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class AuthenticateErrorPayload extends Payload<AuthenticationError> {
        public AuthenticateErrorPayload(@NonNull AuthenticationError error) {
            this.error = error;
        }
        public AuthenticateErrorPayload(@NonNull AuthenticationErrorType errorType) {
            this.error = new AuthenticationError(errorType, "");
        }
    }

    public static class AuthEmailPayload extends Payload<BaseNetworkError> {
        public AuthEmailPayloadScheme scheme;
        public AuthEmailPayloadFlow flow;
        public AuthEmailPayloadSource source;
        public String emailOrUsername;
        public String signupFlowName;
        public boolean isSignup;

        public AuthEmailPayload(String emailOrUsername, boolean isSignup, AuthEmailPayloadFlow flow,
                                AuthEmailPayloadSource source, AuthEmailPayloadScheme scheme) {
            this.emailOrUsername = emailOrUsername;
            this.isSignup = isSignup;
            this.flow = flow;
            this.source = source;
            this.scheme = scheme;
        }

        public AuthEmailPayload(String emailOrUsername, boolean isSignup, AuthEmailPayloadFlow flow,
                                AuthEmailPayloadSource source) {
            this(emailOrUsername, isSignup, flow, source, null);
        }
    }

    public enum AuthEmailPayloadScheme {
        WORDPRESS("wordpress"),
        WOOCOMMERCE("woocommerce");

        private final String mString;

        AuthEmailPayloadScheme(final String s) {
            mString = s;
        }

        @Override
        public String toString() {
            return mString;
        }
    }

    public enum AuthEmailPayloadFlow {
        JETPACK("jetpack");

        private final String mString;

        AuthEmailPayloadFlow(final String s) {
            mString = s;
        }

        @Override
        public String toString() {
            return mString;
        }
    }

    public enum AuthEmailPayloadSource {
        NOTIFICATIONS("notifications"),
        STATS("stats");

        private final String mString;

        AuthEmailPayloadSource(final String s) {
            mString = s;
        }

        @Override
        public String toString() {
            return mString;
        }
    }

    public static class FetchUsernameSuggestionsPayload extends Payload<BaseNetworkError> {
        public String name;
        public FetchUsernameSuggestionsPayload(@NonNull String name) {
            this.name = name;
        }
    }

    public static class PushAccountSettingsPayload extends Payload<BaseNetworkError> {
        public Map<String, Object> params;
        public PushAccountSettingsPayload() {
        }
    }

    public static class PushSocialPayload extends Payload<BaseNetworkError> {
        public String idToken;
        public String service;
        public PushSocialPayload(@NonNull String idToken, @NonNull String service) {
            this.idToken = idToken;
            this.service = service;
        }
    }

    public static class PushSocialAuthPayload extends Payload<BaseNetworkError> {
        public String code;
        public String nonce;
        public String type;
        public String userId;
        public PushSocialAuthPayload(@NonNull String userId, @NonNull String type, @NonNull String nonce,
                                     @NonNull String code) {
            this.userId = userId;
            this.type = type;
            this.nonce = nonce;
            this.code = code;
        }
    }

    public static class PushSocialSmsPayload extends Payload<BaseNetworkError> {
        public String nonce;
        public String userId;
        public PushSocialSmsPayload(@NonNull String userId, @NonNull String nonce) {
            this.userId = userId;
            this.nonce = nonce;
        }
    }

    public static class PushUsernamePayload extends Payload<BaseNetworkError> {
        public AccountUsernameActionType actionType;
        public String username;
        public PushUsernamePayload(@NonNull String username, @NonNull AccountUsernameActionType actionType) {
            this.username = username;
            this.actionType = actionType;
        }
    }

    public static class NewAccountPayload extends Payload<BaseNetworkError> {
        public String username;
        public String password;
        public String email;
        public boolean dryRun;
        public NewAccountPayload(@NonNull String username, @NonNull String password, @NonNull String email,
                                 boolean dryRun) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.dryRun = dryRun;
        }
    }

    public static class UpdateTokenPayload extends Payload<BaseNetworkError> {
        public UpdateTokenPayload(String token) {
            this.token = token;
        }

        public String token;
    }

    public static class AddOrDeleteSubscriptionPayload extends Payload<BaseNetworkError> {
        public String site;
        public SubscriptionAction action;
        public AddOrDeleteSubscriptionPayload(@NonNull String site, @NonNull SubscriptionAction action) {
            this.site = site;
            this.action = action;
        }
        public enum SubscriptionAction {
            DELETE("delete"),
            NEW("new");

            private final String mString;

            SubscriptionAction(final String s) {
                mString = s;
            }

            @Override
            public String toString() {
                return mString;
            }
        }
    }

    public static class FetchAuthOptionsPayload extends Payload<BaseNetworkError> {
        public FetchAuthOptionsPayload(String emailOrUsername) {
            this.emailOrUsername = emailOrUsername;
        }

        public String emailOrUsername;
    }

    public static class UpdateSubscriptionPayload extends Payload<BaseNetworkError> {
        public String site;
        public SubscriptionFrequency frequency;
        public UpdateSubscriptionPayload(@NonNull String site, @NonNull SubscriptionFrequency frequency) {
            this.site = site;
            this.frequency = frequency;
        }
        public enum SubscriptionFrequency {
            DAILY("daily"),
            INSTANTLY("instantly"),
            WEEKLY("weekly");

            private final String mString;

            SubscriptionFrequency(final String s) {
                mString = s;
            }

            @Override
            public String toString() {
                return mString;
            }
        }
    }

    public static class SubscriptionResponsePayload extends Payload<SubscriptionError> {
        public SubscriptionType type;
        public boolean isSubscribed;
        public SubscriptionResponsePayload() {
        }
        public SubscriptionResponsePayload(boolean isSubscribed) {
            this.isSubscribed = isSubscribed;
        }
    }

    public enum SubscriptionType {
        EMAIL_COMMENT,
        EMAIL_POST,
        EMAIL_POST_FREQUENCY,
        NOTIFICATION_POST
    }

    /**
     * Error for any of these methods:
     * {@link AccountRestClient#updateSubscriptionEmailComment(String,
     *      AddOrDeleteSubscriptionPayload.SubscriptionAction)}
     * {@link AccountRestClient#updateSubscriptionEmailPost(String,
     *      AddOrDeleteSubscriptionPayload.SubscriptionAction)}
     * {@link AccountRestClient#updateSubscriptionEmailPostFrequency(String,
     *      UpdateSubscriptionPayload.SubscriptionFrequency)}
     * {@link AccountRestClient#updateSubscriptionNotificationPost(String,
     *      AddOrDeleteSubscriptionPayload.SubscriptionAction)}
     */
    public static class SubscriptionError implements OnChangedError {
        public SubscriptionErrorType type;
        public String message;

        public SubscriptionError(@NonNull String type, @NonNull String message) {
            this(SubscriptionErrorType.fromString(type), message);
        }

        public SubscriptionError(SubscriptionErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public enum SubscriptionErrorType {
        ALREADY_SUBSCRIBED,
        AUTHORIZATION_REQUIRED,
        EMAIL_ADDRESS_MISSING,
        REST_CANNOT_VIEW,
        GENERIC_ERROR;

        public static SubscriptionErrorType fromString(final String string) {
            if (!TextUtils.isEmpty(string)) {
                for (SubscriptionErrorType type : SubscriptionErrorType.values()) {
                    if (string.equalsIgnoreCase(type.name())) {
                        return type;
                    }
                }
            }

            return GENERIC_ERROR;
        }
    }

    /**
     * Error for {@link AccountRestClient#fetchSubscriptions()} method.
     */
    public static class SubscriptionsError implements OnChangedError {
        public String message;

        public SubscriptionsError(BaseNetworkError error) {
            this.message = error.message;
        }
    }

    // OnChanged Events
    public static class OnAccountChanged extends OnChanged<AccountError> {
        public boolean accountInfosChanged;
        public AccountAction causeOfChange;
    }

    public static class OnAuthenticationChanged extends OnChanged<AuthenticationError> {
        public String userName;
        public boolean createdAccount;
    }

    public static class OnSocialChanged extends OnChanged<AccountSocialError> {
        public List<String> twoStepTypes;
        public String nonce;
        public String nonceAuthenticator;
        public String nonceBackup;
        public String nonceSms;
        public String notificationSent;
        public String phoneNumber;
        public String userId;
        public boolean requiresTwoStepAuth;

        public OnSocialChanged() {
        }

        public OnSocialChanged(@NonNull AccountPushSocialResponsePayload payload) {
            this.twoStepTypes = payload.twoStepTypes;
            this.nonce = payload.twoStepNonce;
            this.nonceAuthenticator = payload.twoStepNonceAuthenticator;
            this.nonceBackup = payload.twoStepNonceBackup;
            this.nonceSms = payload.twoStepNonceSms;
            this.notificationSent = payload.twoStepNotificationSent;
            this.phoneNumber = payload.phoneNumber;
            this.userId = payload.userId;
        }
    }

    public static class OnUsernameChanged extends OnChanged<AccountUsernameError> {
        public AccountUsernameActionType type;
        public String username;
    }

    public static class OnUsernameSuggestionsFetched extends OnChanged<AccountFetchUsernameSuggestionsError> {
        public List<String> suggestions;
    }

    public static class OnDomainContactFetched extends OnChanged<DomainContactError> {
        @Nullable public DomainContactModel contactModel;

        public OnDomainContactFetched(@Nullable DomainContactModel contactModel, @Nullable DomainContactError error) {
            this.contactModel = contactModel;
            this.error = error;
        }
    }

    public static class OnAuthOptionsFetched extends OnChanged<AuthOptionsError> {
        public boolean isPasswordless;
        public boolean isEmailVerified;
    }

    public static class OnDiscoveryResponse extends OnChanged<DiscoveryError> {
        public String xmlRpcEndpoint;
        public String wpRestEndpoint;
        public String failedEndpoint;
    }

    public static class OnNewUserCreated extends OnChanged<NewUserError> {
        public boolean dryRun;
    }

    public static class OnAvailabilityChecked extends OnChanged<IsAvailableError> {
        public IsAvailable type;
        public String value;
        public boolean isAvailable;

        public OnAvailabilityChecked(IsAvailable type, String value, boolean isAvailable) {
            this.type = type;
            this.value = value;
            this.isAvailable = isAvailable;
        }
    }

    public static class OnAuthEmailSent extends OnChanged<AuthEmailError> {
        public final boolean isSignup;

        public OnAuthEmailSent(boolean isSignup) {
            this.isSignup = isSignup;
        }
    }

    public static class AuthenticationError implements OnChangedError {
        public AuthenticationErrorType type;
        public String message;
        public AuthenticationError(AuthenticationErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class OnSubscriptionsChanged extends OnChanged<SubscriptionsError> {
    }

    public static class OnSubscriptionUpdated extends OnChanged<SubscriptionError> {
        public SubscriptionType type;
        public boolean subscribed;
        public OnSubscriptionUpdated() {
        }
    }

    // Enums
    public enum AuthenticationErrorType {
        // From response's "error" field
        ACCESS_DENIED,
        AUTHORIZATION_REQUIRED,
        INVALID_CLIENT,
        INVALID_GRANT,
        INVALID_OTP,
        INVALID_REQUEST,
        INVALID_TOKEN,
        NEEDS_2FA,
        UNSUPPORTED_GRANT_TYPE,
        UNSUPPORTED_RESPONSE_TYPE,
        UNKNOWN_TOKEN,
        EMAIL_LOGIN_NOT_ALLOWED,

        // From response's "message" field - sadly... (be careful with i18n)
        INCORRECT_USERNAME_OR_PASSWORD,

        // .org specifics
        INVALID_SSL_CERTIFICATE,
        HTTP_AUTH_ERROR,
        NOT_AUTHENTICATED,

        // Generic error
        GENERIC_ERROR;

        public static AuthenticationErrorType fromString(String string) {
            if (string != null) {
                for (AuthenticationErrorType v : AuthenticationErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public static class AccountError implements OnChangedError {
        public AccountErrorType type;
        public String message;
        public AccountError(AccountErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    public enum AccountErrorType {
        ACCOUNT_FETCH_ERROR,
        SETTINGS_FETCH_GENERIC_ERROR,
        SETTINGS_FETCH_REAUTHORIZATION_REQUIRED_ERROR,
        SETTINGS_POST_ERROR,
        SEND_VERIFICATION_EMAIL_ERROR,
        GENERIC_ERROR
    }

    public static class IsAvailableError implements OnChangedError {
        public IsAvailableErrorType type;
        public String message;

        public IsAvailableError(@NonNull String type, @NonNull String message) {
            this.type = IsAvailableErrorType.fromString(type);
            this.message = message;
        }
    }

    public enum IsAvailableErrorType {
        INVALID,
        GENERIC_ERROR;

        public static IsAvailableErrorType fromString(String string) {
            if (string != null) {
                for (IsAvailableErrorType v : IsAvailableErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public static class AccountSocialError implements OnChangedError {
        public AccountSocialErrorType type;
        public String message;
        public String nonce;

        public AccountSocialError(@NonNull String type, @NonNull String message) {
            this.type = AccountSocialErrorType.fromString(type);
            this.message = message;
        }

        public AccountSocialError(@NonNull AccountSocialErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    public enum AccountSocialErrorType {
        INVALID_TOKEN,
        INVALID_TWO_STEP_CODE,
        INVALID_TWO_STEP_NONCE,
        NO_PHONE_NUMBER_FOR_ACCOUNT,
        SMS_AUTHENTICATION_UNAVAILABLE,
        SMS_CODE_THROTTLED,
        TWO_STEP_ENABLED,
        UNABLE_CONNECT,
        UNKNOWN_USER,
        USER_ALREADY_ASSOCIATED,
        USER_EXISTS,
        GENERIC_ERROR;

        public static AccountSocialErrorType fromString(String string) {
            if (string != null) {
                string = string.replace("2FA_enabled", "two_step_enabled");
                for (AccountSocialErrorType type : AccountSocialErrorType.values()) {
                    if (string.equalsIgnoreCase(type.name())) {
                        return type;
                    }
                }
            }

            return GENERIC_ERROR;
        }
    }

    public enum AccountUsernameActionType {
        CREATE_NEW_SITE_AND_ADDRESS, // site and address remain unchanged plus empty site created with new username
        KEEP_OLD_SITE_AND_ADDRESS, // site and address remain unchanged; only username is changed
        RENAME_SITE_AND_DISCARD_OLD_ADDRESS, // site renamed and old site address discarded
        RENAME_SITE_AND_KEEP_OLD_ADDRESS; // site renamed and new empty site belonging to user created with old address

        public static String getStringFromType(AccountUsernameActionType type) {
            switch (type) {
                case CREATE_NEW_SITE_AND_ADDRESS:
                    return "new";
                case KEEP_OLD_SITE_AND_ADDRESS:
                    return "none";
                case RENAME_SITE_AND_DISCARD_OLD_ADDRESS:
                    return "rename_discard";
                case RENAME_SITE_AND_KEEP_OLD_ADDRESS:
                    return "rename_keep";
                default:
                    return "";
            }
        }
    }

    public static class AccountUsernameError implements OnChangedError {
        public AccountUsernameErrorType type;
        public String message;

        public AccountUsernameError(@NonNull String type, @NonNull String message) {
            this.type = AccountUsernameErrorType.fromString(type);
            this.message = message;
        }
    }

    public enum AccountUsernameErrorType {
        INVALID_ACTION,
        INVALID_INPUT,
        GENERIC_ERROR;

        public static AccountUsernameErrorType fromString(String string) {
            if (string != null) {
                for (AccountUsernameErrorType type : AccountUsernameErrorType.values()) {
                    if (string.equalsIgnoreCase(type.name())) {
                        return type;
                    }
                }
            }

            return GENERIC_ERROR;
        }
    }

    public static class AccountFetchUsernameSuggestionsError implements OnChangedError {
        public AccountFetchUsernameSuggestionsErrorType type;
        public String message;

        public AccountFetchUsernameSuggestionsError(@NonNull String type, @NonNull String message) {
            this.type = AccountFetchUsernameSuggestionsErrorType.fromString(type);
            this.message = message;
        }
    }

    public enum AccountFetchUsernameSuggestionsErrorType {
        REST_MISSING_CALLBACK_PARAM,
        REST_NO_NAME,
        GENERIC_ERROR;

        public static AccountFetchUsernameSuggestionsErrorType fromString(String string) {
            if (string != null) {
                for (AccountFetchUsernameSuggestionsErrorType type
                        : AccountFetchUsernameSuggestionsErrorType.values()) {
                    if (string.equalsIgnoreCase(type.name())) {
                        return type;
                    }
                }
            }

            return GENERIC_ERROR;
        }
    }

    public static class DomainContactError implements OnChangedError {
        @NonNull public DomainContactErrorType type;
        @Nullable public String message;

        public DomainContactError(@NonNull DomainContactErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }
    }

    public enum DomainContactErrorType {
        GENERIC_ERROR;
    }

    public static class AuthOptionsError implements OnChangedError {
        @NonNull public AuthOptionsErrorType type;
        @Nullable public String message;

        public AuthOptionsError(@Nullable String type, @Nullable String message) {
            this.type = AuthOptionsErrorType.fromString(type);
            this.message = message;
        }

        public AuthOptionsError(@NonNull AuthOptionsErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }
    }

    public enum AuthOptionsErrorType {
        UNKNOWN_USER,
        EMAIL_LOGIN_NOT_ALLOWED,
        GENERIC_ERROR;

        public static AuthOptionsErrorType fromString(String string) {
            if (string != null) {
                for (AuthOptionsErrorType v : AuthOptionsErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }

            return GENERIC_ERROR;
        }
    }

    public static class AuthEmailError implements OnChangedError {
        public AuthEmailErrorType type;
        public String message;

        public AuthEmailError(AuthEmailErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }

        public AuthEmailError(@NonNull String type, @NonNull String message) {
            this.type = AuthEmailErrorType.fromString(type);
            this.message = message;
        }
    }

    public enum AuthEmailErrorType {
        INVALID_EMAIL,
        USER_EXISTS,
        UNSUCCESSFUL,
        GENERIC_ERROR;

        public static AuthEmailErrorType fromString(String string) {
            if (string != null) {
                for (AuthEmailErrorType v : AuthEmailErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public static class NewUserError implements OnChangedError {
        public NewUserErrorType type;
        public String message;
        public NewUserError(NewUserErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    public enum NewUserErrorType {
        USERNAME_ONLY_LOWERCASE_LETTERS_AND_NUMBERS,
        USERNAME_REQUIRED,
        USERNAME_NOT_ALLOWED,
        USERNAME_MUST_BE_AT_LEAST_FOUR_CHARACTERS,
        USERNAME_CONTAINS_INVALID_CHARACTERS,
        USERNAME_MUST_INCLUDE_LETTERS,
        USERNAME_EXISTS,
        USERNAME_RESERVED_BUT_MAY_BE_AVAILABLE,
        USERNAME_INVALID,
        PASSWORD_INVALID,
        EMAIL_CANT_BE_USED_TO_SIGNUP,
        EMAIL_INVALID,
        EMAIL_NOT_ALLOWED,
        EMAIL_EXISTS,
        EMAIL_RESERVED,
        GENERIC_ERROR;

        public static NewUserErrorType fromString(String string) {
            if (string != null) {
                for (NewUserErrorType v : NewUserErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    // Fields
    private AccountRestClient mAccountRestClient;
    private Authenticator mAuthenticator;
    private AccountModel mAccount;
    private AccessToken mAccessToken;
    private SelfHostedEndpointFinder mSelfHostedEndpointFinder;

    @Inject
    public AccountStore(Dispatcher dispatcher, AccountRestClient accountRestClient,
                        SelfHostedEndpointFinder selfHostedEndpointFinder, Authenticator authenticator,
                        AccessToken accessToken) {
        super(dispatcher);
        mAuthenticator = authenticator;
        mAccountRestClient = accountRestClient;
        mSelfHostedEndpointFinder = selfHostedEndpointFinder;
        mAccount = loadAccount();
        mAccessToken = accessToken;
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "AccountStore onRegister");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (actionType instanceof AccountAction) {
            onAccountAction((AccountAction) actionType, action.getPayload());
        }
        if (actionType instanceof AuthenticationAction) {
            onAuthenticationAction((AuthenticationAction) actionType, action.getPayload());
        }
    }

    private void onAccountAction(AccountAction actionType, Object payload) {
        switch (actionType) {
            case FETCH_ACCOUNT:
                mAccountRestClient.fetchAccount();
                break;
            case FETCH_SETTINGS:
                mAccountRestClient.fetchAccountSettings();
                break;
            case FETCH_USERNAME_SUGGESTIONS:
                createFetchUsernameSuggestions((FetchUsernameSuggestionsPayload) payload);
                break;
            case SEND_VERIFICATION_EMAIL:
                mAccountRestClient.sendVerificationEmail();
                break;
            case PUSH_SETTINGS:
                mAccountRestClient.pushAccountSettings(((PushAccountSettingsPayload) payload).params);
                break;
            case PUSH_SOCIAL_AUTH:
                createPushSocialAuth((PushSocialAuthPayload) payload);
                break;
            case PUSH_SOCIAL_CONNECT:
                createPushSocialConnect((PushSocialPayload) payload);
                break;
            case PUSH_SOCIAL_LOGIN:
                createPushSocialLogin((PushSocialPayload) payload);
                break;
            case PUSH_SOCIAL_SIGNUP:
                createPushSocialSignup((PushSocialPayload) payload);
                break;
            case PUSH_SOCIAL_SMS:
                createPushSocialSms((PushSocialSmsPayload) payload);
                break;
            case PUSH_USERNAME:
                createPushUsername((PushUsernamePayload) payload);
                break;
            case UPDATE_ACCOUNT:
                updateDefaultAccount((AccountModel) payload, AccountAction.UPDATE_ACCOUNT);
                break;
            case UPDATE_ACCESS_TOKEN:
                updateToken((UpdateTokenPayload) payload);
                break;
            case SIGN_OUT:
                signOut();
                break;
            case CREATE_NEW_ACCOUNT:
                createNewAccount((NewAccountPayload) payload);
                break;
            case CREATED_NEW_ACCOUNT:
                handleNewAccountCreated((NewAccountResponsePayload) payload);
                break;
            case PUSHED_SETTINGS:
                handlePushSettingsCompleted((AccountPushSettingsResponsePayload) payload);
                break;
            case PUSHED_SOCIAL:
                handlePushSocialCompleted((AccountPushSocialResponsePayload) payload);
                break;
            case PUSHED_USERNAME:
                handlePushUsernameCompleted((AccountPushUsernameResponsePayload) payload);
                break;
            case FETCHED_SETTINGS:
                handleFetchSettingsCompleted((AccountRestPayload) payload);
                break;
            case FETCHED_USERNAME_SUGGESTIONS:
                handleFetchUsernameSuggestionsCompleted((AccountFetchUsernameSuggestionsResponsePayload) payload);
                break;
            case FETCHED_ACCOUNT:
                handleFetchAccountCompleted((AccountRestPayload) payload);
                break;
            case SENT_VERIFICATION_EMAIL:
                handleSentVerificationEmail((NewAccountResponsePayload) payload);
                break;
            case IS_AVAILABLE_BLOG:
                mAccountRestClient.isAvailable((String) payload, IsAvailable.BLOG);
                break;
            case IS_AVAILABLE_EMAIL:
                mAccountRestClient.isAvailable((String) payload, IsAvailable.EMAIL);
                break;
            case IS_AVAILABLE_USERNAME:
                mAccountRestClient.isAvailable((String) payload, IsAvailable.USERNAME);
                break;
            case CHECKED_IS_AVAILABLE:
                handleCheckedIsAvailable((IsAvailableResponsePayload) payload);
                break;
            case FETCH_SUBSCRIPTIONS:
                mAccountRestClient.fetchSubscriptions();
                break;
            case FETCHED_SUBSCRIPTIONS:
                updateSubscriptions((SubscriptionsModel) payload);
                break;
            case UPDATE_SUBSCRIPTION_EMAIL_COMMENT:
                createAddOrDeleteSubscriptionEmailComment((AddOrDeleteSubscriptionPayload) payload);
                break;
            case UPDATE_SUBSCRIPTION_EMAIL_POST:
                createAddOrDeleteSubscriptionEmailPost((AddOrDeleteSubscriptionPayload) payload);
                break;
            case UPDATE_SUBSCRIPTION_EMAIL_POST_FREQUENCY:
                createUpdateSubscriptionEmailPostFrequency((UpdateSubscriptionPayload) payload);
                break;
            case UPDATE_SUBSCRIPTION_NOTIFICATION_POST:
                createAddOrDeleteSubscriptionNotificationPost((AddOrDeleteSubscriptionPayload) payload);
                break;
            case UPDATED_SUBSCRIPTION:
                handleUpdatedSubscription((SubscriptionResponsePayload) payload);
                break;
            case FETCH_DOMAIN_CONTACT:
                mAccountRestClient.fetchDomainContact();
                break;
            case FETCHED_DOMAIN_CONTACT:
                handleFetchedDomainContact((DomainContactPayload) payload);
                break;
            case FETCH_AUTH_OPTIONS:
                createFetchAuthOptions((FetchAuthOptionsPayload) payload);
                break;
            case FETCHED_AUTH_OPTIONS:
                handleFetchedAuthOptions((FetchAuthOptionsResponsePayload) payload);
                break;
        }
    }

    private void onAuthenticationAction(AuthenticationAction actionType, Object payload) {
        switch (actionType) {
            case AUTHENTICATE:
                authenticate((AuthenticatePayload) payload);
                break;
            case AUTHENTICATE_ERROR:
                handleAuthenticateError((AuthenticateErrorPayload) payload);
                break;
            case DISCOVER_ENDPOINT:
                discoverEndPoint((String) payload);
                break;
            case DISCOVERY_RESULT:
                discoveryResult((DiscoveryResultPayload) payload);
                break;
            case SEND_AUTH_EMAIL:
                mAuthenticator.sendAuthEmail((AuthEmailPayload) payload);
                break;
            case SENT_AUTH_EMAIL:
                handleSentAuthEmail((AuthEmailResponsePayload) payload);
                break;
        }
    }

    private void handleAuthenticateError(AuthenticateErrorPayload payload) {
        if (payload.error.type == AuthenticationErrorType.INVALID_TOKEN) {
            clearAccountAndAccessToken();
        }
        OnAuthenticationChanged event = new OnAuthenticationChanged();
        event.error = payload.error;
        emitChange(event);
    }

    private void discoverEndPoint(String payload) {
        mSelfHostedEndpointFinder.findEndpoint(payload);
    }

    private void discoveryResult(DiscoveryResultPayload payload) {
        OnDiscoveryResponse discoveryResponse = new OnDiscoveryResponse();
        if (payload.isError()) {
            discoveryResponse.error = payload.error;
            discoveryResponse.failedEndpoint = payload.failedEndpoint;
        } else {
            discoveryResponse.xmlRpcEndpoint = payload.xmlRpcEndpoint;
            discoveryResponse.wpRestEndpoint = payload.wpRestEndpoint;
        }
        emitChange(discoveryResponse);
    }

    private void handleFetchAccountCompleted(AccountRestPayload payload) {
        if (!hasAccessToken()) {
            emitAccountChangeError(AccountErrorType.ACCOUNT_FETCH_ERROR);
            return;
        }
        if (!checkError(payload, "Error fetching Account via REST API (/me)")) {
            mAccount.copyAccountAttributes(payload.account);
            updateDefaultAccount(mAccount, AccountAction.FETCH_ACCOUNT);
        } else {
            emitAccountChangeError(AccountErrorType.ACCOUNT_FETCH_ERROR);
        }
    }

    private void handleFetchSettingsCompleted(AccountRestPayload payload) {
        if (!hasAccessToken()) {
            emitAccountChangeError(AccountErrorType.SETTINGS_FETCH_GENERIC_ERROR);
            return;
        }
        if (!checkError(payload, "Error fetching Account Settings via REST API (/me/settings)")) {
            mAccount.copyAccountSettingsAttributes(payload.account);
            updateDefaultAccount(mAccount, AccountAction.FETCH_SETTINGS);
        } else {
            OnAccountChanged accountChanged = new OnAccountChanged();
            accountChanged.causeOfChange = AccountAction.FETCH_SETTINGS;

            AccountErrorType errorType;
            if (payload.error.apiError.equals("reauthorization_required")) {
                // This error will always occur for 2FA accounts when using a non-production WordPress.com OAuth client.
                // Essentially, some APIs around account management are disabled in those cases for security reasons.
                // The error is a bit generic from the server-side - it essentially means the user isn't privileged to
                // do the action and needs to reauthorize. For bearer token-based login, there is no escalation of
                // privileges possible, so the request just fails at that point.
                errorType = AccountErrorType.SETTINGS_FETCH_REAUTHORIZATION_REQUIRED_ERROR;
            } else {
                errorType = AccountErrorType.SETTINGS_FETCH_GENERIC_ERROR;
            }
            accountChanged.error = new AccountError(errorType, payload.error.message);

            emitChange(accountChanged);
        }
    }

    private void handleFetchUsernameSuggestionsCompleted(AccountFetchUsernameSuggestionsResponsePayload payload) {
        OnUsernameSuggestionsFetched event = new OnUsernameSuggestionsFetched();
        event.error = payload.error;
        event.suggestions = payload.suggestions;
        emitChange(event);
    }

    private void handleSentVerificationEmail(NewAccountResponsePayload payload) {
        OnAccountChanged accountChanged = new OnAccountChanged();
        accountChanged.causeOfChange = AccountAction.SEND_VERIFICATION_EMAIL;
        if (payload.isError()) {
            accountChanged.error = new AccountError(AccountErrorType.SEND_VERIFICATION_EMAIL_ERROR, "");
        }
        emitChange(accountChanged);
    }

    private void handlePushSettingsCompleted(AccountPushSettingsResponsePayload payload) {
        if (!hasAccessToken()) {
            emitAccountChangeError(AccountErrorType.SETTINGS_POST_ERROR);
            return;
        }
        if (!payload.isError()) {
            boolean updated = AccountRestClient.updateAccountModelFromPushSettingsResponse(mAccount, payload.settings);
            if (updated) {
                updateDefaultAccount(mAccount, AccountAction.PUSH_SETTINGS);
            } else {
                OnAccountChanged accountChanged = new OnAccountChanged();
                accountChanged.causeOfChange = AccountAction.PUSH_SETTINGS;
                accountChanged.accountInfosChanged = false;
                emitChange(accountChanged);
            }
        } else {
            if (payload.error != null) {
                OnAccountChanged accountChanged = new OnAccountChanged();
                accountChanged.error = new AccountError(AccountErrorType.SETTINGS_POST_ERROR, payload.error.message);
                emitChange(accountChanged);
            } else {
                emitAccountChangeError(AccountErrorType.SETTINGS_POST_ERROR);
            }
        }
    }

    private void handlePushSocialCompleted(AccountPushSocialResponsePayload payload) {
        // Error; emit only social change.
        if (payload.isError()) {
            OnSocialChanged event = new OnSocialChanged();
            event.error = payload.error;
            emitChange(event);
        // Two-factor authentication code sent via SMS; emit only social change.
        } else if (payload.hasPhoneNumber()) {
            OnSocialChanged event = new OnSocialChanged(payload);
            emitChange(event);
        // Two-factor authentication or social connect is required; emit only social change.
        } else if (!payload.hasToken()) {
            OnSocialChanged event = new OnSocialChanged(payload);
            event.requiresTwoStepAuth = payload.hasTwoStepTypes();
            emitChange(event);
            // No error and two-factor authentication is not required; emit only authentication change.
        } else {
            // Social login or signup completed; update token and send boolean flag.
            if (payload.hasUsername()) {
                updateToken(new UpdateTokenPayload(payload.bearerToken), payload.createdAccount, payload.userName);
            } else {
                updateToken(new UpdateTokenPayload(payload.bearerToken));
            }
        }
    }

    private void handlePushUsernameCompleted(AccountPushUsernameResponsePayload payload) {
        if (!payload.isError()) {
            AccountSqlUtils.updateUsername(getAccount(), payload.username);
            getAccount().setUserName(payload.username);
        }

        OnUsernameChanged onUsernameChanged = new OnUsernameChanged();
        onUsernameChanged.username = payload.username;
        onUsernameChanged.type = payload.type;
        onUsernameChanged.error = payload.error;
        emitChange(onUsernameChanged);
    }

    private void handleNewAccountCreated(NewAccountResponsePayload payload) {
        OnNewUserCreated onNewUserCreated = new OnNewUserCreated();
        onNewUserCreated.error = payload.error;
        onNewUserCreated.dryRun = payload.dryRun;
        emitChange(onNewUserCreated);
    }

    private void handleCheckedIsAvailable(IsAvailableResponsePayload payload) {
        OnAvailabilityChecked event = new OnAvailabilityChecked(payload.type, payload.value, payload.isAvailable);

        if (payload.isError()) {
            event.error = payload.error;
        }

        emitChange(event);
    }

    private void emitAccountChangeError(AccountErrorType errorType) {
        OnAccountChanged event = new OnAccountChanged();
        event.error = new AccountError(errorType, "");
        emitChange(event);
    }

    private void createFetchUsernameSuggestions(FetchUsernameSuggestionsPayload payload) {
        mAccountRestClient.fetchUsernameSuggestions(payload.name);
    }

    private void createNewAccount(NewAccountPayload payload) {
        mAccountRestClient.newAccount(payload.username, payload.password, payload.email, payload.dryRun);
    }

    private void createPushSocialAuth(PushSocialAuthPayload payload) {
        mAccountRestClient.pushSocialAuth(payload.userId, payload.type, payload.nonce, payload.code);
    }

    private void createPushSocialConnect(PushSocialPayload payload) {
        mAccountRestClient.pushSocialConnect(payload.idToken, payload.service);
    }

    private void createPushSocialLogin(PushSocialPayload payload) {
        mAccountRestClient.pushSocialLogin(payload.idToken, payload.service);
    }

    private void createPushSocialSignup(PushSocialPayload payload) {
        mAccountRestClient.pushSocialSignup(payload.idToken, payload.service);
    }

    private void createPushSocialSms(PushSocialSmsPayload payload) {
        mAccountRestClient.pushSocialSms(payload.userId, payload.nonce);
    }

    private void createPushUsername(PushUsernamePayload payload) {
        mAccountRestClient.pushUsername(payload.username, payload.actionType);
    }

    private void clearAccountAndAccessToken() {
        // Remove Account
        AccountSqlUtils.deleteAccount(mAccount);
        mAccount.init();
        // Remove authentication token
        mAccessToken.set(null);
    }

    private void signOut() {
        clearAccountAndAccessToken();
        OnAccountChanged accountChanged = new OnAccountChanged();
        accountChanged.accountInfosChanged = true;
        emitChange(accountChanged);
        emitChange(new OnAuthenticationChanged());
    }

    public AccountModel getAccount() {
        return mAccount;
    }

    /**
     * Can be used to check if Account is signed into WordPress.com.
     */
    public boolean hasAccessToken() {
        return mAccessToken.exists();
    }

    /**
     * Should be used for very specific purpose (like forwarding the token to a Webview)
     */
    public String getAccessToken() {
        return mAccessToken.get();
    }

    private void updateToken(UpdateTokenPayload updateTokenPayload) {
        mAccessToken.set(updateTokenPayload.token);
        emitChange(new OnAuthenticationChanged());
    }

    /**
     * Update access token for account store for social login or signup.
     *
     * @param updateTokenPayload payload containing token to be updated
     * @param createdAccount     flag to send in event to determine login or signup
     * @param userName           username of created account
     */
    private void updateToken(UpdateTokenPayload updateTokenPayload, boolean createdAccount, String userName) {
        mAccessToken.set(updateTokenPayload.token);
        OnAuthenticationChanged event = new OnAuthenticationChanged();
        event.createdAccount = createdAccount;
        event.userName = userName;
        emitChange(event);
    }

    private void updateDefaultAccount(AccountModel accountModel, AccountAction cause) {
        // Update memory instance
        mAccount = accountModel;
        AccountSqlUtils.insertOrUpdateDefaultAccount(accountModel);
        OnAccountChanged accountChanged = new OnAccountChanged();
        accountChanged.accountInfosChanged = true;
        accountChanged.causeOfChange = cause;
        emitChange(accountChanged);
    }

    private AccountModel loadAccount() {
        AccountModel account = AccountSqlUtils.getDefaultAccount();
        return account == null ? new AccountModel() : account;
    }

    private void authenticate(final AuthenticatePayload payload) {
        mAuthenticator.authenticate(payload.username, payload.password, payload.twoStepCode,
                payload.shouldSendTwoStepSms, new Authenticator.Listener() {
                    @Override
                    public void onResponse(Token token) {
                        mAccessToken.set(token.getAccessToken());
                        if (payload.nextAction != null) {
                            mDispatcher.dispatch(payload.nextAction);
                        }
                        emitChange(new OnAuthenticationChanged());
                    }
                }, new Authenticator.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        AppLog.e(T.API, "Authentication error");
                        OnAuthenticationChanged event = new OnAuthenticationChanged();
                        event.error = new AuthenticationError(
                                Authenticator.volleyErrorToAuthenticationError(volleyError),
                                Authenticator.volleyErrorToErrorMessage(volleyError));
                        emitChange(event);
                    }
                });
    }

    private void handleSentAuthEmail(final AuthEmailResponsePayload payload) {
        if (payload.isError()) {
            OnAuthEmailSent event = new OnAuthEmailSent(payload.isSignup);
            event.error = payload.error;
            emitChange(event);
        } else {
            OnAuthEmailSent event = new OnAuthEmailSent(payload.isSignup);
            emitChange(event);
        }
    }

    private boolean checkError(AccountRestPayload payload, String log) {
        if (payload.isError()) {
            AppLog.w(T.API, log + "\nError: " + payload.error.volleyError);
            return true;
        }
        return false;
    }

    /**
     * Get all subscriptions in store as a {@link SubscriptionModel} list.
     *
     * @return {@link List} of {@link SubscriptionModel}
     */
    public List<SubscriptionModel> getSubscriptions() {
        return WellSql.select(SubscriptionModel.class).getAsModel();
    }

    /**
     * Get all subscriptions in store matching {@param searchString} as a {@link SubscriptionModel} list.
     *
     * @param searchString      Text to filter subscriptions by
     *
     * @return {@link List} of {@link SubscriptionModel}
     */
    public List<SubscriptionModel> getSubscriptionsByNameOrUrlMatching(@NonNull String searchString) {
        return AccountSqlUtils.getSubscriptionsByNameOrUrlMatching(searchString);
    }

    private void updateSubscriptions(SubscriptionsModel subscriptions) {
        OnSubscriptionsChanged event = new OnSubscriptionsChanged();
        if (subscriptions.isError()) {
            event.error = new SubscriptionsError(subscriptions.error);
        } else {
            AccountSqlUtils.updateSubscriptions(subscriptions.getSubscriptions());
        }
        emitChange(event);
    }

    private void createAddOrDeleteSubscriptionEmailComment(AddOrDeleteSubscriptionPayload payload) {
        mAccountRestClient.updateSubscriptionEmailComment(payload.site, payload.action);
    }

    private void createAddOrDeleteSubscriptionEmailPost(AddOrDeleteSubscriptionPayload payload) {
        mAccountRestClient.updateSubscriptionEmailPost(payload.site, payload.action);
    }

    private void createUpdateSubscriptionEmailPostFrequency(UpdateSubscriptionPayload payload) {
        mAccountRestClient.updateSubscriptionEmailPostFrequency(payload.site, payload.frequency);
    }

    private void createAddOrDeleteSubscriptionNotificationPost(AddOrDeleteSubscriptionPayload payload) {
        mAccountRestClient.updateSubscriptionNotificationPost(payload.site, payload.action);
    }

    private void handleUpdatedSubscription(SubscriptionResponsePayload payload) {
        OnSubscriptionUpdated event = new OnSubscriptionUpdated();
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.subscribed = payload.isSubscribed;
            event.type = payload.type;
        }
        emitChange(event);
    }

    private void handleFetchedDomainContact(DomainContactPayload payload) {
        emitChange(new OnDomainContactFetched(payload.contactModel, payload.error));
    }

    private void createFetchAuthOptions(FetchAuthOptionsPayload payload) {
        mAccountRestClient.fetchAuthOptions(payload.emailOrUsername);
    }

    private void handleFetchedAuthOptions(FetchAuthOptionsResponsePayload payload) {
        OnAuthOptionsFetched event = new OnAuthOptionsFetched();
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.isPasswordless = payload.isPasswordless;
            event.isEmailVerified = payload.isEmailVerified;
        }
        emitChange(event);
    }
}
