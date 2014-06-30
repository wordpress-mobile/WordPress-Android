package org.wordpress.android.networking;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class OAuthAuthenticatorFactory {
    public static OAuthAuthenticatorFactoryAbstract sFactory;

    public static OAuthAuthenticator instantiate() {
        if (sFactory == null) {
            sFactory = new OAuthAuthenticatorFactoryDefault();
        }
        AppLog.v(T.UTILS, "instantiate OAuth using sFactory: " + sFactory.getClass());
        return sFactory.make();
    }
}
