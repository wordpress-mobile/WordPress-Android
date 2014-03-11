package org.wordpress.android.networking;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import android.content.Context;
import android.net.http.SslCertificate;
import android.os.Bundle;

import org.wordpress.android.Config;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class SelfSignedSSLCertsManager {
    
    private static SelfSignedSSLCertsManager instance;
    
    private File localTrustStoreFile;
    private KeyStore localKeyStore;
    
    private X509Certificate[] lastFailureChain; //Used to hold the last self-signed certificate chain that doesn't pass trusting
    
    private SelfSignedSSLCertsManager(Context ctx) throws IOException, GeneralSecurityException {
        localTrustStoreFile = new File(ctx.getFilesDir(), "self_signed_certs_truststore.bks");
        createLocalKeyStoreFile();
        localKeyStore = loadTrustStore(ctx);
    }
    
    public static synchronized SelfSignedSSLCertsManager getIstance(Context ctx) throws GeneralSecurityException, IOException {
        if (instance == null) {
            instance = new SelfSignedSSLCertsManager(ctx);
        }
        return instance;
    }

    public void addCertificates(X509Certificate[] certs) throws IOException, GeneralSecurityException {
        if (certs==null || certs.length==0)
            return;
        
        for (X509Certificate cert : certs) {
            String alias = hashName(cert.getSubjectX500Principal());
            localKeyStore.setCertificateEntry(alias, cert);
        }
        saveTrustStore();
        
        WordPress.setupVolleyQueue(); //reset the Volley queue Otherwise new certs are not used
    }
    
    public void addCertificate(X509Certificate cert) throws IOException, GeneralSecurityException {
        if (cert==null)
            return;
        
        String alias = hashName(cert.getSubjectX500Principal());
        localKeyStore.setCertificateEntry(alias, cert);
        saveTrustStore();
    }
    
    public KeyStore getLocalKeyStore() {
        return localKeyStore;
    }
    
    private KeyStore loadTrustStore(Context ctx) {
        try {
            KeyStore localTrustStore = KeyStore.getInstance("BKS");
            InputStream in = new FileInputStream(localTrustStoreFile);
            try {
                localTrustStore.load(in, Config.DB_SECRET.toCharArray());
            } finally {
                in.close();
            }
            return localTrustStore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void saveTrustStore() throws IOException, GeneralSecurityException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(localTrustStoreFile);
            localKeyStore.store(out, Config.DB_SECRET.toCharArray());
        } finally {
            out.close();
        }
    }
    
    //Create an empty trust store file if missing
    private void createLocalKeyStoreFile() throws GeneralSecurityException, IOException {
       if (!localTrustStoreFile.exists()) { 
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(localTrustStoreFile);
                KeyStore localTrustStore = KeyStore.getInstance("BKS");
                localTrustStore.load(null, Config.DB_SECRET.toCharArray());
                localTrustStore.store(out, Config.DB_SECRET.toCharArray());
            } finally {
                if (out!=null){
                    try {
                        out.close();
                    } catch (IOException e) {
                        AppLog.e(T.UTILS, e);
                    }
                }
            }
        }
    }
    
    public void emptyLocalKeyStoreFile() {
        if (localTrustStoreFile.exists()) {
            localTrustStoreFile.delete();
        }
        try {
            createLocalKeyStoreFile();
            localKeyStore = KeyStore.getInstance("BKS");
        } catch (GeneralSecurityException e) {
        } catch (IOException e) {
        } 
    }
    
    private static String hashName(X500Principal principal) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(principal.getEncoded());
            String result = Integer.toString(leInt(digest), 16);
            if (result.length() > 8) {
                StringBuffer buff = new StringBuffer();
                int padding = 8 - result.length();
                for (int i = 0; i < padding; i++) {
                    buff.append("0");
                }
                buff.append(result);

                return buff.toString();
            }

            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
    
    private static int leInt(byte[] bytes) {
        int offset = 0;
        return ((bytes[offset++] & 0xff) << 0)
                | ((bytes[offset++] & 0xff) << 8)
                | ((bytes[offset++] & 0xff) << 16)
                | ((bytes[offset] & 0xff) << 24);
    }

    public X509Certificate[] getLastFailureChain() {
        return lastFailureChain;
    }

    public void setLastFailureChain(X509Certificate[] lastFaiulreChain) {
        lastFailureChain = lastFaiulreChain;
    }
    
    public String getLastFailureChainDescription() {
        return (lastFailureChain == null ||  lastFailureChain.length == 0) ? "" :  lastFailureChain[0].toString();
    }
    
    public boolean isCertificateTrusted(SslCertificate cert){
        if (cert==null)
            return false;
        
        Bundle bundle = SslCertificate.saveState(cert);
        X509Certificate x509Certificate;
        byte[] bytes = bundle.getByteArray("x509-certificate");
        if (bytes == null) {
            AppLog.e(T.API, "Cannot load the SSLCertificate bytes from the bundle!");
            x509Certificate = null;
        } else {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate certX509 = certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                x509Certificate = (X509Certificate) certX509;
            } catch (CertificateException e) {
                AppLog.e(T.API, "Cannot generate the X509Certificate with the bytes provided", e);
                x509Certificate = null;
            }
        }
        
        if (x509Certificate==null)
            return false;
        
        // Now I have an X509Certificate I can pass to an X509TrustManager for validation.
        try {
            String certificateAlias = this.getLocalKeyStore().getCertificateAlias(x509Certificate);
            if(certificateAlias != null ) {
                AppLog.w(T.API, "Current certificate " + x509Certificate.getSubjectDN().getName() +" is NOT in KeyStore.");
                return true;
            }
        } catch (KeyStoreException e) {
            AppLog.e(T.API, "Cannot check if the certificate is in KeyStore. Seems that Keystore is not initialized.", e);
        }
        return false;
    }
}