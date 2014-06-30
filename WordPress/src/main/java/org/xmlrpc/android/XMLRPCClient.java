package org.xmlrpc.android;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Xml;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * A WordPress XMLRPC Client.
 * Based on android-xmlrpc: code.google.com/p/android-xmlrpc/
 * Async support based on aXMLRPC: https://github.com/timroes/aXMLRPC
 */

public class XMLRPCClient implements XMLRPCClientInterface {
    private static final String TAG_METHOD_CALL = "methodCall";
    private static final String TAG_METHOD_NAME = "methodName";
    private static final String TAG_METHOD_RESPONSE = "methodResponse";
    private static final String TAG_PARAMS = "params";
    private static final String TAG_PARAM = "param";
    private static final String TAG_FAULT = "fault";
    private static final String TAG_FAULT_CODE = "faultCode";
    private static final String TAG_FAULT_STRING = "faultString";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 60000;

    private Map<Long,Caller> backgroundCalls = new HashMap<Long, Caller>();

    private DefaultHttpClient mClient;
    private HttpPost mPostMethod;
    private XmlSerializer mSerializer;
    private HttpParams mHttpParams;
    private boolean mIsWpcom;

    /**
     * XMLRPCClient constructor. Creates new instance based on server URI
     * @param XMLRPC server URI
     */
    public XMLRPCClient(URI uri, String httpuser, String httppasswd) {
        mPostMethod = new HttpPost(uri);
        mPostMethod.addHeader("Content-Type", "text/xml");
        mPostMethod.addHeader("charset", "UTF-8");
        mPostMethod.addHeader("User-Agent", WordPress.getUserAgent());

        mHttpParams = mPostMethod.getParams();
        HttpProtocolParams.setUseExpectContinue(mHttpParams, false);

        UsernamePasswordCredentials credentials = null;
        if (!TextUtils.isEmpty(httpuser) && !TextUtils.isEmpty(httppasswd)) {
            credentials = new UsernamePasswordCredentials(httpuser, httppasswd);
        }

        mClient = instantiateClientForUri(uri, credentials);
        mSerializer = Xml.newSerializer();
    }

    private class ConnectionClient extends DefaultHttpClient {
        public ConnectionClient(int port) throws IOException, GeneralSecurityException {
            super();
            TrustUserSSLCertsSocketFactory tasslf = new TrustUserSSLCertsSocketFactory();
            Scheme scheme = new Scheme("https", tasslf, port);
            getConnectionManager().getSchemeRegistry().register(scheme);
        }
    }

