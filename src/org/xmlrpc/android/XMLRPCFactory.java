package org.xmlrpc.android;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.net.URI;

public class XMLRPCFactory {
    public static XMLRPCFactoryAbstract factory;

    public static XMLRPCClientInterface instantiate(URI uri, String httpUser, String httpPassword) {
        if (factory == null) {
            factory = new XMLRPCFactoryDefault();
        }
        AppLog.v(T.UTILS, "instantiate RestClientUtilsInterface using factory: " + factory.getClass());
        return factory.make(uri, httpUser, httpPassword);
    }
}
