package org.wordpress.android.ui.accounts.signup;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.util.EditTextUtils;

public class SiteCreationSiteDetailsFragment extends SiteCreationBaseFormFragment<SiteCreationListener>
        implements TextWatcher {
    public static final String TAG = "site_creation_site_details_fragment_tag";

    private ScrollView mScrollView;
    private WPLoginInputRow mSiteTitleInput;
    private WPLoginInputRow mSiteTaglineInput;

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.site_creation_site_details_screen;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        // important for accessibility - talkback
        getActivity().setTitle(R.string.site_creation_site_details_title);
        mScrollView = rootView.findViewById(R.id.scroll_view);

        mSiteTitleInput = rootView.findViewById(R.id.site_creation_site_title_row);
        mSiteTitleInput.addTextChangedListener(this);
        mSiteTitleInput.setOnEditorCommitListener(new WPLoginInputRow.OnEditorCommitListener() {
            @Override
            public void onEditorCommit() {
                mSiteTaglineInput.getEditText().requestFocus();
            }
        });

        mSiteTaglineInput = rootView.findViewById(R.id.site_creation_site_tagline_row);
        mSiteTaglineInput.addTextChangedListener(this);
        mSiteTaglineInput.setOnEditorCommitListener(new WPLoginInputRow.OnEditorCommitListener() {
            @Override
            public void onEditorCommit() {
                next();
            }
        });

        ViewGroup bottomButtons = rootView.findViewById(R.id.bottom_buttons);
        bottomButtons.setVisibility(View.VISIBLE);
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        primaryButton.setVisibility(View.VISIBLE);
        primaryButton.setEnabled(false); // "Next" is disabled on start until the site title field gets some valid data
        primaryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                next();
            }
        });
    }

    @Override
    protected void onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpSiteDetailsScreen();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_DETAILS_VIEWED);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        showSiteTitleError(null);
        getPrimaryButton().setEnabled(!TextUtils.isEmpty(getCleanedSiteTitle()));
    }

    private void next() {
        final String siteTitle = getCleanedSiteTitle();

        if (TextUtils.isEmpty(siteTitle)) {
            showSiteTitleError(getString(R.string.site_creation_empty_site_title));
            EditTextUtils.showSoftInput(mSiteTitleInput.getEditText());
            return;
        }

        mSiteCreationListener.withSiteDetails(siteTitle, getCleanedSiteTagline());
    }

    private String getCleanedSiteTitle() {
        return EditTextUtils.getText(mSiteTitleInput.getEditText()).trim();
    }

    private String getCleanedSiteTagline() {
        return EditTextUtils.getText(mSiteTaglineInput.getEditText()).trim();
    }

    private void showSiteTitleError(String errorMessage) {
        mSiteTitleInput.setError(errorMessage);

        if (errorMessage != null) {
            requestScrollToView(mSiteTitleInput);
        }
    }

    private void requestScrollToView(final View view) {
        view.post(new Runnable() {
            @Override
            public void run() {
                Rect rect = new Rect(); // coordinates to scroll to
                view.getHitRect(rect);
                mScrollView.requestChildRectangleOnScreen(view, rect, false);
            }
        });
    }
}
