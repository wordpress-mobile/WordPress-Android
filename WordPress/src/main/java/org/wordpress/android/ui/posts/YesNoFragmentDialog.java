package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;

import org.wordpress.android.R;

public class YesNoFragmentDialog extends BaseYesNoFragmentDialog {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog));
        builder.setTitle(mTitle)
               .setMessage(mMessage)
               .setPositiveButton(mPositiveButtonLabel, new DialogInterface.OnClickListener() {
                   @Override public void onClick(DialogInterface dialog, int which) {
                       ((BasicYesNoDialogClickInterface) getActivity()).onPositiveClicked(mTag);
                   }
               })
               .setNegativeButton(mNegativeButtonLabel, new DialogInterface.OnClickListener() {
                   @Override public void onClick(DialogInterface dialog, int which) {
                       ((BasicYesNoDialogClickInterface) getActivity()).onNegativeClicked(mTag);
                   }
               })
               .setCancelable(true);
        return builder.create();
    }

    @Override public void onAttach(Context context) {
        super.onAttach(context);
        if (!(getActivity() instanceof BasicYesNoDialogClickInterface)) {
            throw new RuntimeException("Hosting activity must implement BasicYesNoDialogClickInterface");
        }
    }
}
