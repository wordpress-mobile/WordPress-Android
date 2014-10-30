package org.wordpress.android.mocks;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestClient.REST_CLIENT_VERSIONS;

import org.wordpress.android.networking.RestClientFactoryAbstract;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.HashSet;
import java.util.Set;

public class RestClientFactoryTest implements RestClientFactoryAbstract {
    public static String sPrefix = "default";
    public static RestClient.REST_CLIENT_VERSIONS sVersion = REST_CLIENT_VERSIONS.V1;
    public static Context sContext;
    // keep a reference to each instances so we can update contexts and prefixes after instantiation
    public static Set<RestClientCustomizableMock> sInstances = new HashSet<RestClientCustomizableMock>();

    public static void setContextAllInstances(Context context) {
        sContext = context;
        if (sMode != Mode.CUSTOMIZABLE) {
            AppLog.e(T.TESTS, "You try to change context on a non-customizable RestClient mock");
        }
        for (RestClientCustomizableMock client : sInstances) {
            client.setContext(context);
        }
    }

    public static void setPrefixAllInstances(String prefix) {
        sPrefix = prefix;
        if (sMode != Mode.CUSTOMIZABLE) {
            AppLog.e(T.TESTS, "You try to change prefix on a non-customizable RestClient mock");
        }
        for (RestClientCustomizableMock client : sInstances) {
            client.setPrefix(prefix);
        }
    }

    public static Mode sMode = Mode.EMPTY;

    public RestClient make(RequestQueue queue) {
        switch (sMode) {
            case CUSTOMIZABLE:
                RestClientCustomizableMock client = new RestClientCustomizableMock(queue);
                if (sContext != null) {
                    client.setContextAndPrefix(sContext, sPrefix);
                } else {
                    AppLog.e(T.TESTS, "You have to set RestClientFactoryTest.sContext field before running tests");
                    throw new IllegalStateException();
                }
                AppLog.v(T.TESTS, "make: RestClientCustomizableMock");
                sInstances.add(client);
                return client;
            case EMPTY:
            default:
                AppLog.v(T.TESTS, "make: RestClientEmptyMock");
                return new RestClientEmptyMock(queue);
        }
    }

    public RestClient make(RequestQueue queue, RestClient.REST_CLIENT_VERSIONS version) {
        sVersion = version;
        return make(queue);
    }

    public enum Mode {EMPTY, CUSTOMIZABLE}
}
