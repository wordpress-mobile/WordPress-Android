package org.wordpress.android.fluxc.network;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.BasicNetwork;

/**
 * Enhances [BasicNetwork] by adding retries on temporary redirect (307) according to the applied retry policy
 */
public class RetryOnRedirectBasicNetwork extends BasicNetwork {
    public static final int HTTP_TEMPORARY_REDIRECT = 307;

    public RetryOnRedirectBasicNetwork(BaseHttpStack httpStack) {
        super(httpStack);
    }

    @Override public NetworkResponse performRequest(Request<?> request) throws VolleyError {
        try {
            return super.performRequest(request);
        } catch (ServerError error) {
            if (request != null && error.networkResponse.statusCode == HTTP_TEMPORARY_REDIRECT) {
                RetryPolicy policy = request.getRetryPolicy();
                policy.retry(error); // If no attempts are left an error is thrown
                try {
                    Thread.sleep(policy.getCurrentTimeout()); // Wait before retrying
                } catch (InterruptedException e) {
                    throw error;
                }
                return performRequest(request);
            }
            throw error;
        }
    }
}