    private DefaultHttpClient instantiateClientForUri(URI uri, UsernamePasswordCredentials usernamePasswordCredentials) {
        DefaultHttpClient client = null;
        if (uri != null && uri.getHost() != null && uri.getHost().endsWith("wordpress.com")) {
            mIsWpcom = true;
        }
        if (mIsWpcom || (uri == null || uri.getScheme() == null || uri.getScheme().equals("http"))) {
            //wpcom blog or self-hosted blog on plain HTTP
            client = new DefaultHttpClient();
        } else {
            int port = uri.getPort();
            if (port == -1) {
                port = 443;
            }

            try {
                client = new ConnectionClient(port);
            } catch (GeneralSecurityException e) {
                AppLog.e(T.API, "Cannot create the DefaultHttpClient object with our TrustAllSSLSocketFactory", e);
                client = null;
            } catch (IOException e) {
                AppLog.e(T.API, "Cannot create the DefaultHttpClient object with our TrustAllSSLSocketFactory", e);
                client = null;
            }

            if (client == null) {
                client = new DefaultHttpClient();
            }
        }

        // This is probably superfluous, since we're setting the timeouts in the method parameters. See preparePostMethod
        HttpConnectionParams.setConnectionTimeout(client.getParams(), DEFAULT_CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(client.getParams(), DEFAULT_SOCKET_TIMEOUT);

        //Setup HTTP Basic Auth if necessary
        if (usernamePasswordCredentials != null) {
            BasicCredentialsProvider cP = new BasicCredentialsProvider();
            cP.setCredentials(AuthScope.ANY, usernamePasswordCredentials);
            client.setCredentialsProvider(cP);
        }

        return client;
    }

    public void addQuickPostHeader(String type) {
        mPostMethod.addHeader("WP-QUICK-POST", type);
    }

    /**
     * Convenience constructor. Creates new instance based on server String address
     * @param url server url
     */
    public XMLRPCClient(String url, String httpuser, String httppasswd) {
        this(URI.create(url), httpuser, httppasswd);
    }

    /**
     * Convenience XMLRPCClient constructor. Creates new instance based on server URL
     * @param url server URL
     */
    public XMLRPCClient(URL url, String httpuser, String httppasswd) {
        this(URI.create(url.toExternalForm()), httpuser, httppasswd);
    }

    /**
     * Set WP.com auth header
     * @param authToken authorization token
     */
    public void setAuthorizationHeader(String authToken) {
        if( authToken != null)
            mPostMethod.addHeader("Authorization", String.format("Bearer %s", authToken));
        else
            mPostMethod.removeHeaders("Authorization");
    }

    /**
     * Call method with optional parameters. This is general method.
     * If you want to call your method with 0-8 parameters, you can use more
     * convenience call methods
     *
     * @param method name of method to call
     * @param params parameters to pass to method (may be null if method has no parameters)
     * @return deserialized method return value
     * @throws XMLRPCException
     */
    public Object call(String method, Object[] params) throws XMLRPCException, IOException, XmlPullParserException {
        return call(method, params, null);
    }

    /**
     * Convenience method call with no parameters
     *
     * @param method name of method to call
     * @return deserialized method return value
     * @throws XMLRPCException
     */
    public Object call(String method) throws XMLRPCException, IOException, XmlPullParserException {
        return call(method, null, null);
    }


    public Object call(String method, Object[] params, File tempFile) throws XMLRPCException, IOException, XmlPullParserException {
        return new Caller().callXMLRPC(method, params, tempFile);
    }

    /**
     * Convenience call for callAsync with two paramaters
     *
     * @param XMLRPCCallback listener, XMLRPC methodName, XMLRPC parameters
     * @return unique id of this async call
     * @throws XMLRPCException
     */
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params) {
        return callAsync(listener, methodName, params, null);
    }

    /**
     * Asynchronous XMLRPC call
     *
     * @param XMLRPCCallback listener, XMLRPC methodName, XMLRPC parameters, File for large uploads
     * @return unique id of this async call
     * @throws XMLRPCException
     */
    public long callAsync(XMLRPCCallback listener, String methodName, Object[] params, File tempFile) {
        long id = System.currentTimeMillis();
        new Caller(listener, id, methodName, params, tempFile).start();
        return id;
    }

    @SuppressWarnings("unchecked")
    public static Object parseXMLRPCResponse(InputStream is, HttpEntity entity)
            throws XMLRPCException, IOException, XmlPullParserException, NumberFormatException {
        // setup pull parser
        XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();

        // Many WordPress configs can output junk before the xml response (php warnings for example), this cleans it.
        int bomCheck = -1;
        int stopper = 0;
        while ((bomCheck = is.read()) != -1 && stopper <= 5000) {
            stopper++;
            String snippet = "";
            // 60 == '<' character
            if (bomCheck == 60) {
                for (int i = 0; i < 4; i++) {
                    byte[] chunk = new byte[1];
                    is.read(chunk);
                    snippet += new String(chunk, "UTF-8");
                }
                if (snippet.equals("?xml")) {
                    // it's all good, add xml tag back and start parsing
                    String start = "<" + snippet;
                    List<InputStream> streams = Arrays.asList(new ByteArrayInputStream(start.getBytes()), is);
                    is = new SequenceInputStream(Collections.enumeration(streams));
                    break;
                } else {
                    // keep searching...
                    List<InputStream> streams = Arrays.asList(new ByteArrayInputStream(snippet.getBytes()), is);
                    is = new SequenceInputStream(Collections.enumeration(streams));
                }
            }
        }

        pullParser.setInput(is, "UTF-8");

        // lets start pulling...
        pullParser.nextTag();
        pullParser.require(XmlPullParser.START_TAG, null, TAG_METHOD_RESPONSE);

        pullParser.nextTag(); // either TAG_PARAMS (<params>) or TAG_FAULT (<fault>)
        String tag = pullParser.getName();
        if (tag.equals(TAG_PARAMS)) {
            // normal response
            pullParser.nextTag(); // TAG_PARAM (<param>)
            pullParser.require(XmlPullParser.START_TAG, null, TAG_PARAM);
            pullParser.nextTag(); // TAG_VALUE (<value>)
            // no parser.require() here since its called in XMLRPCSerializer.deserialize() below
            // deserialize result
            Object obj = XMLRPCSerializer.deserialize(pullParser);
            if (entity != null) {
                entity.consumeContent();
            }
            return obj;
        } else if (tag.equals(TAG_FAULT)) {
            // fault response
            pullParser.nextTag(); // TAG_VALUE (<value>)
            // no parser.require() here since its called in XMLRPCSerializer.deserialize() below
            // deserialize fault result
            Map<String, Object> map = (Map<String, Object>) XMLRPCSerializer.deserialize(pullParser);
            String faultString = (String) map.get(TAG_FAULT_STRING);
            int faultCode = (Integer) map.get(TAG_FAULT_CODE);
            if (entity != null) {
                entity.consumeContent();
            }
            throw new XMLRPCFault(faultString, faultCode);
        } else {
            if (entity != null) {
                entity.consumeContent();
            }
            throw new XMLRPCException("Bad tag <" + tag + "> in XMLRPC response - neither <params> nor <fault>");
        }
    }

