package org.wordpress.android.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;

import com.actionbarsherlock.app.SherlockDialogFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.stats.WPComLoginActivity;

public class WPAlertDialogFragment extends SherlockDialogFragment implements
    DialogInterface.OnClickListener {
    private static boolean isXMLRPC = false;
    private static boolean isLoadMore = false;
    private static boolean isLearnMore = false;

    public static WPAlertDialogFragment newInstance(String message) {
        return newInstance(message, null, false, null, null);
    }

    // XMLRPC Error
    public static WPAlertDialogFragment newInstance(String message, String error) {
        return newInstance(message, error, true, null, null);
    }

    // Load More Posts Override Warning
    public static WPAlertDialogFragment newInstance(String message, String error,
                                                    boolean loadMore) {
        return newInstance(message, error, loadMore, null, null);
    }

    public static WPAlertDialogFragment newInstance(String message, String error, boolean loadMore,
                                                    String infoTitle, String infoUrl) {
        WPAlertDialogFragment adf = new WPAlertDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("alert-message", message);
        if (error != null) {
            bundle.putString("alert-error", error);
        }
        if (infoTitle != null && infoUrl != null) {
            bundle.putString("info-title", infoTitle);
            bundle.putString("info-url", infoUrl);
        }
        adf.setArguments(bundle);
        isLoadMore = loadMore;
        return adf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setCancelable(true);
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);
    }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
      AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
    if (isXMLRPC) {
        String error = this.getArguments().getString("alert-error");
        if (error == null)
            error = getString(R.string.error_generic);
        String message = this.getArguments().getString("alert-message");
        if (message == null)
            message = getString(R.string.error_generic);

        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(R.string.connection_error);
        b.setMessage((error.contains("code 405")) ? error : message);
        b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        b.setCancelable(true);
        return b.create();
    } else if (isLoadMore) {
        String error = this.getArguments().getString("alert-error");
        String message = this.getArguments().getString("alert-message");
            //invalid credentials
            b.setIcon(android.R.drawable.ic_dialog_alert);
            b.setTitle(error);
            b.setMessage(message);
            b.setCancelable(true);
            b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (getActivity() instanceof OnDialogConfirmListener) {
                        OnDialogConfirmListener act = (OnDialogConfirmListener) getActivity();
                        act.onDialogConfirm();
                    }
                }
            });
            b.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            return b.create();
    } else {
        String infoTitle = this.getArguments().getString("info-title");
        final String infoURL = this.getArguments().getString("info-url");
        String error = this.getArguments().getString("alert-error");
        if (error != null)
            b.setTitle(error);
        else
            b.setTitle(R.string.error);
        b.setPositiveButton("OK", this);
        if (infoTitle != null && infoURL != null) {
            b.setNeutralButton(infoTitle, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(infoURL)));
                }
            });
        }
        b.setMessage(this.getArguments().getString("alert-message"));
        return b.create();
    }
  }

  public interface OnDialogConfirmListener {
      public void onDialogConfirm();
}

@Override
public void onClick(DialogInterface dialog, int which) {

}
}

