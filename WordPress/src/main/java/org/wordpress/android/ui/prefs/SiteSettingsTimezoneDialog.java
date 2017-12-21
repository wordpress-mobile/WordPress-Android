package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteSettingsTimezoneDialog extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String KEY_TIMEZONE = "timezone";
    public static final String KEY_LANGUAGE_CODE = "language_code";

    private class Timezone {
        private final String label;
        private final String value;
        private Timezone(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private boolean mConfirmed;
    private String mSelectedTimezone;
    private String mLanguageCode;

    private ListView mListView;
    private TimezoneAdapter mAdapter;

    public static SiteSettingsTimezoneDialog newInstance(@NonNull String timezone, @NonNull String languageCode) {
        Bundle args = new Bundle();
        args.putString(KEY_TIMEZONE, timezone);
        args.putString(KEY_LANGUAGE_CODE, languageCode);

        SiteSettingsTimezoneDialog dialog = new SiteSettingsTimezoneDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.site_settings_timezone_dialog, null);

        mListView = view.findViewById(R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Timezone tz = (Timezone) mAdapter.getItem(position);
                mSelectedTimezone = tz.value;
                mAdapter.notifyDataSetChanged();
            }
        });

        mSelectedTimezone = getArguments().getString(KEY_TIMEZONE);
        mLanguageCode = getArguments().getString(KEY_LANGUAGE_CODE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(view);

        requestTimezones();

        return builder.create();
    }

    private void requestTimezones() {
        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                AppLog.d(AppLog.T.SETTINGS, "timezones requested");
                if (isAdded() && response != null) {
                    loadTimezones(response);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppLog.e(AppLog.T.SETTINGS, "Error requesting timezones", error);
                if (isAdded()) {
                    dismissWithError();
                }
            }
        };

        String path = "https://public-api.wordpress.com/wpcom/v2/timezones";
        StringRequest request = new StringRequest(path, listener, errorListener) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("locale", mLanguageCode);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(getActivity());
        queue.add(request);
    }

    private void loadTimezones(@NonNull String responseJson) {
        ArrayList<Timezone> timezones = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(responseJson);
            JSONArray jsonTimezones = jsonResponse.getJSONArray("timezones");
            for (int i = 0; i < jsonTimezones.length(); i++) {
                JSONObject json = jsonTimezones.getJSONObject(i);
                timezones.add(
                        new Timezone(json.getString("label"), json.getString("value"))
                );
            }

            mAdapter = new TimezoneAdapter(timezones);
            mListView.setAdapter(mAdapter);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.SETTINGS, "Error parsing timezones", e);
            dismissWithError();
        }
    }

    private void dismissWithError() {
        ToastUtils.showToast(getActivity(), R.string.error_generic);
        dismiss();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mConfirmed = which == DialogInterface.BUTTON_POSITIVE;
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Fragment target = getTargetFragment();
        if (mConfirmed && target != null && !TextUtils.isEmpty(mSelectedTimezone)) {
            Intent intent = new Intent().putExtra(KEY_TIMEZONE, mSelectedTimezone);
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        }

        super.onDismiss(dialog);
    }

    private class TimezoneAdapter extends BaseAdapter {
        private final List<Timezone> mTimezones = new ArrayList<>();

        private TimezoneAdapter(@NonNull List<Timezone> timezones) {
            mTimezones.addAll(timezones);
        }

        @Override
        public int getCount() {
            return mTimezones.size();
        }

        @Override
        public Object getItem(int position) {
            return mTimezones.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = View.inflate(getActivity(), android.R.layout.simple_list_item_1, null);
            TextView txtLabel = view.findViewById(android.R.id.text1);
            txtLabel.setText(mTimezones.get(position).label);

            boolean isSelected = mSelectedTimezone != null && mSelectedTimezone.equals(mTimezones.get(position).value);
            int colorRes = isSelected ? R.color.list_row_selected : R.color.transparent;
            txtLabel.setBackgroundColor(getResources().getColor(colorRes));

            return view;
        }
    }

}
