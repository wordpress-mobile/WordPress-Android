package org.wordpress.android.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;

public class TrustAllSSLSocketFactory extends SSLSocketFactory {
    private javax.net.ssl.SSLSocketFactory factory;

    public TrustAllSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        super(null);
            try {
                SSLContext sslcontext = SSLContext.getInstance("TLS");
                sslcontext.init(null, new TrustManager[] { new TrustAllManager() }, null);
                factory = sslcontext.getSocketFactory();
                setHostnameVerifier(new AllowAllHostnameVerifier());
            } catch(Exception ex) { }
    }

    public static SocketFactory getDefault() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException { return new TrustAllSSLSocketFactory(); }
    public Socket createSocket() throws IOException { return factory.createSocket(); }
    public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException { return factory.createSocket(socket, s, i, flag); }
    public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr1, int j) throws IOException { return factory.createSocket(inaddr, i, inaddr1, j); }
    public Socket createSocket(InetAddress inaddr, int i) throws IOException { return factory.createSocket(inaddr, i); }
    public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException { return factory.createSocket(s, i, inaddr, j); }
    public Socket createSocket(String s, int i) throws IOException { return factory.createSocket(s, i); }
    public String[] getDefaultCipherSuites() { return factory.getDefaultCipherSuites(); }
    public String[] getSupportedCipherSuites() { return factory.getSupportedCipherSuites(); }
}
