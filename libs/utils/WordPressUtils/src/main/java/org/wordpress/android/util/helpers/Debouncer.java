package org.wordpress.android.util.helpers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Debouncer {
    private final ScheduledExecutorService mScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<Object, Future<?>> mDelayedMap = new ConcurrentHashMap<>();

    /**
     * Debounces {@code callable} by {@code delay}, i.e., schedules it to be executed after {@code delay},
     * or cancels its execution if the method is called with the same key within the {@code delay} again.
     */
    public void debounce(final Object key, final Runnable runnable, long delay, TimeUnit unit) {
        if (mScheduler.isShutdown()) {
            return;
        }
        final Future<?> prev = mDelayedMap.put(key, mScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    mDelayedMap.remove(key);
                }
            }
        }, delay, unit));
        if (prev != null) {
            prev.cancel(true);
        }
    }

    public void shutdown() {
        mScheduler.shutdownNow();
    }
}
