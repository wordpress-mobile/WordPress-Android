package org.wordpress.android.fluxc.utils;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCFault;
import org.wordpress.android.fluxc.store.CommentStore.CommentError;
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentResponsePayload;

import java.util.ArrayList;

public class CommentErrorUtils {
    public static RemoteCommentResponsePayload commentErrorToFetchCommentPayload(BaseNetworkError error,
                                                                                 CommentModel comment) {
        RemoteCommentResponsePayload payload = new org.wordpress.android.fluxc.store.CommentStore
                .RemoteCommentResponsePayload(comment);
        payload.error = new CommentError(genericToCommentError(error), "");
        return payload;
    }

    public static FetchCommentsResponsePayload commentErrorToFetchCommentsPayload(BaseNetworkError error) {
        FetchCommentsResponsePayload payload = new FetchCommentsResponsePayload(new ArrayList<CommentModel>());
        payload.error = new CommentError(genericToCommentError(error), "");
        return payload;
    }

    public static RemoteCommentResponsePayload commentErrorToPushCommentPayload(BaseNetworkError error,
                                                                                CommentModel comment) {
        RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(comment);
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
        // Duplicate comment on WPCom REST
        if (error instanceof WPComGsonNetworkError) {
            WPComGsonNetworkError wpComGsonNetworkError = (WPComGsonNetworkError) error;
            if ("comment_duplicate".equals(wpComGsonNetworkError.apiError)) {
                errorType = CommentErrorType.DUPLICATE_COMMENT;
            }
        }
        // Duplicate comment on XMLRPC
        if (error.type == GenericErrorType.PARSE_ERROR && error.hasVolleyError()
            && error.volleyError.getCause() instanceof XMLRPCFault
            && ((XMLRPCFault) error.volleyError.getCause()).getFaultCode() == 409) {
            errorType = CommentErrorType.DUPLICATE_COMMENT;
        }
        return errorType;
    }
}