    public void preparePostMethod(String method, Object[] params, File tempFile) throws IOException, XMLRPCException, IllegalArgumentException, IllegalStateException {
        // prepare POST body
        if (method.equals("wp.uploadFile")) {
            if (!tempFile.exists() && !tempFile.mkdirs()) {
                throw new XMLRPCException("Path to file could not be created.");
            }

            FileWriter fileWriter = new FileWriter(tempFile);
            mSerializer.setOutput(fileWriter);

            mSerializer.startDocument(null, null);
            mSerializer.startTag(null, TAG_METHOD_CALL);
            // set method name
            mSerializer.startTag(null, TAG_METHOD_NAME).text(method).endTag(null, TAG_METHOD_NAME);
            if (params != null && params.length != 0) {
                // set method params
                mSerializer.startTag(null, TAG_PARAMS);
                for (int i = 0; i < params.length; i++) {
                    mSerializer.startTag(null, TAG_PARAM).startTag(null, XMLRPCSerializer.TAG_VALUE);
                    XMLRPCSerializer.serialize(mSerializer, params[i]);
                    mSerializer.endTag(null, XMLRPCSerializer.TAG_VALUE).endTag(null, TAG_PARAM);
                }
                mSerializer.endTag(null, TAG_PARAMS);
            }
            mSerializer.endTag(null, TAG_METHOD_CALL);
            mSerializer.endDocument();

            fileWriter.flush();
            fileWriter.close();

            FileEntity fEntity = new FileEntity(tempFile, "text/xml; charset=\"UTF-8\"");
            fEntity.setContentType("text/xml");
            //fEntity.setChunked(true);
            mPostMethod.setEntity(fEntity);
        } else {
            StringWriter bodyWriter = new StringWriter();
            mSerializer.setOutput(bodyWriter);

            mSerializer.startDocument(null, null);
            mSerializer.startTag(null, TAG_METHOD_CALL);
            // set method name
            mSerializer.startTag(null, TAG_METHOD_NAME).text(method).endTag(null, TAG_METHOD_NAME);
            if (params != null && params.length != 0) {
                // set method params
                mSerializer.startTag(null, TAG_PARAMS);
                for (int i = 0; i < params.length; i++) {
                    mSerializer.startTag(null, TAG_PARAM).startTag(null, XMLRPCSerializer.TAG_VALUE);
                    if (method.equals("metaWeblog.editPost") || method.equals("metaWeblog.newPost")) {
                        XMLRPCSerializer.serialize(mSerializer, params[i]);
                    } else {
                        XMLRPCSerializer.serialize(mSerializer, params[i]);
                    }
                    mSerializer.endTag(null, XMLRPCSerializer.TAG_VALUE).endTag(null, TAG_PARAM);
                }
                mSerializer.endTag(null, TAG_PARAMS);
            }
            mSerializer.endTag(null, TAG_METHOD_CALL);
            mSerializer.endDocument();

            HttpEntity entity = new StringEntity(bodyWriter.toString());
            //Log.i("WordPress", bodyWriter.toString());
            mPostMethod.setEntity(entity);
        }

        //set timeout to 30 seconds, does it need to be set for both mClient and method?
        mClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        mClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
        mPostMethod.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        mPostMethod.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
    }

