package org.wordpress.android.fluxc.network.xmlrpc;

public enum XMLRPC {
    // Media
    GET_MEDIA_LIBRARY("wp.getMediaLibrary"),
    GET_MEDIA_ITEM("wp.getMediaItem"),
    EDIT_MEDIA("wp.editPost"),
    DELETE_MEDIA("wp.deletePost"),
    UPLOAD_FILE("wp.uploadFile"),

    GET_OPTIONS("wp.getOptions"),

    GET_USERS_BLOGS("wp.getUsersBlogs"),
    LIST_METHODS("system.listMethods"),

    DELETE_POST("wp.deletePost"),
    EDIT_POST("wp.editPost"),
    GET_POST("wp.getPost"),
    GET_POSTS("wp.getPosts"),
    NEW_POST("wp.newPost");

    private final String mEndpoint;

    XMLRPC(String endpoint) {
        mEndpoint = endpoint;
    }

    @Override
    public String toString() {
        return mEndpoint;
    }
}
