package org.wordpress.android.ui.publicize;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class PublicizeAccountChooserDialogFragment extends DialogFragment implements PublicizeAccountChooserListAdapter.OnPublicizeAccountChooserListener {
    public static String TAG = "publicize-account-chooser-dialog-fragment";
    private RecyclerView mNotConnectedRecyclerView;
    private PublicizeConnection[] mPublicizeConnections;
    private ArrayList<PublicizeConnection> mNotConnectedAccounts;
    private ArrayList<PublicizeConnection> mConnectedAccounts;
    private String mConnectionName = "";
    private int mSelectedIndex = 0;
    private int mSiteId = 0;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        retrieveCurrentSiteFromArgs();
        addConnectionsToLists();

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.publicize_account_chooser_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        configureAlertDialog(view, builder);
        configureRecyclerViews(view);

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if (activity != null && activity instanceof DialogInterface.OnDismissListener) {
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }
    }

    public void setConnections(PublicizeConnection[] publicizeConnections) {
        mPublicizeConnections = publicizeConnections;
    }

    private void configureRecyclerViews(View view) {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());

        mNotConnectedRecyclerView = (RecyclerView) view.findViewById(R.id.not_connected_recyclerview);
        mNotConnectedRecyclerView.setLayoutManager(linearLayoutManager);
        PublicizeAccountChooserListAdapter notConnectedAdapter = new PublicizeAccountChooserListAdapter(mNotConnectedAccounts, this, false);
        notConnectedAdapter.setHasStableIds(true);
        mNotConnectedRecyclerView.setAdapter(notConnectedAdapter);

        RecyclerView listViewConnected = (RecyclerView) view.findViewById(R.id.connected_recyclerview);
        listViewConnected.setLayoutManager(linearLayoutManager);
        PublicizeAccountChooserListAdapter connectedAdapter = new PublicizeAccountChooserListAdapter(mConnectedAccounts, null, true);
        listViewConnected.setAdapter(connectedAdapter);
    }

    private void configureAlertDialog(View view, AlertDialog.Builder builder) {
        builder.setView(view);
        builder.setTitle(getString(R.string.connecting_social_network, mConnectionName));
        builder.setMessage(getString(R.string.connection_chooser_message));
        builder.setPositiveButton(R.string.share_btn_connect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                int keychainId = mNotConnectedAccounts.get(mSelectedIndex).connectionId;
                EventBus.getDefault().post(new PublicizeEvents.ActionAccountChosen(mSiteId, keychainId));
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                ToastUtils.showToast(getActivity(), getActivity().getString(R.string.cannot_connect_account_error, mConnectionName));
            }
        });
    }

    private void addConnectionsToLists() {
        mNotConnectedAccounts = new ArrayList<>();
        mConnectedAccounts = new ArrayList<>();
        for (PublicizeConnection connection : mPublicizeConnections) {
            if (containsSiteId(connection.getSites())) {
                mConnectedAccounts.add(connection);
            } else {
                mNotConnectedAccounts.add(connection);
            }
        }
    }

    private boolean containsSiteId(int[] array) {
        for (int a : array) {
            if (a == mSiteId) {
                return true;
            }
        }

        return false;
    }

    private void retrieveCurrentSiteFromArgs() {
        Bundle args = getArguments();
        if (args != null) {
            mSiteId = args.getInt(PublicizeConstants.ARG_SITE_ID);
            mConnectionName = args.getString(PublicizeConstants.ARG_CONNECTION_NAME);
        }
    }

    @Override
    public void onAccountSelected(int selectedIndex) {
        mSelectedIndex = selectedIndex;
        mNotConnectedRecyclerView.getAdapter().notifyDataSetChanged();
    }
}
