package org.wordpress.android;

import org.wordpress.android.mocks.OAuthAuthenticatorFactoryTest;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.networking.OAuthAuthenticatorFactory;
import org.wordpress.android.networking.RestClientFactory;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCFactory;

public class FactoryUtils {
    public static void initWithTestFactories() {
        // create test factories
        XMLRPCFactory.factory = new XMLRPCFactoryTest();
        RestClientFactory.factory = new RestClientFactoryTest();
        OAuthAuthenticatorFactory.factory = new OAuthAuthenticatorFactoryTest();
        AppLog.v(T.TESTS, "Mocks factories instantiated");
    }
}
