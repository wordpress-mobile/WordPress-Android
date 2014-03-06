package org.wordpress.android.networking;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import android.text.TextUtils;

import org.wordpress.android.datasets.TrustedSslDomainTable;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class WPTrustManager implements X509TrustManager {

    private X509TrustManager defaultTrustManager;
    private X509TrustManager localTrustManager;    
    private X509Certificate[] acceptedIssuers;

    public WPTrustManager() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            defaultTrustManager = findX509TrustManager(tmf);
            if (defaultTrustManager == null) {
                throw new IllegalStateException("Couldn't find X509TrustManager");
            }

            localTrustManager = new LocalStoreX509TrustManager();

            List<X509Certificate> allIssuers = new ArrayList<X509Certificate>();
            for (X509Certificate cert : defaultTrustManager.getAcceptedIssuers()) {
                allIssuers.add(cert);
            }
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


    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        try {
            AppLog.i(T.API, "checkClientTrusted() with default trust manager...");
            defaultTrustManager.checkClientTrusted(chain, authType);
        } catch (CertificateException ce) {
            AppLog.i(T.API, "checkClientTrusted() with local trust manager...");
            localTrustManager.checkClientTrusted(chain, authType);
        }
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        try {
            AppLog.i(T.API, "checkServerTrusted() with default trust manager...");
            defaultTrustManager.checkServerTrusted(chain, authType);
        } catch (CertificateException ce) {
            AppLog.i(T.API, "checkServerTrusted() with local trust manager...");
            localTrustManager.checkServerTrusted(chain, authType);
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return acceptedIssuers;
    }

    static class LocalStoreX509TrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException{
            AppLog.i(T.API, "checkClientTrusted from localstore");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            AppLog.i(T.API, "checkServerTrusted from localstore");
            if(TextUtils.isEmpty(authType))
                throw new IllegalArgumentException("The specified authetication type is empy or null");
            
            if (certs == null || certs.length == 0)
                throw new IllegalArgumentException("The specified certificates chain type is empty or null");
                
            boolean trusted = false;
            for (X509Certificate cert : certs) {
                if (cert==null)
                    continue;
                String name = cert.getSubjectX500Principal() != null ? cert.getSubjectX500Principal().getName() : null;
                if (name==null)
                    continue;
                name = name.toLowerCase();
                try {
                    if (name.indexOf("cn=")>-1)
                        name = name.substring(name.indexOf("cn=")).replace("cn=", "");
                    if (name.indexOf(",")>0)
                        name = name.substring(0,name.indexOf(","));
                    if (TrustedSslDomainTable.isDomainTrusted(name)) {
                        trusted = true;
                    }
                } catch (Exception e) {
                    AppLog.e(T.API, "Can't parse the certificate X500 Principal", e);
                }
            }
            
            if (!trusted)
                throw new CertificateException("Cant validate the certificate");
        }
    }
}