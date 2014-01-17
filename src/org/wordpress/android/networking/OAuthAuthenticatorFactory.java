package org.wordpress.android.networking;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class OAuthAuthenticatorFactory {
    public static OAuthAuthenticatorFactoryAbstract factory;

    public static OAuthAuthenticator instantiate() {
        if (factory == null) {
            factory = new OAuthAuthenticatorFactoryDefault();
        }
        AppLog.v(T.UTILS, "instantiate OAuth using factory: " + factory.getClass());
        return factory.make();
    }
}
