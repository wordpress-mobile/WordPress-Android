package org.wordpress.android.fluxc.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class MemorizingTrustManager implements X509TrustManager {
    private static final long FUTURE_TASK_TIMEOUT_SECONDS = 10;
    private static final String ANDROID_KEYSTORE_TYPE = "AndroidKeyStore";

    private FutureTask<X509TrustManager> mTrustManagerFutureTask;
    private FutureTask<KeyStore> mLocalKeyStoreFutureTask;
    private X509Certificate mLastFailure;

    @Inject public MemorizingTrustManager() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        mLocalKeyStoreFutureTask = new FutureTask<>(new Callable<KeyStore>() {
            public KeyStore call() {
                return getKeyStore();
            }
        });
        mTrustManagerFutureTask = new FutureTask<>(new Callable<X509TrustManager>() {
            public X509TrustManager call() {
                return getTrustManager(null);
            }
        });
        executorService.execute(mLocalKeyStoreFutureTask);
        executorService.execute(mTrustManagerFutureTask);
    }

    @NonNull
    private KeyStore getLocalKeyStore() {
        try {
            return mLocalKeyStoreFutureTask.get(FUTURE_TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            AppLog.e(T.API, e);
            throw new IllegalStateException("Couldn't find KeyStore");
        }
    }

    @NonNull
    private X509TrustManager getDefaultTrustManager() {
        try {
            return mTrustManagerFutureTask.get(FUTURE_TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            AppLog.e(T.API, e);
            throw new IllegalStateException("Couldn't find X509TrustManager");
        }
    }

    private KeyStore getKeyStore() {
        KeyStore localKeyStore;
        try {
            localKeyStore = loadTrustStore();
        } catch (IOException | GeneralSecurityException e) {
            AppLog.e(T.API, e);
            throw new IllegalStateException(e);
        }
        return localKeyStore;
    }

    private X509TrustManager getTrustManager(@Nullable KeyStore keyStore) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            for (TrustManager t : tmf.getTrustManagers()) {
                if (t instanceof X509TrustManager) {
                    return (X509TrustManager) t;
                }
            }
        } catch (Exception e) {
            // no op
        }
        return null;
    }

    private KeyStore loadTrustStore() throws IOException, GeneralSecurityException {
        KeyStore localKeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_TYPE);
        localKeyStore.load(null);
        return localKeyStore;
    }

    public boolean isCertificateAccepted(X509Certificate cert) {
        try {
            return getLocalKeyStore().getCertificateAlias(cert) != null;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    public void storeLastFailure() {
        storeCert(mLastFailure);
    }

    public void storeCert(X509Certificate cert) {
        try {
            getLocalKeyStore().setCertificateEntry(cert.getSubjectDN().toString(), cert);
        } catch (KeyStoreException e) {
            AppLog.e(T.API, "Unable to store the certificate: " + cert);
        }
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        getDefaultTrustManager().checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            getDefaultTrustManager().checkServerTrusted(chain, authType);
        } catch (CertificateException ce) {
            mLastFailure = chain[0];
            if (isCertificateAccepted(chain[0])) {
                // Certificate has already been accepted by the user
                return;
            }
            throw ce;
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        // return mDefaultTrustManager.getAcceptedIssuers();
        // ^ Original code is super slow (~1200 msecs) - Return an empty list since it seems unused by OkHttp.
        return new X509Certificate[0];
    }

    public X509Certificate getLastFailure() {
        return mLastFailure;
    }

    public void clearLocalTrustStore() {
        KeyStore localKeyStore = getLocalKeyStore();
        try {
            Enumeration<String> aliases = localKeyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (localKeyStore.isCertificateEntry(alias)) {
                    localKeyStore.deleteEntry(alias);
                }
            }
        } catch (KeyStoreException e) {
            AppLog.e(T.API, "Unable to clear KeyStore");
        }
    }

    public HostnameVerifier wrapHostnameVerifier(final HostnameVerifier defaultVerifier) {
        if (defaultVerifier == null) {
            throw new IllegalArgumentException("The default verifier may not be null");
        }

        return new MemorizingHostnameVerifier(defaultVerifier);
    }

    private class MemorizingHostnameVerifier implements HostnameVerifier {
        private HostnameVerifier mDefaultVerifier;

        MemorizingHostnameVerifier(HostnameVerifier hostnameVerifier) {
            mDefaultVerifier = hostnameVerifier;
        }

        @Override
        public boolean verify(String hostname, SSLSession session) {
            // if the default verifier accepts the hostname, we are done
            if (mDefaultVerifier.verify(hostname, session)) {
                return true;
            }
            // otherwise, we check if the hostname is an alias for this cert in our keystore
            try {
                X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
                return cert.equals(getLocalKeyStore().getCertificate(cert.getSubjectDN().toString()));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
