package org.wordpress.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wordpress.android.stores.network.MemorizingTrustManager;
import org.wordpress.android.ui.ActivityLauncher;

public class SelfSignedSSLUtils {
    public interface Callback {
        void certificateTrusted();
    }
    public static void showSSLWarningDialog(@NonNull final Context context,
                                            @NonNull final MemorizingTrustManager memorizingTrustManager,
                                            @Nullable final Callback callback) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
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
        alert.setNeutralButton(org.wordpress.android.R.string.ssl_certificate_details, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ActivityLauncher.viewSSLCerts(context, memorizingTrustManager.getLastFailure().toString());
            }
        });
        alert.show();
    }
}
