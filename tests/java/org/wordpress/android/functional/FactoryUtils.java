package org.wordpress.android.functional;

import android.content.Context;

import org.wordpress.android.TestUtils;
import org.wordpress.android.WordPress;
import org.wordpress.android.mocks.OAuthAuthenticatorFactoryTest;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.networking.OAuthAuthenticatorFactory;
import org.wordpress.android.networking.RestClientFactory;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCFactory;

public class FuncUtils {
    public static void initWithTestFactories() {
        // create test factories
        XMLRPCFactory.factory = new XMLRPCFactoryTest();
        RestClientFactory.factory = new RestClientFactoryTest();
        OAuthAuthenticatorFactory.factory = new OAuthAuthenticatorFactoryTest();
        AppLog.v(T.TESTS, "Mocks factories instantiated");
    }

    public static void clearApplicationState(Context context) {
        WordPress.signOut(context);
        TestUtils.clearDefaultSharedPreferences(context);
        TestUtils.dropDB(context);
    }
}
