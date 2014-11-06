package org.wordpress.android.networking;

/**
 * Interface that provides a method that should perform the necessary task to make sure
 * the provided AuthenticatorRequest will be authenticated.
 *
 * The Authenticator must call AuthenticatorRequest.send() when it has completed its operations. For
 * convenience the AuthenticatorRequest class provides AuthenticatorRequest.setAccessToken so the Authenticator can
 * easily update the access token.
 */
public interface Authenticator {
    void authenticate(final AuthenticatorRequest authenticatorRequest);
}
