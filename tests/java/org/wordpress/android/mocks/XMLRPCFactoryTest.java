package org.wordpress.android.mocks;

import android.content.Context;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCFactoryAbstract;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class XMLRPCFactoryTest implements XMLRPCFactoryAbstract {
    public static String sPrefix = "default";
    public static Context sContext;
    public static Mode sMode = Mode.EMPTY;
    public static Set<XMLRPCClientCustomizableMock> sInstances = new HashSet<XMLRPCClientCustomizableMock>();;

    public static void setContextAllInstances(Context context) {
        sContext = context;
        if (sMode != Mode.CUSTOMIZABLE) {
            AppLog.e(T.TESTS, "You try to change context on a non-customizable RestClient mock");
        }
        for (XMLRPCClientCustomizableMock client : sInstances) {
            client.setContext(context);
        }
    }

    public static void setPrefixAllInstances(String prefix) {
        sPrefix = prefix;
        if (sMode != Mode.CUSTOMIZABLE) {
            AppLog.e(T.TESTS, "You try to change prefix on a non-customizable RestClient mock");
        }
        for (XMLRPCClientCustomizableMock client : sInstances) {
            client.setPrefix(prefix);
        }
    }

    public XMLRPCClientInterface make(URI uri, String httpUser, String httpPassword) {
        switch (sMode) {
            case CUSTOMIZABLE:
                XMLRPCClientCustomizableMock client = new XMLRPCClientCustomizableMock(uri, httpUser, httpPassword);
                if (sContext != null) {
                    client.setContextAndPrefix(sContext, sPrefix);
                } else {
                    AppLog.e(T.TESTS, "You have to set XMLRPCFactoryTest.sContext field before running tests");
                    throw new IllegalStateException();
                }
                AppLog.v(T.TESTS, "make: XMLRPCClientCustomizableMock");
                sInstances.add(client);
                return client;
            case EMPTY:
            default:
                AppLog.v(T.TESTS, "make: XMLRPCClientEmptyMock");
                return new XMLRPCClientEmptyMock(uri, httpUser, httpPassword);
        }
    }

    public enum Mode {EMPTY, CUSTOMIZABLE}
}
