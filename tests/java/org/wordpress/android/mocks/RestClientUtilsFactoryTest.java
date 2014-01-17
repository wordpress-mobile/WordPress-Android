package org.wordpress.android.mocks;

import android.content.Context;

import com.android.volley.RequestQueue;

import org.wordpress.android.networking.Authenticator;
import org.wordpress.android.networking.RestClientUtilsFactoryAbstract;
import org.wordpress.android.networking.RestClientUtilsInterface;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class RestClientUtilsFactoryTest implements RestClientUtilsFactoryAbstract {
    public enum Mode {EMPTY}
    public static Mode sMode = Mode.EMPTY;
    public static Context sContext;

    public RestClientUtilsInterface make(RequestQueue queue, Authenticator authenticator) {
        switch (sMode) {
            case EMPTY:
            default:
                AppLog.v(T.TESTS, "make: RestClientUtilsEmptyMock");
                return new RestClientUtilsEmptyMock();
        }
    }
}