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

import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.networking.WPTrustManager;

public class TrustUserSSLCertsSocketFactory extends SSLSocketFactory {
    private javax.net.ssl.SSLSocketFactory factory;

    public TrustUserSSLCertsSocketFactory() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        super(null);
            try {
                SSLContext sslcontext = SSLContext.getInstance("TLS");
                TrustManager[] trustAllowedCerts = new TrustManager[]{ new WPTrustManager(SelfSignedSSLCertsManager.getInstance(null).getLocalKeyStore()) };
                sslcontext.init(null, trustAllowedCerts, null);
                factory = sslcontext.getSocketFactory();
                setHostnameVerifier(new AllowAllHostnameVerifier());
            } catch(Exception ex) { }
    }

    public static SocketFactory getDefault() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException { return new TrustUserSSLCertsSocketFactory(); }
    public Socket createSocket() throws IOException { return factory.createSocket(); }
    public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException { return factory.createSocket(socket, s, i, flag); }
    public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr1, int j) throws IOException { return factory.createSocket(inaddr, i, inaddr1, j); }
    public Socket createSocket(InetAddress inaddr, int i) throws IOException { return factory.createSocket(inaddr, i); }
    public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException { return factory.createSocket(s, i, inaddr, j); }
    public Socket createSocket(String s, int i) throws IOException { return factory.createSocket(s, i); }
    public String[] getDefaultCipherSuites() { return factory.getDefaultCipherSuites(); }
    public String[] getSupportedCipherSuites() { return factory.getSupportedCipherSuites(); }
}
