package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SiteSettingsTimezoneDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static final String KEY_TIMEZONE = "timezone";

    private class Timezone {
        private final String mLabel;
        private final String mValue;

        private Timezone(String label, String value) {
            mLabel = label;
            mValue = value;
        }
    }

    private boolean mConfirmed;
    private String mSelectedTimezone;

    private ListView mListView;
    private SearchView mSearchView;
    private View mEmptyView;
    private View mProgressView;

    private TimezoneAdapter mAdapter;
    private LayoutInflater mInflater;

    public static SiteSettingsTimezoneDialog newInstance(@NonNull String timezone) {
        Bundle args = new Bundle();
        args.putString(KEY_TIMEZONE, timezone);

        SiteSettingsTimezoneDialog dialog = new SiteSettingsTimezoneDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mInflater = LayoutInflater.from(getActivity());
        //noinspection InflateParams
        View view = mInflater.inflate(R.layout.site_settings_timezone_dialog, null);

        mListView = view.findViewById(R.id.list);
        mListView.setOnItemClickListener((parent, view1, position, id) -> {
            Timezone tz = (Timezone) mAdapter.getItem(position);
            mSelectedTimezone = tz.mValue;
            mAdapter.notifyDataSetChanged();
            hideSearchKeyboard();
        });

        mSearchView = view.findViewById(R.id.search_view);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (mAdapter != null) {
                    mAdapter.getFilter().filter(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (mAdapter != null) {
                    mAdapter.getFilter().filter(newText);
                }
                return true;
            }
        });

        mSearchView.setEnabled(false);
        mSearchView.setIconifiedByDefault(false);
        setSearchViewContentDescription(mSearchView);


        mEmptyView = view.findViewById(R.id.empty_view);
        mProgressView = view.findViewById(R.id.progress_view);

        int topOffset = getResources().getDimensionPixelOffset(R.dimen.settings_fragment_dialog_vertical_inset);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity())
                .setBackgroundInsetTop(topOffset)
                .setBackgroundInsetBottom(topOffset);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(view);

        mSelectedTimezone = getArguments().getString(KEY_TIMEZONE);
        requestTimezones();

        return builder.create();
    }

    private void requestTimezones() {
        Response.Listener<String> listener = response -> {
            AppLog.d(AppLog.T.SETTINGS, "timezones requested");
            if (isAdded()) {
                showProgressView(false);
                if (!TextUtils.isEmpty(response)) {
                    loadTimezones(response);
                } else {
                    AppLog.w(AppLog.T.SETTINGS, "empty response requesting timezones");
                    dismissWithError();
                }
            }
        };

        Response.ErrorListener errorListener = error -> {
            AppLog.e(AppLog.T.SETTINGS, "Error requesting timezones", error);
            if (isAdded()) {
                dismissWithError();
            }
        };

        StringRequest request = new StringRequest(Constants.URL_TIMEZONE_ENDPOINT, listener, errorListener) {
            @Override
            protected Map<String, String> getParams() {
                return RestClientUtils.getRestLocaleParams(getActivity());
            }
        };

        showProgressView(true);
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        queue.add(request);
    }

    private void setSearchViewContentDescription(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof EditText) {
                child.setContentDescription(getString(R.string.search));
            } else if (child instanceof ViewGroup) {
                setSearchViewContentDescription((ViewGroup) child);
            }
        }
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

            // sort by label
            Collections.sort(timezones, (t1, t2) -> StringUtils.compare(t1.mLabel, t2.mLabel));

            mAdapter = new TimezoneAdapter(timezones, mListView.getContext());
            mListView.setAdapter(mAdapter);
            mSearchView.setEnabled(true);

            // give the list time to load before making the selection
            new Handler().postDelayed(() -> {
                if (isAdded()) {
                    int index = mAdapter.indexOfValue(mSelectedTimezone);
                    if (index > -1) {
                        mListView.setSelection(index);
                    }
                }
            }, 100);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.SETTINGS, "Error parsing timezones", e);
            dismissWithError();
        }
    }

    private void dismissWithError() {
        ToastUtils.showToast(getActivity(), R.string.site_settings_timezones_error);
        dismiss();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mConfirmed = which == DialogInterface.BUTTON_POSITIVE;
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // TODO: android.app.Fragment  is deprecated since Android P.
        // Needs to be replaced with android.support.v4.app.Fragment
        // See https://developer.android.com/reference/android/app/Fragment
        android.app.Fragment target = getTargetFragment();
        if (mConfirmed && target != null && !TextUtils.isEmpty(mSelectedTimezone)) {
            Intent intent = new Intent().putExtra(KEY_TIMEZONE, mSelectedTimezone);
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        }

        super.onDismiss(dialog);
    }

    private void showEmptyView(boolean show) {
        if (isAdded()) {
            mEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showProgressView(boolean show) {
        if (isAdded()) {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void hideSearchKeyboard() {
        if (isAdded()) {
            ActivityUtils.hideKeyboardForced(mSearchView);
        }
    }

    private class TimezoneViewHolder {
        private final TextView mTxtLabel;

        TimezoneViewHolder(View view) {
            mTxtLabel = view.findViewById(android.R.id.text1);
        }
    }

    private class TimezoneAdapter extends BaseAdapter implements Filterable {
        private final List<Timezone> mAllTimezones = new ArrayList<>();
        private final List<Timezone> mFilteredTimezones = new ArrayList<>();

        private int mSelectedRowColor;
        private int mUnselectedColor;

        private TimezoneAdapter(@NonNull List<Timezone> timezones, Context context) {
            mAllTimezones.addAll(timezones);
            mFilteredTimezones.addAll(timezones);

            mSelectedRowColor = ColorUtils
                    .setAlphaComponent(
                            ContextExtensionsKt.getColorFromAttribute(context, R.attr.colorOnSurface),
                            context.getResources()
                                   .getInteger(R.integer.custom_popup_selected_list_item_opacity_dec)
                    );

            mUnselectedColor = context.getResources().getColor(android.R.color.transparent);
        }

        @Override
        public int getCount() {
            return mFilteredTimezones.size();
        }

        @Override
        public Object getItem(int position) {
            return mFilteredTimezones.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public int indexOfValue(String timezoneValue) {
            if (!TextUtils.isEmpty(timezoneValue)) {
                for (int i = 0; i < mFilteredTimezones.size(); i++) {
                    if (timezoneValue.equals(mFilteredTimezones.get(i).mValue)) {
                        return i;
                    }
                }
            }
            return -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TimezoneViewHolder holder;
            if (convertView == null || convertView.getTag() == null) {
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                holder = new TimezoneViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (TimezoneViewHolder) convertView.getTag();
            }

            boolean isSelected = mSelectedTimezone != null
                                 && mSelectedTimezone.equals(mFilteredTimezones.get(position).mValue);


            int bgColor = isSelected ? mSelectedRowColor : mUnselectedColor;
            holder.mTxtLabel.setBackgroundColor(bgColor);
            holder.mTxtLabel.setText(mFilteredTimezones.get(position).mLabel);

            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    mFilteredTimezones.clear();
                    mFilteredTimezones.addAll((List<Timezone>) results.values);
                    showEmptyView(mFilteredTimezones.isEmpty());
                    TimezoneAdapter.this.notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    List<Timezone> filtered = new ArrayList<>();
                    if (TextUtils.isEmpty(constraint)) {
                        filtered.addAll(mAllTimezones);
                    } else {
                        String lcConstraint = constraint.toString().toLowerCase(Locale.ROOT);
                        for (Timezone tz : mAllTimezones) {
                            if (tz.mLabel.toLowerCase(Locale.ROOT).contains(lcConstraint)) {
                                filtered.add(tz);
                            }
                        }
                    }

                    FilterResults results = new FilterResults();
                    results.values = filtered;

                    return results;
                }
            };
        }
    }
}
