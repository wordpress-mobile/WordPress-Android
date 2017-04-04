package org.wordpress.android.models;

public enum MediaUploadState {
    QUEUED,
    UPLOADING,
    DELETE,
    DELETED,
    FAILED,
    UPLOADED;

    public static MediaUploadState fromString(String strState) {
        if (strState != null) {
            for (MediaUploadState state: MediaUploadState.values()) {
                if (strState.equalsIgnoreCase(state.name())) {
                    return state;
                }
            }
        }
        return UPLOADED;
    }
}
