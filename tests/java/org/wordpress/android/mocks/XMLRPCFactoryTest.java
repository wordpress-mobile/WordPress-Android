package org.wordpress.android.mocks;

import android.content.Context;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCFactoryAbstract;

import java.net.URI;

public class XMLRPCFactoryTest implements XMLRPCFactoryAbstract  {
    public enum Mode {EMPTY, CUSTOMIZABLE}
    public static Mode sMode = Mode.EMPTY;
    public static Context sContext;

    public XMLRPCClientInterface make(URI uri, String httpUser, String httpPassword) {
        switch (sMode) {
            case CUSTOMIZABLE:
                XMLRPCClientCustomizableMockup client = new XMLRPCClientCustomizableMockup(uri, httpUser, httpPassword);
                if (sContext != null) {
                    // TODO: add context string param
                    client.setContext(sContext, "default");
                } else {
                    AppLog.e(T.TESTS, "You have to set XMLRPCFactoryTest.sContext field before running tests");
                }
                return client;
            case EMPTY:
            default:
                return new XMLRPCClientEmptyMock(uri, httpUser, httpPassword);
        }
    }
}
