package org.xmlrpc.android;

import org.wordpress.android.mocks.XMLRPCClientMock;

import java.net.URI;

public class XMLRPCFactory extends FlavoredFactory {
    public static XMLRPCClientInterface instantiate(URI uri, String httpUser, String httpPassword) {
        switch (getMode()) {
            case TEST:
                return new XMLRPCClientMock(uri, httpUser, httpPassword);
            default:
            case PRODUCTION:
                return new XMLRPCClient(uri, httpUser, httpPassword);
        }
    }
}
