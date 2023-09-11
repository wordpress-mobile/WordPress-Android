package org.wordpress.android.networking;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.util.StringUtils;

// TODO: kill this when we don't need any other rest client than the one in FluxC
public class OAuthAuthenticator implements Authenticator {
    @NonNull private final AccessToken mAccessToken;

    public OAuthAuthenticator(@NonNull AccessToken accessToken) {
        mAccessToken = accessToken;
    }

    @Override
    public void authenticate(@NonNull AuthenticatorRequest request) {
        request.sendWithAccessToken(StringUtils.notNullStr(mAccessToken.get()));
    }
}
