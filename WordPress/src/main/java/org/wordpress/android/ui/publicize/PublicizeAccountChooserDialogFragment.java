package org.wordpress.android.ui.publicize;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;

public class PublicizeAccountChooserDialogFragment extends DialogFragment
        implements PublicizeAccountChooserListAdapter.OnPublicizeAccountChooserListener {
    public static final String TAG = "publicize-account-chooser-dialog-fragment";
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
        //noinspection InflateParams
        View view = inflater.inflate(R.layout.publicize_account_chooser_dialog, null);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
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
        PublicizeAccountChooserListAdapter notConnectedAdapter =
                new PublicizeAccountChooserListAdapter(getActivity(), mNotConnectedAccounts, this, false);
        notConnectedAdapter.setHasStableIds(true);
        mNotConnectedRecyclerView = view.findViewById(R.id.not_connected_recyclerview);
        mNotConnectedRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mNotConnectedRecyclerView.setAdapter(notConnectedAdapter);

        if (mConnectedAccounts.isEmpty()) {
            hideConnectedView(view);
        } else {
            populateConnectedListView(view);
        }
    }

    private void hideConnectedView(View view) {
        LinearLayout connectedHeader = view.findViewById(R.id.connected_header);
        connectedHeader.setVisibility(View.GONE);
    }

    private void populateConnectedListView(View view) {
        RecyclerView listViewConnected = view.findViewById(R.id.connected_recyclerview);
        PublicizeAccountChooserListAdapter connectedAdapter =
                new PublicizeAccountChooserListAdapter(getActivity(), mConnectedAccounts, null, true);

        listViewConnected.setLayoutManager(new LinearLayoutManager(getActivity()));
        listViewConnected.setAdapter(connectedAdapter);
    }

    private void configureAlertDialog(View view, AlertDialog.Builder builder) {
        builder.setView(view);
        builder.setTitle(getString(R.string.connecting_social_network, mConnectionName));
        builder.setMessage(getString(R.string.connection_chooser_message));
        builder.setPositiveButton(R.string.share_btn_connect, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            int keychainId = mNotConnectedAccounts.get(mSelectedIndex).connectionId;
            String service = mNotConnectedAccounts.get(mSelectedIndex).getService();
            String externalUserId = mNotConnectedAccounts.get(mSelectedIndex).getExternalId();
            EventBus.getDefault().post(new PublicizeEvents.ActionAccountChosen(mSite.getSiteId(), keychainId,
                    service, externalUserId));
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            dialogInterface.cancel();
            ToastUtils.showToast(getActivity(),
                                 getActivity().getString(R.string.cannot_connect_account_error, mConnectionName));
        });
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
                JSONObject currentConnectionJson = jsonArray.getJSONObject(i);
                PublicizeConnection connection = PublicizeConnection.fromJson(currentConnectionJson);
                if (connection.getService().equals(mServiceId)) {
                    PublicizeService service = PublicizeTable.getService(mServiceId);
                    if (service != null && !service.isExternalUsersOnly()) {
                        if (connection.isInSite(mSite.getSiteId())) {
                            mConnectedAccounts.add(connection);
                        } else {
                            mNotConnectedAccounts.add(connection);
                        }
                    }

                    JSONArray externalJsonArray = currentConnectionJson.getJSONArray("additional_external_users");
                    for (int j = 0; j < externalJsonArray.length(); j++) {
                        JSONObject currentExternalConnectionJson = externalJsonArray.getJSONObject(j);
                        PublicizeConnection.updateConnectionfromExternalJson(connection, currentExternalConnectionJson);
                        if (connection.isInSite(mSite.getSiteId())) {
                            mConnectedAccounts.add(connection);
                        } else {
                            mNotConnectedAccounts.add(connection);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void configureConnectionName() {
        if (mNotConnectedAccounts.isEmpty()) {
            return;
        }

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
