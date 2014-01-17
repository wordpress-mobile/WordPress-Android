package org.wordpress.android.mocks;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;

import org.wordpress.android.networking.RestClientFactoryAbstract;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

/**
 * Created by max on 17/01/2014.
 */
public class RestClientFactoryTest implements RestClientFactoryAbstract {
    public enum Mode {EMPTY}

    public static Mode sMode = Mode.EMPTY;
    public static Context sContext;

    public RestClient make(RequestQueue queue) {
        switch (sMode) {
            case EMPTY:
            default:
                AppLog.v(T.TESTS, "make: RestClientUtilsEmptyMock");
                return new RestClientEmptyMock(queue);
        }
    }
}
