package org.xmlrpc.android;

import java.net.URI;

public class XMLRPCFactory {
    private static XMLRPCFactoryAbstract sFactory;

    public static XMLRPCClientInterface instantiate(URI uri, String httpUser, String httpPassword) {
        if (sFactory == null) {
            sFactory = new XMLRPCFactoryDefault();
        }
        return sFactory.make(uri, httpUser, httpPassword);
    }
}
