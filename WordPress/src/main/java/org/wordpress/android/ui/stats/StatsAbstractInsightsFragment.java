package org.wordpress.android.ui.stats;


import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;


public abstract class StatsAbstractInsightsFragment extends StatsAbstractFragment {
    public static final String TAG = StatsAbstractInsightsFragment.class.getSimpleName();

    private TextView mErrorLabel;
    private LinearLayout mEmptyModulePlaceholder;
    LinearLayout mResultContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_insights_generic_fragment, container, false);
        TextView moduleTitleTextView = (TextView) view.findViewById(R.id.stats_module_title);
        moduleTitleTextView.setText(getTitle());

        mEmptyModulePlaceholder = (LinearLayout) view.findViewById(R.id.stats_empty_module_placeholder);
        mResultContainer = (LinearLayout) view.findViewById(R.id.stats_module_result_container);
        mErrorLabel = (TextView) view.findViewById(R.id.stats_error_text);
        return view;
    }

    @Override
    protected void showPlaceholderUI() {
        mErrorLabel.setVisibility(View.GONE);
        mResultContainer.setVisibility(View.GONE);
        mEmptyModulePlaceholder.setVisibility(View.VISIBLE);
    }

    @Override
    protected void showErrorUI(String label) {
        if (!isAdded()) {
            return;
        }

        // Use the generic error message when the string passed to this method is null.
        if (TextUtils.isEmpty(label)) {
            label = "<b>" + getString(R.string.error_refresh_stats) + "</b>";
        }

        if (label.contains("<")) {
            mErrorLabel.setText(Html.fromHtml(label));
        } else {
            mErrorLabel.setText(label);
        }
        mErrorLabel.setVisibility(View.VISIBLE);
        mResultContainer.setVisibility(View.GONE);
        mEmptyModulePlaceholder.setVisibility(View.GONE);
    }

    /**
     * Insights module all have the same basic implementation of updateUI. Let's provide a common code here.
     */
    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        // Another check that the data is available. At this point it should be available.
        if (!hasDataAvailable()) {
            showErrorUI();
            return;
        }

        // not an error - update the module UI here
        mErrorLabel.setVisibility(View.GONE);
        mResultContainer.setVisibility(View.VISIBLE);
        mEmptyModulePlaceholder.setVisibility(View.GONE);

        mResultContainer.removeAllViews();
    }
}