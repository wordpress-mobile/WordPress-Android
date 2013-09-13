package org.wordpress.android.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;

import com.actionbarsherlock.app.SherlockDialogFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.AccountSetupActivity;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.PreferencesActivity;

public class WPAlertDialogFragment extends SherlockDialogFragment implements
    DialogInterface.OnClickListener {
    private static boolean isXMLRPC = false;
    private static boolean isLoadMore = false;

  public static WPAlertDialogFragment newInstance(String message) {
      WPAlertDialogFragment adf = new WPAlertDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString("alert-message", message);
    adf.setArguments(bundle);

    return adf;
  }

  // XMLRPC Error
  public static WPAlertDialogFragment newInstance(String message, String error) {
      WPAlertDialogFragment adf = new WPAlertDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString("alert-message", message);
    bundle.putString("alert-error", error);
    adf.setArguments(bundle);
    isXMLRPC = true;
    return adf;
  }

  // Load More Posts Override Warning
  public static WPAlertDialogFragment newInstance(String message, String error, boolean loadMore) {
      WPAlertDialogFragment adf = new WPAlertDialogFragment();
      Bundle bundle = new Bundle();
      bundle.putString("alert-message", message);
      bundle.putString("alert-error", error);
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
        if (error.contains("code 403") || error.contains("code 503")) {
            //invalid credentials
            b.setIcon(android.R.drawable.ic_dialog_alert);
            b.setTitle(R.string.connection_error);
            
            if (WordPress.currentBlog.isDotcomFlag()) {
                // Remove wpcom password since it is no longer valid
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(this.getActivity().getApplicationContext()).edit();
                editor.remove(WordPress.WPCOM_PASSWORD_PREFERENCE);
                editor.commit();
                b.setMessage(getResources().getText(R.string.incorrect_credentials) + " " + getResources().getText(R.string.please_sign_in));
                b.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent authIntent = new Intent(getActivity(), AccountSetupActivity.class);
                        authIntent.putExtra("wpcom", true);
                        authIntent.putExtra("auth-only", true);
                        getActivity().startActivity(authIntent);
                    }
                });
            } else {
                b.setMessage(getResources().getText(R.string.incorrect_credentials) + " " + getResources().getText(R.string.load_settings));
                b.setCancelable(true);
                b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent settingsIntent = new Intent(getActivity(), BlogPreferencesActivity.class);
                        getActivity().startActivity(settingsIntent);
                    }
                });
                b.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
            }
            return b.create();
        } else {
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
        }
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
                    OnDialogConfirmListener act = (OnDialogConfirmListener) getActivity();
                    act.onDialogConfirm();
                }
            });
            b.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            return b.create();
    }
    else {
        String error = this.getArguments().getString("alert-error");
        if (error != null) 
            b.setTitle(error);
        else
            b.setTitle(R.string.error);
        
        b.setPositiveButton("OK", this);
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

