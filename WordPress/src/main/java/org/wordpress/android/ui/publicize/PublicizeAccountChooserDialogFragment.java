package org.wordpress.android.ui.publicize;

import android.app.Dialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import org.wordpress.android.R;
import org.wordpress.android.databinding.PublicizeConnectionListItemBinding;

/**
 * Created by Will on 6/19/16.
 */
public class PublicizeAccountChooserDialogFragment extends DialogFragment {
    private PublicizeEvents.Connection[] mConnections;
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.publicize_account_chooser_dialog, null);
        builder.setView(view);
        builder.setPositiveButton(R.string.share_btn_connect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                PublicizeActions.connectStepTwo(1, 1);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });


        ListView listView = (ListView) view.findViewById(R.id.listView);
        PublicizeConnectionListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.publicize_connection_list_item, listView, false);

        return super.onCreateDialog(savedInstanceState);
    }

    public void setConnections(PublicizeEvents.Connection[] connections) {
        mConnections = connections;
    }
}
