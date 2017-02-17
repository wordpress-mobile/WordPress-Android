package org.wordpress.android.fluxc;

import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

import java.lang.reflect.Field;

public abstract class Payload {
    public BaseNetworkError error;

    public boolean isError() {
        try {
            Field field = getClass().getDeclaredField("error");
            return field.get(this) != null;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    protected Payload clone() throws CloneNotSupportedException {
        if (!(this instanceof Cloneable)) {
            throw new CloneNotSupportedException("Class " + getClass().getName() + " doesn't implement Cloneable");
        }

        Payload clonedPayload = (Payload) super.clone();

        // Clone non-primitive, mutable fields
        if (this.error != null) {
            clonedPayload.error = new BaseRequest.BaseNetworkError(this.error);
        }

        return clonedPayload;
    }
}
