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
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class PublicizeAccountChooserDialogFragment extends DialogFragment implements PublicizeAccountChooserListAdapter.OnPublicizeAccountChooserListener {
    public static String TAG = "publicize-account-chooser-dialog-fragment";
    private RecyclerView mNotConnectedRecyclerView;
    private ArrayList<PublicizeConnection> mNotConnectedAccounts;
    private ArrayList<PublicizeConnection> mConnectedAccounts;
    private String mConnectionName = "";
    private String mServiceId = "";
    private int mSelectedIndex = 0;
    private SiteModel mSite;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        retrieveCurrentSiteFromArgs();
        configureConnectionName();

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

    private void configureRecyclerViews(View view) {
        PublicizeAccountChooserListAdapter notConnectedAdapter = new PublicizeAccountChooserListAdapter(mNotConnectedAccounts, this, false);
        notConnectedAdapter.setHasStableIds(true);
        mNotConnectedRecyclerView = (RecyclerView) view.findViewById(R.id.not_connected_recyclerview);
        mNotConnectedRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mNotConnectedRecyclerView.setAdapter(notConnectedAdapter);

        if (mConnectedAccounts.isEmpty()) {
            hideConnectedView(view);
        } else {
            populateConnectedListView(view);
        }
    }

    private void hideConnectedView(View view) {
        LinearLayout connectedHeader = (LinearLayout) view.findViewById(R.id.connected_header);
        connectedHeader.setVisibility(View.GONE);
    }

    private void populateConnectedListView(View view) {
        RecyclerView listViewConnected = (RecyclerView) view.findViewById(R.id.connected_recyclerview);
        PublicizeAccountChooserListAdapter connectedAdapter = new PublicizeAccountChooserListAdapter(mConnectedAccounts, null, true);

        listViewConnected.setLayoutManager(new LinearLayoutManager(getActivity()));
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
                EventBus.getDefault().post(new PublicizeEvents.ActionAccountChosen(mSite.getSiteId(), keychainId));
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
    
    private boolean containsSiteId(long[] array) {
        for (long a : array) {
            if (a == mSite.getSiteId()) {
                return true;
            }
        }

        return false;
    }

    private void retrieveCurrentSiteFromArgs() {
        Bundle args = getArguments();
        if (args != null) {
            mSite = (SiteModel) args.getSerializable(WordPress.SITE);
            mServiceId = args.getString(PublicizeConstants.ARG_SERVICE_ID);
            String jsonString = args.getString(PublicizeConstants.ARG_CONNECTION_ARRAY_JSON);
            addConnectionsToLists(jsonString);
        }
    }

    private void addConnectionsToLists(String jsonString) {
        mNotConnectedAccounts = new ArrayList<>();
        mConnectedAccounts = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonObject.getJSONArray("connections");
            for (int i = 0; i < jsonArray.length(); i++) {
                PublicizeConnection connection = PublicizeConnection.fromJson(jsonArray.getJSONObject(i));
                if (connection.getService().equals(mServiceId)) {
                    if (connection.isInSite(mSite.getSiteId())) {
                        mConnectedAccounts.add(connection);
                    } else {
                        mNotConnectedAccounts.add(connection);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void configureConnectionName() {
        PublicizeConnection connection = mNotConnectedAccounts.get(0);
        if (connection != null) {
            mConnectionName = connection.getLabel();
        }
    }

    @Override
    public void onAccountSelected(int selectedIndex) {
        mSelectedIndex = selectedIndex;
        mNotConnectedRecyclerView.getAdapter().notifyDataSetChanged();
    }
}
