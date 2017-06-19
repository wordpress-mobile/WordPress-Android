package org.wordpress.android.ui.publicize;

public class PublicizeConstants {
    public static final String ARG_SERVICE_ID     = "service_id";
    public static final String ARG_CONNECTION_ID  = "connection_id";
    public static final String ARG_CONNECTION_ARRAY_JSON = "connection_array_json";

    public enum ConnectAction {
        CONNECT,
        DISCONNECT,
        RECONNECT,
        CONNECT_ANOTHER_ACCOUNT
    }
}
