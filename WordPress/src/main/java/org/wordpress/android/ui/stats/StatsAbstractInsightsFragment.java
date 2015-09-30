package org.wordpress.android.ui.stats;


import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.NoConnectionError;
import com.android.volley.VolleyError;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.exceptions.StatsError;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.NetworkUtils;

import java.io.Serializable;

import de.greenrobot.event.EventBus;


public abstract class StatsAbstractInsightsFragment extends StatsAbstractFragment {
    public static final String TAG = StatsAbstractInsightsFragment.class.getSimpleName();

    Serializable[] mDatamodels;

    StatsResourceVars mResourceVars;

    private TextView mErrorLabel;
    private LinearLayout mEmptyModulePlaceholder;
    protected LinearLayout mResultContainer;

    abstract void customizeUIWithResults(); // This is where all the UI is customized at module level

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mResourceVars = new StatsResourceVars(activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
                Serializable oldData = savedInstanceState.getSerializable(ARG_REST_RESPONSE);
                if (oldData != null && oldData instanceof Serializable[]) {
                    mDatamodels = (Serializable[]) oldData;
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Do not serialize VolleyError, but rewrite in a simple stats Exception.
        // VolleyErrors should be serializable, but for some reason they are not.
        // FIX for https://github.com/wordpress-mobile/WordPress-Android/issues/2228
        if (mDatamodels != null) {
            for (int i=0; i < mDatamodels.length; i++) {
                if (mDatamodels[i] != null && mDatamodels[i] instanceof VolleyError) {
                    VolleyError currentVolleyError = (VolleyError) mDatamodels[i];
                    mDatamodels[i] = StatsUtils.rewriteVolleyError(currentVolleyError, getString(R.string.error_refresh_stats));
                }
            }
        }

        outState.putSerializable(ARG_REST_RESPONSE, mDatamodels);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Init the UI
        if (mDatamodels != null) {
            updateUI();
        } else {
            if (NetworkUtils.isNetworkAvailable(getActivity())) {
                showPlaceholderUI();
                refreshStats();
            } else {
                showErrorUI(new NoConnectionError());
            }
        }
    }


    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (mDatamodels == null) {
            return;
        }

        if (isErrorResponse(0)) {
            showErrorUI(mDatamodels[0]);
            return;
        }

        if (isDataEmpty(0)) {
            // This is just an additional check. We only have 1 endpoint per fragment here.
            // mDatamodels is either null or not empty at position 0
            String label = "<b>" + getString(R.string.error_refresh_stats) + "</b>";
            showErrorUI(label);
            return;
        }

        // not an error - update the module UI here
        mErrorLabel.setVisibility(View.GONE);
        mResultContainer.setVisibility(View.VISIBLE);
        mEmptyModulePlaceholder.setVisibility(View.GONE);

        customizeUIWithResults(); // call the subclass and draw the real UI here
    }

    void showPlaceholderUI() {
        mErrorLabel.setVisibility(View.GONE);
        mResultContainer.setVisibility(View.GONE);
        mEmptyModulePlaceholder.setVisibility(View.VISIBLE);
    }

    protected final void showErrorUI(Serializable error) {
        if (!isAdded()) {
            return;
        }

        String label = "<b>" + getString(R.string.error_refresh_stats) + "</b>";

        if (error instanceof NoConnectionError) {
            label += "<br/>" + getString(R.string.no_network_message);
        }

        if (StatsUtils.isRESTDisabledError(error)) {
            label += "<br/>" + getString(R.string.stats_enable_rest_api_in_jetpack);
        }

        showErrorUI(label);
    }

    protected final void showErrorUI(String label) {
        if (!isAdded()) {
            return;
        }

        // Use the generic error message when the string passed to this method is null.
        if (label == null) {
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


    boolean isDataEmpty(int index) {
        return mDatamodels == null
                || mDatamodels[index] == null
                || isErrorResponse(index);
    }

    boolean isErrorResponse(int index) {
        return mDatamodels != null && mDatamodels[index] != null
                && (mDatamodels[index] instanceof VolleyError || mDatamodels[index] instanceof StatsError);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdated event) {
        if (!isAdded()) {
            return;
        }

        if (!event.mRequestBlogId.equals(StatsUtils.getBlogId(getLocalTableBlogID()))) {
            return;
        }

        if (event.mTimeframe != getTimeframe()) {
            return;
        }

        StatsService.StatsEndpointsEnum sectionToUpdate = event.mEndPointName;
        StatsService.StatsEndpointsEnum[] sectionsToUpdate = getSectionsToUpdate();
        int indexOfDatamodelMatch = -1;
        for (int i = 0; i < getSectionsToUpdate().length; i++) {
            if (sectionToUpdate == sectionsToUpdate[i]) {
                indexOfDatamodelMatch = i;
                break;
            }
        }

        if (-1 == indexOfDatamodelMatch) {
            return;
        }


        if (mDatamodels == null) {
            mDatamodels = new Serializable[getSectionsToUpdate().length];
        }

        mDatamodels[indexOfDatamodelMatch] = event.mResponseObjectModel;
        updateUI();
    }

}