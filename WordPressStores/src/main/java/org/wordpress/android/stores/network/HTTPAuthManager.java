package org.wordpress.android.stores.network;

import android.content.Context;

public class HTTPAuthManager {
    private Context mContext;

    public HTTPAuthManager(Context appContext) {
        mContext = appContext;
    }

    public boolean match(String url) {
        // FIXME:
        return true;
    }
}
