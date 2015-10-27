package org.wordpress.android.stores.store;

import com.android.volley.VolleyError;
import com.squareup.otto.Subscribe;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.action.AccountAction;
import org.wordpress.android.stores.action.Action;
import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.action.IAction;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator.Token;
import org.wordpress.android.stores.persistence.UpdateAllExceptId;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;

import javax.inject.Inject;

/**
 * In-memory based and persisted in SQLite.
 */
public class AccountStore extends Store {
    // Payloads
    public static class AuthenticatePayload implements Payload {
        public AuthenticatePayload() {}
        public String username;
        public String password;
        public Action nextAction;
    }

    // OnChanged Events
    public class OnAccountChanged extends OnChanged {
        public boolean accountInfosChanged;
    }

    public class OnAuthenticationChanged extends OnChanged {
        public boolean isError;
        public AuthError authError;
    }

    private AccountRestClient mAccountRestClient;
    private Authenticator mAuthenticator;

    private AccountModel mAccount;
    private AccessToken mAccessToken;

    @Inject
    public AccountStore(Dispatcher dispatcher, AccountRestClient accountRestClient,
                        Authenticator authenticator, AccessToken accessToken) {
        super(dispatcher);
        mAuthenticator = authenticator;
        mAccountRestClient = accountRestClient;
        mAccount = loadAccount();
        mAccessToken = accessToken;
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "AccountStore onRegister");
        // TODO: I'm really not sure about emitting OnChange events here. It helps by having startup events, but
        // activity listeners must be registered before
        emitChange(new OnAccountChanged());
        emitChange(new OnAuthenticationChanged());
    }

    @Subscribe
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (actionType == AuthenticationAction.AUTHENTICATE_ERROR) {
            OnAuthenticationChanged event = new OnAuthenticationChanged();
            event.isError = true;
            event.authError = (AuthError) action.getPayload();
            emitChange(event);
        } else if (actionType == AuthenticationAction.AUTHENTICATE) {
            AuthenticatePayload payload = (AuthenticatePayload) action.getPayload();
            authenticate(payload.username, payload.password, payload);
        } else if (actionType == AccountAction.FETCH) {
            mAccountRestClient.getMe();
        } else if (actionType == AccountAction.UPDATE) {
            AccountModel accountModel = (AccountModel) action.getPayload();
            update(accountModel);
            OnAccountChanged accountChanged = new OnAccountChanged();
            accountChanged.accountInfosChanged = true;
            emitChange(accountChanged);
        }
    }

    public AccountModel getAccount() {
        return mAccount;
    }

    public boolean hasAccessToken() {
        return mAccessToken.exists();
    }

    private void update(AccountModel accountModel) {
        // Update memory instance
        mAccount = accountModel;
        // Update DB
        List<AccountModel> accountResults = WellSql.selectUnique(AccountModel.class).getAsModel();
        if (accountResults.isEmpty()) {
            // insert
            WellSql.insert(accountModel).execute();
        } else {
            // update
            int oldId = accountResults.get(0).getId();
            WellSql.update(AccountModel.class).whereId(oldId)
                    .put(accountModel, new UpdateAllExceptId<AccountModel>()).execute();
        }
    }

    private AccountModel loadAccount() {
        List<AccountModel> accountResults = WellSql.selectUnique(AccountModel.class).getAsModel();
        if (accountResults.isEmpty()) {
            return new AccountModel();
        } else {
            return accountResults.get(0);
        }
    }

    private void authenticate(String username, String password, final AuthenticatePayload payload) {
        mAuthenticator.authenticate(username, password, null, false, new Authenticator.Listener() {
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
                event.isError = true;
                emitChange(event);
            }
        });
    }
}
