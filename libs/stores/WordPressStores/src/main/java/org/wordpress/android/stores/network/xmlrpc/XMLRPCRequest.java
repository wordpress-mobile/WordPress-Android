package org.wordpress.android.stores.network.xmlrpc;

import android.util.Xml;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.network.BaseRequest;
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

    private final Listener mListener;
    private final XMLRPC mMethod;
    private final Object[] mParams;
    private final XmlSerializer mSerializer = Xml.newSerializer();

    public XMLRPCRequest(String url, XMLRPC method, List<Object> params, Listener listener,
                         ErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        mListener = listener;
        mMethod = method;
        // First params are always username/password
        mParams = params.toArray();
    }

    @Override
    protected void deliverResponse(Object response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<Object> parseNetworkResponse(NetworkResponse response) {
        try {
            String data = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            InputStream is = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
            // TODO: Clean up response here (WordPress can output junk before the xml response (php warnings for
            // example)
            Object obj = XMLSerializerUtils.deserialize(is);
            return Response.success(obj, HttpHeaderParser.parseCacheHeaders(response));
        } catch (XMLRPCFault e) {
            return Response.error(new ParseError(e));
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
    public void deliverError(VolleyError error) {
        super.deliverError(error);
        if (error.getCause() instanceof XMLRPCFault) {
            XMLRPCFault xmlrpcFault = (XMLRPCFault) error.getCause();
            if (xmlrpcFault.getFaultCode() == 401) {
                // Unauthorized
                mOnAuthFailedListener.onAuthFailed(AuthError.UNAUTHORIZED);
            } else if (xmlrpcFault.getFaultCode() == 403) {
                // Not authenticated
                mOnAuthFailedListener.onAuthFailed(AuthError.NOT_AUTHENTICATED);
                return;
            }
        }
        if (error instanceof AuthFailureError) {
            mOnAuthFailedListener.onAuthFailed(AuthError.HTTP_AUTH_ERROR);
            return;
        }
    }
}
