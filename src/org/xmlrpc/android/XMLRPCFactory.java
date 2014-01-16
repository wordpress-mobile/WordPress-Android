package org.xmlrpc.android;

import java.net.URI;

public class XMLRPCFactory {
    public static XMLRPCFactoryAbstract factory;

    public static XMLRPCClientInterface instantiate(URI uri, String httpUser, String httpPassword) {
        if (factory == null) {
            factory = new XMLRPCFactoryDefault();
        }
        return factory.make(uri, httpUser, httpPassword);
    }
}
