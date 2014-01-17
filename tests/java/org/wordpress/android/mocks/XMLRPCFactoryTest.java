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
    public static String sPrefix = "default";
    public static Context sContext;

    public XMLRPCClientInterface make(URI uri, String httpUser, String httpPassword) {
        switch (sMode) {
            case CUSTOMIZABLE:
                XMLRPCClientCustomizableMockup client = new XMLRPCClientCustomizableMockup(uri, httpUser, httpPassword);
                if (sContext != null) {
                    client.setContextAndPrefix(sContext, sPrefix);
                } else {
                    AppLog.e(T.TESTS, "You have to set XMLRPCFactoryTest.sContext field before running tests");
                    throw new IllegalStateException();
                }
                AppLog.v(T.TESTS, "make: XMLRPCClientCustomizableMockup");
                return client;
            case EMPTY:
            default:
                AppLog.v(T.TESTS, "make: XMLRPCClientEmptyMock");
                return new XMLRPCClientEmptyMock(uri, httpUser, httpPassword);
        }
    }
}
