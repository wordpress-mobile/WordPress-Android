package org.wordpress.android.stores.example;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.action.AccountAction;
import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.action.SiteAction;
import org.wordpress.android.stores.example.SignInDialog.Listener;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.stores.store.AccountStore.OnAccountChanged;
import org.wordpress.android.stores.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.stores.store.SiteStore;
import org.wordpress.android.stores.store.SiteStore.OnSiteChanged;
import org.wordpress.android.stores.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.stores.utils.SelfHostedDiscoveryUtils;
import org.wordpress.android.stores.utils.SelfHostedDiscoveryUtils.DiscoveryCallback;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;

public class MainExampleActivity extends AppCompatActivity {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;

    private TextView mLogView;
    private Button mAccountInfos;
    private Button mListSites;
    private Button mLogSites;
    private Button mUpdateFirstSite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getApplication()).component().inject(this);
        setContentView(R.layout.activity_example);
        mListSites = (Button) findViewById(R.id.list_sites_button);
        mListSites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // show signin dialog
                showSigninDialog();
            }
        });
        mAccountInfos = (Button) findViewById(R.id.account_infos_button);
        mAccountInfos.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDispatcher.dispatch(AccountAction.FETCH);
            }
        });
        mUpdateFirstSite = (Button) findViewById(R.id.update_first_site);
        mUpdateFirstSite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDispatcher.dispatch(SiteAction.FETCH_SITE, mSiteStore.getSites().get(0));
            }
        });

        mLogSites = (Button) findViewById(R.id.log_sites);
        mLogSites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (SiteModel site : mSiteStore.getSites()) {
                    AppLog.i(T.API, LogUtils.toString(site));
                }
            }
        });

        mLogView = (TextView) findViewById(R.id.log);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Order is important here since onRegister could fire onChanged events. "register(this)" should probably go
        // first everywhere.
        mDispatcher.register(this);
        mDispatcher.register(mSiteStore);
        mDispatcher.register(mAccountStore);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDispatcher.unregister(mSiteStore);
        mDispatcher.unregister(mAccountStore);
        mDispatcher.unregister(this);
    }

    // Event listeners

    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        if (event.accountInfosChanged) {
            prependToLog("Display Name: " + mAccountStore.getAccount().getDisplayName());
        }
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        mAccountInfos.setEnabled(mAccountStore.hasAccessToken());
        if (event.isError) {
            prependToLog("Authentication error: " + event.authError);
        }
    }

    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        if (mSiteStore.hasSite()) {
            SiteModel firstSite = mSiteStore.getSites().get(0);
            prependToLog("First site name: " + firstSite.getName() + " - Total sites: " + mSiteStore.getSitesCount());
            mUpdateFirstSite.setEnabled(true);
        } else {
            mUpdateFirstSite.setEnabled(false);
        }
    }

    // Private methods

    private void prependToLog(String s) {
        s = s + "\n" + mLogView.getText();
        mLogView.setText(s);
    }

    private void showSigninDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment newFragment = SignInDialog.newInstance(new Listener() {
            @Override
            public void onClick(String username, String password, String url) {
                signInAction(username, password, url);
            }
        });
        newFragment.show(ft, "dialog");
    }

    /**
     * Called when the user tap OK on the SignIn Dialog. It authenticates and list user sites, on wpcom or self hosted
     * depending if the user filled the URL field.
     */
    private void signInAction(final String username, final String password, final String url) {
        if (TextUtils.isEmpty(url)) {
            wpcomFetchSites(username, password);
        } else {
            SelfHostedDiscoveryUtils.discoverSelfHostedEndPoint(url, new DiscoveryCallback() {
                @Override
                public void onError(Error error) {
                    if (error == Error.WORDPRESS_COM_SITE) {
                        wpcomFetchSites(username, password);
                    }
                    AppLog.e(T.API, "Discover error: " + error);
                }

                @Override
                public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                    selfHostedFetchSites(username, password, xmlrpcEndpoint);
                }
            });
        }
    }

    private void wpcomFetchSites(String username, String password) {
        AuthenticatePayload payload = new AuthenticatePayload();
        payload.username = username;
        payload.password = password;
        // Next action will be dispatched if authentication is successful
        payload.nextAction = mDispatcher.createAction(SiteAction.FETCH_SITES);
        mDispatcher.dispatch(AuthenticationAction.AUTHENTICATE, payload);
    }

    private void selfHostedFetchSites(String username, String password, String xmlrpcEndpoint) {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = username;
        payload.password = password;
        payload.xmlrpcEndpoint = xmlrpcEndpoint;
        // Self Hosted don't have any "Authentication" request, try to list sites with user/password
        mDispatcher.dispatch(SiteAction.FETCH_SITES_XMLRPC, payload);
    }
}
