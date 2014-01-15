package org.xmlrpc.android;

import java.net.URI;

public class XMLRPCFactory {
    public enum Mode {PRODUCTION, EMPTY_MOCK, CUSTOMIZABLE_MOCK}
    private static Mode mMode = Mode.EMPTY_MOCK;
    public static XMLRPCFactoryAbstract factory;

    public static Mode getMode() {
        return mMode;
    }

    public static void setMode(Mode mode) {
        mMode = mode;
    }

    public static XMLRPCClientInterface instantiate(URI uri, String httpUser, String httpPassword) {
        if (factory == null) {
            factory = new XMLRPCFactoryDefault();
        }
        return factory.make(uri, httpUser, httpPassword);
        /*
        switch (getMode()) {
            case EMPTY_MOCK:
                return new XMLRPCClientEmptyMock(uri, httpUser, httpPassword);
            default:
            case PRODUCTION:
                return new XMLRPCClient(uri, httpUser, httpPassword);
        }
        */
    }
}
