package org.wordpress.android.stores.network.xmlrpc;

public enum XMLRPC {
    GET_OPTIONS("wp.getOptions"),
    GET_USERS_BLOGS("wp.getUsersBlogs");

    private final String mEndpoint;

    XMLRPC(String endpoint) {
        mEndpoint = endpoint;
    }

    @Override
    public String toString() {
        return mEndpoint;
    }
}
