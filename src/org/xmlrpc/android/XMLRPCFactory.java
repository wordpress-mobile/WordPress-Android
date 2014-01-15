package org.xmlrpc.android;

import org.xmlrpc.android.mocks.XMLRPCClientEmptyMock;

import java.net.URI;

public class XMLRPCFactory {
    public enum Mode {PRODUCTION, EMPTY_MOCK, CUSTOMIZABLE_MOCK}
    private static Mode mMode = Mode.PRODUCTION;

    public static Mode getMode() {
        return mMode;
    }

    public static void setMode(Mode mode) {
        mMode = mode;
    }

    public static XMLRPCClientInterface instantiate(URI uri, String httpUser, String httpPassword) {
        switch (getMode()) {
            case EMPTY_MOCK:
                return new XMLRPCClientEmptyMock(uri, httpUser, httpPassword);
            default:
            case PRODUCTION:
                return new XMLRPCClient(uri, httpUser, httpPassword);
        }
    }
}