    /**
     * The Caller class is used to make asynchronous calls to the server.
     * For synchronous calls the Thread function of this class isn't used.
     */
    private class Caller extends Thread {
        private XMLRPCCallback listener;
        private long threadId;
        private String methodName;
        private Object[] params;
        private File tempFile;

        /**
         * Create a new Caller for asynchronous use.
         *
         * @param listener The listener to notice about the response or an error.
         * @param threadId An id that will be send to the listener.
         * @param methodName The method name to call.
         * @param params The parameters of the call or null.
         */
        public Caller(XMLRPCCallback listener, long threadId, String methodName, Object[] params, File tempFile) {
            this.listener = listener;
            this.threadId = threadId;
            this.methodName = methodName;
            this.params = params;
            this.tempFile = tempFile;
        }

        /**
         * Create a new Caller for synchronous use.
         * If the caller has been created with this constructor you cannot use the
         * start method to start it as a thread. But you can call the call method
         * on it for synchronous use.
         */
        public Caller() { }

        /**
         * The run method is invoked when the thread gets started.
         * This will only work, if the Caller has been created with parameters.
         * It execute the call method and notify the listener about the result.
         */
        @Override
        public void run() {
            if(listener == null)
                return;

            try {
                backgroundCalls.put(threadId, this);
                Object o = this.callXMLRPC(methodName, params, tempFile);
                listener.onSuccess(threadId, o);
            } catch(CancelException ex) {
                // Don't notify the listener, if the call has been canceled.
            } catch (Exception ex) {
                listener.onFailure(threadId, ex);
            } finally {
                backgroundCalls.remove(threadId);
            }

        }

