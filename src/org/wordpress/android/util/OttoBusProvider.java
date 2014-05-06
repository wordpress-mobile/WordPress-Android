package org.wordpress.android.util;

import com.squareup.otto.Bus;

public class OttoBusProvider {
    private static final Bus OTTO_BUS = new Bus();

    private OttoBusProvider() {
        // No instances.
    }

    public static Bus getInstance() {
        return OTTO_BUS;
    }

    // Global events
    public static class RefreshEvent {}
}