package org.wordpress.android.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class TrustAllManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] cert, String authType) throws CertificateException { }
    public void checkServerTrusted(X509Certificate[] cert, String authType) throws CertificateException { }
    public X509Certificate[] getAcceptedIssuers() { return null; }
}
