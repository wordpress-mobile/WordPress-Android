package org.wordpress.android.util;

import android.content.Context;
import android.content.DialogInterface;
import android.net.http.SslCertificate;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AppLog.T;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SelfSignedSSLUtils {
    public interface Callback {
        void certificateTrusted();
    }

    public static void showSSLWarningDialog(@NonNull final Context context,
                                            @NonNull final MemorizingTrustManager memorizingTrustManager,
                                            @Nullable final Callback callback) {
        AlertDialog.Builder alert = new MaterialAlertDialogBuilder(context);
        alert.setTitle(context.getString(org.wordpress.android.R.string.ssl_certificate_error));
        alert.setMessage(context.getString(org.wordpress.android.R.string.ssl_certificate_ask_trust));
        alert.setPositiveButton(org.wordpress.android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Add the certificate to our list
                                        memorizingTrustManager.storeLastFailure();
                                        // Retry login action
                                        if (callback != null) {
                                            callback.certificateTrusted();
                                        }
                                    }
                                }
                               );
        alert.setNeutralButton(org.wordpress.android.R.string.ssl_certificate_details,
                               new DialogInterface.OnClickListener() {
                                   public void onClick(DialogInterface dialog, int which) {
                                       ActivityLauncher.viewSSLCerts(context, memorizingTrustManager.getLastFailure()
                                                                                                    .toString());
                                   }
                               });
        alert.show();
    }

    public static X509Certificate sslCertificateToX509(@Nullable SslCertificate cert) {
        if (cert == null) {
            return null;
        }

        Bundle bundle = SslCertificate.saveState(cert);
        X509Certificate x509Certificate = null;
        byte[] bytes = bundle.getByteArray("x509-certificate");
        if (bytes == null) {
            AppLog.e(T.API, "Cannot load the SSLCertificate bytes from the bundle");
        } else {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate certX509 = certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                x509Certificate = (X509Certificate) certX509;
            } catch (CertificateException e) {
                AppLog.e(T.API, "Cannot generate the X509Certificate with the bytes provided", e);
            }
        }
        return x509Certificate;
    }
}
