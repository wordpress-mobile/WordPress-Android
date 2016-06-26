package org.wordpress.android.ui.publicize;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import org.apache.commons.lang.ArrayUtils;
import org.wordpress.android.R;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeConnectionList;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Created by Will on 6/19/16.
 */
public class PublicizeAccountChooserDialogFragment extends DialogFragment {
    private PublicizeConnection[] mPublicizeConnections;
    private PublicizeConnection[] mNotConnectedAccounts;
    private PublicizeConnection[] mConnectedAccounts;
    private int mCurrentSite;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        retrieveCurrentSite();
        addConnectionsToLists();
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

        ListView listView = (ListView) view.findViewById(R.id.listView_not_connected);
        PublicizeAccountChooserListAdapter adapter = new PublicizeAccountChooserListAdapter(getActivity(), R.layout.publicize_connection_list_item, mNotConnectedAccounts, false);

        ListView listViewConnected = (ListView) view.findViewById(R.id.listview_connected);
        PublicizeAccountChooserListAdapter connectedAdapter = new PublicizeAccountChooserListAdapter(getActivity(), R.layout.publicize_connection_list_item, mConnectedAccounts, true);

        listView.setAdapter(adapter);
        listViewConnected.setAdapter(connectedAdapter);

        AlertDialog dialog = builder.create();
        return dialog;
    }

    public void setConnections(PublicizeConnection[] publicizeConnections) {
        mPublicizeConnections = publicizeConnections;
    }

    private void addConnectionsToLists() {
        ArrayList<PublicizeConnection> unconnected = new ArrayList<>();
        ArrayList<PublicizeConnection> connected = new ArrayList<>();
        for (int i = 0; i < mPublicizeConnections.length; i++) {
            PublicizeConnection connection = mPublicizeConnections[i];
            if (containsCurrentSite(connection.getSites())) {
                connected.add(connection);
            } else {
                unconnected.add(connection);
            }
        }

        mNotConnectedAccounts = new PublicizeConnection[unconnected.size()];
        mConnectedAccounts = new PublicizeConnection[connected.size()];

        for (int i = 0; i < unconnected.size(); i++) {
            mNotConnectedAccounts[i] = unconnected.get(i);
        }

        for (int i = 0; i < connected.size(); i++) {
            mConnectedAccounts[i] = connected.get(i);
        }

    }

    private boolean containsCurrentSite(int[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == mCurrentSite) {
                return true;
            }
        }

        return false;
    }

    private void retrieveCurrentSite() {
        Bundle args = getArguments();
        if (args != null) {
            mCurrentSite = args.getInt("site_id");
        } else {
            mCurrentSite = 0;
        }
    }
}
