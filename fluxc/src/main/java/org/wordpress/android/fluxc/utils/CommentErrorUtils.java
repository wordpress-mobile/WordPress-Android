package org.wordpress.android.fluxc.utils;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.store.CommentStore.CommentError;
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.PushCommentResponsePayload;

import java.util.ArrayList;

public class CommentErrorUtils {
    public static FetchCommentResponsePayload commentErrorToFetchCommentPayload(BaseNetworkError error,
                                                                                 CommentModel comment) {
        FetchCommentResponsePayload payload = new FetchCommentResponsePayload(comment);
        payload.error = new CommentError(genericToCommentError(error), "");
        return payload;
    }

    public static FetchCommentsResponsePayload commentErrorToFetchCommentsPayload(BaseNetworkError error) {
        FetchCommentsResponsePayload payload = new FetchCommentsResponsePayload(new ArrayList<CommentModel>());
        payload.error = new CommentError(genericToCommentError(error), "");
        return payload;
    }

    public static PushCommentResponsePayload commentErrorToPushCommentPayload(BaseNetworkError error,
                                                                               CommentModel comment) {
        PushCommentResponsePayload payload = new PushCommentResponsePayload(comment);
        payload.error = new CommentError(genericToCommentError(error), "");
        return payload;
    }

    private static CommentErrorType genericToCommentError(BaseNetworkError error) {
        CommentErrorType errorType = CommentErrorType.GENERIC_ERROR;
        if (error.isGeneric()) {
            switch (error.type) {
                case NOT_FOUND:
                    errorType = CommentErrorType.INVALID_COMMENT;
                    break;
                case INVALID_RESPONSE:
                    errorType = CommentErrorType.INVALID_RESPONSE;
                    break;
            }
        }
        return errorType;
    }
}
