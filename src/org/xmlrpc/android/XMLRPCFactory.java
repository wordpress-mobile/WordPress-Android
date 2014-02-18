package org.xmlrpc.android;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.net.URI;

public class XMLRPCFactory {
    public static XMLRPCFactoryAbstract sFactory;

    public static XMLRPCClientInterface instantiate(URI uri, String httpUser, String httpPassword) {
        if (sFactory == null) {
            sFactory = new XMLRPCFactoryDefault();
        }
        AppLog.v(T.UTILS, "instantiate XMLRPCClient using sFactory: " + sFactory.getClass());
        return sFactory.make(uri, httpUser, httpPassword);
    }
}
