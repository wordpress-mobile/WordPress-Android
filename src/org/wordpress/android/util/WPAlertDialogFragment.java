package org.wordpress.android.util;

import org.wordpress.android.R;
import org.wordpress.android.Settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class WPAlertDialogFragment extends DialogFragment implements
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
	  isLoadMore = true;
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
		if (error.contains("code 403")) {
    		//invalid credentials
            b.setIcon(android.R.drawable.ic_dialog_alert);
            b.setTitle(R.string.connection_error);
            b.setMessage(R.string.incorrect_credentials);
            b.setCancelable(true);
            b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent settingsIntent = new Intent(getActivity(), Settings.class);
					getActivity().startActivity(settingsIntent);	
				}
			});
			b.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
						
				}
			});
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
		b.setTitle(R.string.error);
		b.setPositiveButton("OK", this);
		//b.setNegativeButton("Cancel", this);
		b.setMessage(this.getArguments().getString("alert-message"));
		return b.create();
	}
  }
  
  public interface OnDialogConfirmListener {
	  public void onDialogConfirm();
}

@Override
public void onClick(DialogInterface dialog, int which) {
	// TODO Auto-generated method stub
	
}
}

