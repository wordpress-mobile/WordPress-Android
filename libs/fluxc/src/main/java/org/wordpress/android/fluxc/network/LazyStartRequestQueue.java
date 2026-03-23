package org.wordpress.android.fluxc.network;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

/**
 * A {@link RequestQueue} that defers calling {@link #start()} until the
 * first {@link Request} is added. This avoids eagerly spinning up the
 * network-thread pool during Application.onCreate when no requests are
 * pending, saving thread-creation overhead on the critical startup path.
 */
public class LazyStartRequestQueue extends RequestQueue {
    private volatile boolean mStarted = false;

    public LazyStartRequestQueue(Cache cache, Network network, int threadPoolSize) {
        super(cache, network, threadPoolSize);
    }

    @Override
    public <T> Request<T> add(Request<T> request) {
        if (!mStarted) {
            synchronized (this) {
                if (!mStarted) {
                    start();
                    mStarted = true;
                }
            }
        }
        return super.add(request);
    }
}
