package org.wordpress.android.fluxc.network.xmlrpc;

import android.util.Xml;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;


// TODO: Would be great to use generics / return POJO or model direclty (see GSON code?)
public class XMLRPCRequest extends BaseRequest<Object> {
    private static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE = String.format("text/xml; charset=%s", PROTOCOL_CHARSET);

    private final Listener<? super Object[]> mListener;
    private final XMLRPC mMethod;
    private final Object[] mParams;
    private final XmlSerializer mSerializer = Xml.newSerializer();

    public XMLRPCRequest(String url, XMLRPC method, List<Object> params, Listener<? super Object[]> listener,
                         BaseErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        mListener = listener;
        mMethod = method;
        // First params are always username/password
        mParams = (params == null ? null : params.toArray());
    }

    @Override
    protected void deliverResponse(Object response) {
        deliverResponse(mListener, response);
    }

    @Override
    protected Response<Object> parseNetworkResponse(NetworkResponse response) {
        try {
            String data = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            InputStream is = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
            Object obj = XMLSerializerUtils.deserialize(XMLSerializerUtils.scrubXmlResponse(is));
            return Response.success(obj, createCacheEntry(response));
        } catch (XMLRPCFault e) {
            return Response.error(new VolleyError(e));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (IOException e) {
            AppLog.e(T.API, "Can't deserialize XMLRPC response", e);
            return Response.error(new ParseError(e));
        } catch (XmlPullParserException e) {
            AppLog.e(T.API, "Can't deserialize XMLRPC response", e);
            return Response.error(new ParseError(e));
        } catch (XMLRPCException e) {
            AppLog.e(T.API, "Can't deserialize XMLRPC response", e);
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        try {
            StringWriter stringWriter = XMLSerializerUtils.serialize(mSerializer, mMethod, mParams);
            return stringWriter.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            AppLog.e(T.API, "Can't encode XMLRPC request", e);
        } catch (IOException e) {
            AppLog.e(T.API, "Can't serialize XMLRPC request", e);
        }
        return null;
    }

    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        AuthenticateErrorPayload payload = new AuthenticateErrorPayload(AuthenticationErrorType.GENERIC_ERROR);
        // XMLRPC errors are not managed in the layer below (BaseRequest), so check them here:
        if (error.hasVolleyError() && error.volleyError.getCause() instanceof XMLRPCFault) {
            XMLRPCFault xmlrpcFault = (XMLRPCFault) error.volleyError.getCause();
            if (xmlrpcFault.getFaultCode() == 401) {
                error.type = GenericErrorType.AUTHORIZATION_REQUIRED; // Augmented error
                payload.error.type = AuthenticationErrorType.AUTHORIZATION_REQUIRED;
            } else if (xmlrpcFault.getFaultCode() == 403) {
                error.type = GenericErrorType.NOT_AUTHENTICATED; // Augmented error
                payload.error.type = AuthenticationErrorType.NOT_AUTHENTICATED;
            } else if (xmlrpcFault.getFaultCode() == 404) {
                error.type = GenericErrorType.NOT_FOUND; // Augmented error
            }
            error.message = xmlrpcFault.getMessage();
        }

        // TODO: mOnAuthFailedListener should not be called here and the class/callback should de renamed to something
        // like "onLowNetworkLevelError"
        switch (error.type) {
            case HTTP_AUTH_ERROR:
                payload.error.type = AuthenticationErrorType.HTTP_AUTH_ERROR;
                break;
            case INVALID_SSL_CERTIFICATE:
                payload.error.type = AuthenticationErrorType.INVALID_SSL_CERTIFICATE;
                break;
            default:
                break;
        }

        if (payload.error.type != AuthenticationErrorType.GENERIC_ERROR) {
            mOnAuthFailedListener.onAuthFailed(payload);
        }

        return error;
    }

    /**
     * Helper method to capture the Listener's wildcard parameter type and use it to cast the response before
     * calling {@code onResponse()}.
     */
    private <K> void deliverResponse(final Listener<K> listener, Object rawResponse) {
        // The XMLRPCSerializer always returns an Object - it's up to the client making the request to know whether
        // it's really an Object[] (i.e., when requesting a list of values from the API).
        // We've already restricted the Listener parameterization to Object and Object[], so we know this is returning
        // a 'safe' type - but it's still up to the client to know if an Object or an Object[] is the expected response.
        // So, we're matching the parsed response to the Listener parameter we were given, trusting that the network
        // client knows what it's doing
        @SuppressWarnings("unchecked") K response = (K) rawResponse;
        try {
            listener.onResponse(response);
        } catch (ClassCastException e) {
            // If we aren't returning the type the client was expecting, treat this as an API response parse error
            OnUnexpectedError onUnexpectedError = new OnUnexpectedError(e,
                    "API response parse error: " + e.getMessage());
            onUnexpectedError.addExtra(OnUnexpectedError.KEY_URL, getUrl());
            onUnexpectedError.addExtra(OnUnexpectedError.KEY_RESPONSE, response.toString());
            mOnParseErrorListener.onParseError(onUnexpectedError);
            listener.onResponse(null);
        }
    }
}
