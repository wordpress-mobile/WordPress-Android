package org.wordpress.android.fluxc;

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

public abstract class Payload<T extends FluxCError> {
    public T error;

    public boolean isError() {
        return error != null;
    }

    @Override
    protected Payload clone() throws CloneNotSupportedException {
        if (!(this instanceof Cloneable)) {
            throw new CloneNotSupportedException("Class " + getClass().getName() + " doesn't implement Cloneable");
        }

        Payload clonedPayload = (Payload) super.clone();

        // Clone non-primitive, mutable fields
        if (error != null && error instanceof BaseNetworkError) {
            clonedPayload.error = new BaseNetworkError((BaseNetworkError) error);
        }

        return clonedPayload;
    }
}
