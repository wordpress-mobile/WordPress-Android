package org.wordpress.android;

import org.wordpress.android.mocks.OAuthAuthenticatorFactoryTest;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.SystemServiceFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.networking.OAuthAuthenticatorFactory;
import org.wordpress.android.networking.RestClientFactory;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SystemServiceFactory;
import org.xmlrpc.android.XMLRPCFactory;

public class FactoryUtils {
    public static void initWithTestFactories() {
        // create test factories
        XMLRPCFactory.sFactory = new XMLRPCFactoryTest();
        RestClientFactory.sFactory = new RestClientFactoryTest();
        OAuthAuthenticatorFactory.sFactory = new OAuthAuthenticatorFactoryTest();
        SystemServiceFactory.sFactory = new SystemServiceFactoryTest();
        AppLog.v(T.TESTS, "Mocks factories instantiated");
    }
}
