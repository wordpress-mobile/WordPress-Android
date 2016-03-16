package org.wordpress.android.stores.network;

import org.wordpress.android.stores.Payload;

public enum AuthError implements Payload {
    INVALID_TOKEN,
    NOT_AUTHENTICATED,
    INCORRECT_USERNAME_OR_PASSWORD,
    UNAUTHORIZED,
    HTTP_AUTH_ERROR,
}
