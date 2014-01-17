package org.wordpress.android.mocks;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;

import org.wordpress.android.networking.RestClientFactoryAbstract;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class RestClientFactoryTest implements RestClientFactoryAbstract {
    public static String sPrefix = "default";
    public static Context sContext;
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
                return client;
            case EMPTY:
            default:
                AppLog.v(T.TESTS, "make: RestClientEmptyMock");
                return new RestClientEmptyMock(queue);
        }
    }

    public enum Mode {EMPTY, CUSTOMIZABLE}
}
