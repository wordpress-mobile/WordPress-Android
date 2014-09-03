package org.wordpress.android.networking;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class WPTrustManager implements X509TrustManager {
    private X509TrustManager defaultTrustManager;
    private X509TrustManager localTrustManager;
    private X509Certificate[] acceptedIssuers;

    public WPTrustManager(KeyStore localKeyStore) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            defaultTrustManager = findX509TrustManager(tmf);
            if (defaultTrustManager == null) {
                throw new IllegalStateException("Couldn't find X509TrustManager");
            }

            localTrustManager = new LocalStoreX509TrustManager(localKeyStore);

            List<X509Certificate> allIssuers = new ArrayList<X509Certificate>();
            Collections.addAll(allIssuers, defaultTrustManager.getAcceptedIssuers());
            Collections.addAll(allIssuers, localTrustManager.getAcceptedIssuers());
            acceptedIssuers = allIssuers.toArray(new X509Certificate[allIssuers.size()]);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }


    private static X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
        TrustManager tms[] = tmf.getTrustManagers();
        for (int i = 0; i < tms.length; i++) {
            if (tms[i] instanceof X509TrustManager) {
                return (X509TrustManager) tms[i];
            }
        }
        return null;
    }


    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            defaultTrustManager.checkClientTrusted(chain, authType);
        } catch (CertificateException ce) {
            localTrustManager.checkClientTrusted(chain, authType);
        }
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            defaultTrustManager.checkServerTrusted(chain, authType);
        } catch (CertificateException ce) {
            localTrustManager.checkServerTrusted(chain, authType);
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return acceptedIssuers;
    }

    static class LocalStoreX509TrustManager implements X509TrustManager {
        private X509TrustManager trustManager;

        LocalStoreX509TrustManager(KeyStore localKeyStore) {
            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(localKeyStore);

                trustManager = findX509TrustManager(tmf);
                if (trustManager == null) {
                    throw new IllegalStateException("Couldn't find X509TrustManager");
                }
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            trustManager.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                trustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                AppLog.e(T.API, "Cannot trust the certificate with the local trust manager...", e);
                try {
                    SelfSignedSSLCertsManager.getInstance(null).setLastFailureChain(chain);
                } catch (GeneralSecurityException e1) {
                } catch (IOException e1) {
                }
                throw e;
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return trustManager.getAcceptedIssuers();
        }
    }
}