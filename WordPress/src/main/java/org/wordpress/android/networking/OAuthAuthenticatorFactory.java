package org.wordpress.android.networking;

public class OAuthAuthenticatorFactory {
    private static OAuthAuthenticatorFactoryAbstract sFactory;

    public static OAuthAuthenticator instantiate() {
        if (sFactory == null) {
            sFactory = new OAuthAuthenticatorFactoryDefault();
        }
        return sFactory.make();
    }
}
