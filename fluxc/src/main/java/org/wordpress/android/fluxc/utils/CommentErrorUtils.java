package org.wordpress.android.fluxc.utils;

import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.LikeModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCFault;
import org.wordpress.android.fluxc.store.CommentStore.CommentError;
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType;
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.FetchedCommentLikesResponsePayload;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentResponsePayload;

import java.util.ArrayList;

public class CommentErrorUtils {
    public static RemoteCommentResponsePayload commentErrorToFetchCommentPayload(BaseNetworkError error,
                                                                                 @Nullable CommentModel comment) {
        RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(comment);
        payload.error = new CommentError(genericToCommentError(error), getErrorMessage(error));
        return payload;
    }

    public static FetchCommentsResponsePayload commentErrorToFetchCommentsPayload(BaseNetworkError error,
                                                                                  SiteModel site) {
        FetchCommentsResponsePayload payload = new FetchCommentsResponsePayload(new ArrayList<CommentModel>(), site,
                0, 0, null);
        payload.error = new CommentError(genericToCommentError(error), getErrorMessage(error));
        return payload;
    }

    public static FetchedCommentLikesResponsePayload commentErrorToFetchedCommentLikesPayload(
            BaseNetworkError error,
            long siteId,
            long commentId,
            boolean requestNextPage,
            boolean hasMore
    ) {
        FetchedCommentLikesResponsePayload payload = new FetchedCommentLikesResponsePayload(
                new ArrayList<LikeModel>(),
                siteId,
                commentId,
                requestNextPage,
                hasMore
        );
        payload.error = new CommentError(genericToCommentError(error), getErrorMessage(error));
        return payload;
    }

    public static RemoteCommentResponsePayload commentErrorToPushCommentPayload(BaseNetworkError error,
                                                                                CommentModel comment) {
        RemoteCommentResponsePayload payload = new RemoteCommentResponsePayload(comment);
        payload.error = new CommentError(genericToCommentError(error), getErrorMessage(error));
        return payload;
    }

    public static CommentError networkToCommentError(BaseNetworkError error) {
        return new CommentError(genericToCommentError(error), getErrorMessage(error));
    }

    private static CommentErrorType genericToCommentError(BaseNetworkError error) {
        CommentErrorType errorType = CommentErrorType.GENERIC_ERROR;
        if (error.isGeneric()) {
            switch (error.type) {
                case INVALID_RESPONSE:
                    errorType = CommentErrorType.INVALID_RESPONSE;
                    break;
            }
        }
        if (error instanceof WPComGsonNetworkError) {
            WPComGsonNetworkError wpComGsonNetworkError = (WPComGsonNetworkError) error;
            // Duplicate comment on WPCom REST
            if ("comment_duplicate".equals(wpComGsonNetworkError.apiError)) {
                errorType = CommentErrorType.DUPLICATE_COMMENT;
            }
            if ("unauthorized".equals(wpComGsonNetworkError.apiError)) {
                errorType = CommentErrorType.AUTHORIZATION_REQUIRED;
            }
            // Note: we also get this "unknown_comment" error we we try to comment on the post with id=0.
            if ("unknown_comment".equals(wpComGsonNetworkError.apiError)) {
                errorType = CommentErrorType.UNKNOWN_COMMENT;
            }
            if ("unknown_post".equals(wpComGsonNetworkError.apiError)) {
                errorType = CommentErrorType.UNKNOWN_POST;
            }
        }
        // Duplicate comment on XMLRPC
        if (error.type == GenericErrorType.PARSE_ERROR && error.hasVolleyError()
            && error.volleyError.getCause() instanceof XMLRPCFault
            && ((XMLRPCFault) error.volleyError.getCause()).getFaultCode() == 409) {
            errorType = CommentErrorType.DUPLICATE_COMMENT;
        }
        // Note: We get a 404 error reply via XMLRPC if the comment or post ids are invalid, there is no way to know
        // the exact underlying error. It's described in the error message "Invalid post ID" for instance, but that
        // error message is localized, so not great for parsing.
        return errorType;
    }

    private static String getErrorMessage(BaseNetworkError error) {
        return error.message;
    }
}
