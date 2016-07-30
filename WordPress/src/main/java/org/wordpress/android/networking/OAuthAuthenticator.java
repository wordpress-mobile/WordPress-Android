package org.wordpress.android.networking;

import org.wordpress.android.util.StringUtils;

// TODO: STORES: kill this when we don't need any other rest client
public class OAuthAuthenticator implements Authenticator {
    public static String sAccessToken;

    @Override
    public void authenticate(final AuthenticatorRequest request) {
        request.sendWithAccessToken(StringUtils.notNullStr(sAccessToken));
    }
}
