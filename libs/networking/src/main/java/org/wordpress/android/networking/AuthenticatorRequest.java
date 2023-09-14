package org.wordpress.android.networking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

/**
 * Encapsulates the behaviour for asking the Authenticator for an access token. This
 * allows the request maker to disregard the authentication state when making requests.
 */
public class AuthenticatorRequest {
    @NonNull private final RestRequest mRequest;
    @NonNull private final RestClient mRestClient;
    @Nullable private final Authenticator mAuthenticator;

    protected AuthenticatorRequest(
            @NonNull RestRequest request,
            @NonNull RestClient restClient,
            @Nullable Authenticator authenticator
    ) {
        mRequest = request;
        mRestClient = restClient;
        mAuthenticator = authenticator;
    }

    /**
     * Attempt to send the request, checks to see if we have an access token and if not
     * asks the Authenticator to authenticate the request.
     * <p>
     * If no Authenticator is provided the request is always sent.
     */
    protected void send() {
        if (mAuthenticator == null) {
            mRestClient.send(mRequest);
        } else {
            mAuthenticator.authenticate(this);
        }
    }

    public void sendWithAccessToken(@NonNull String token) {
        mRequest.setAccessToken(token);
        mRestClient.send(mRequest);
    }
}