        /**
         * Call method with optional parameters
         *
         * @param method name of method to call
         * @param params parameters to pass to method (may be null if method has no parameters)
         * @return deserialized method return value
         * @throws XMLRPCException
         */
        private Object callXMLRPC(String method, Object[] params, File tempFile)
                throws XMLRPCException, IOException, XmlPullParserException {
            LoggedInputStream loggedInputStream = null;
            try {
                preparePostMethod(method, params, tempFile);

                // execute HTTP POST request
                HttpResponse response = mClient.execute(mPostMethod);

                if (response.getStatusLine() == null) // StatusLine is null. We can't read the response code.
                    throw new XMLRPCException( "HTTP Status code is missing!" );

                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();

                if (entity == null) {
                    //This is an error since the parser will fail here.
                    throw new XMLRPCException( "HTTP status code: " + statusCode + " was returned AND no response from the server." );
                }

                if (statusCode == HttpStatus.SC_OK) {
                    loggedInputStream = new LoggedInputStream(entity.getContent());
                    return XMLRPCClient.parseXMLRPCResponse(loggedInputStream, entity);
                }

                String statusLineReasonPhrase = StringUtils.notNullStr(response.getStatusLine().getReasonPhrase());
                try {
                    String responseString = EntityUtils.toString(entity, "UTF-8");
                    if (TextUtils.isEmpty(responseString)) {
                        AppLog.e(T.API, "No HTTP error document document from the server");
                    } else {
                        AppLog.e(T.API, "HTTP error document received from the server: " + responseString);
                    }

                    if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                        //Try to intercept out of memory error here and show a better error message.
                        if (!TextUtils.isEmpty(responseString) && responseString.contains("php fatal error") &&
                                responseString.contains("bytes exhausted")) {
                            String newErrorMsg;
                            if (method.equals("wp.uploadFile")) {
                                newErrorMsg =
                                        "The server doesn't have enough memory to upload this file. You may need to increase the PHP memory limit on your site.";
                            } else {
                                newErrorMsg =
                                        "The server doesn't have enough memory to fulfill the request. You may need to increase the PHP memory limit on your site.";
                            }
                            throw new XMLRPCException( statusLineReasonPhrase + ".\n\n" + newErrorMsg);
                        }
                    }

                } catch (Exception e) {
                    // eat all the exceptions here, we dont want to crash the app when trying to show a
                    // better error message.
                }
                throw new XMLRPCException( "HTTP status code: " + statusCode + " was returned. " + statusLineReasonPhrase);
            } catch (XMLRPCFault e) {
                if (loggedInputStream!=null) {
                    AppLog.w(T.API, "Response document received from the server: " + loggedInputStream.getResponseDocument());
                }
                // Detect login issues and broadcast a message if the error is known
                switch (e.getFaultCode()) {
                    case 403:
                        broadcastAction(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_CREDENTIALS);
                        break;
                    case 425:
                        broadcastAction(WordPress.BROADCAST_ACTION_XMLRPC_TWO_FA_AUTH);
                        break;
                    //TODO: Check the login limit here
                    default:
                        break;
                }
                throw e;
            } catch (XmlPullParserException e) {
                AppLog.e(T.API, "Error while parsing the XML-RPC response document received from the server.", e);
                if (loggedInputStream!=null) {
                    AppLog.e(T.API, "Response document received from the server: " + loggedInputStream.getResponseDocument());
                }
                checkXMLRPCErrorMessage(e);
                throw e;
            } catch (NumberFormatException e) {
                //we can catch NumberFormatException here and re-throw an XMLRPCException.
                //The response document is not a valid XML-RPC document after all.
                AppLog.e(T.API, "Error while parsing the XML-RPC response document received from the server.", e);
                if (loggedInputStream!=null) {
                    AppLog.e(T.API, "Response document received from the server: " + loggedInputStream.getResponseDocument());
                }
                throw new XMLRPCException("The response received contains an invalid number. " + e.getMessage());
            } catch (XMLRPCException e) {
                if (loggedInputStream!=null) {
                    AppLog.e(T.API, "Response document received from the server: " + loggedInputStream.getResponseDocument());
                }
                checkXMLRPCErrorMessage(e);
                throw e;
            } catch (SSLHandshakeException e) {
                if (mIsWpcom) {
                    AppLog.e(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected on wordpress.com");
                } else {
                    AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
                    broadcastAction(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_SSL_CERTIFICATE);
                }
                throw e;
            } catch (SSLPeerUnverifiedException e) {
                if (mIsWpcom) {
                    AppLog.e(T.NUX, "SSLPeerUnverifiedException failed. Erroneous SSL certificate detected on wordpress.com");
                } else {
                    AppLog.w(T.NUX, "SSLPeerUnverifiedException failed. Erroneous SSL certificate detected.");
                    broadcastAction(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_SSL_CERTIFICATE);
                }
                throw e;
            } finally {
                deleteTempFile(method, tempFile);
                try {
                    if (loggedInputStream!=null) {
                        loggedInputStream.close();
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Detect login issues and broadcast a message if the error is known, App Activities should listen to these
     * broadcasted events and present user action to take
     *
     * @return true if error is known and event broadcasted, false else
     */
    private boolean checkXMLRPCErrorMessage(Exception exception) {
        String errorMessage = exception.getMessage().toLowerCase();
        if ((errorMessage.contains("code: 503") || errorMessage.contains("code 503"))//TODO Not sure 503 is the correct error code returned by wpcom
                &&
            (errorMessage.contains("limit reached") || errorMessage.contains("login limit")))
        {
            broadcastAction(WordPress.BROADCAST_ACTION_XMLRPC_LOGIN_LIMIT);
            return true;
        }
        return false;
    }

    private void broadcastAction(String action) {
        WordPress.sendLocalBroadcast(WordPress.getContext(), action);
    }

    private void deleteTempFile(String method, File tempFile) {
        if (tempFile != null) {
            if ((method.equals("wp.uploadFile"))){ //get rid of the temp file
                tempFile.delete();
            }
        }

    }

    private class CancelException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}