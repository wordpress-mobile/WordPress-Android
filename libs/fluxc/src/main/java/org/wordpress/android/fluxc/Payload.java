package org.wordpress.android.fluxc;

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

public abstract class Payload {
    public BaseNetworkError error;
    public boolean isError() {
        return error != null;
    }
}
