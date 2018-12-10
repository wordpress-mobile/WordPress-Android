package org.wordpress.android.ui.sitecreation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;

import org.wordpress.android.R;

public abstract class NewSiteCreationBaseFormFragment<SiteCreationListenerType> extends Fragment {
    public static final String EXTRA_SCREEN_TITLE = "extra_screen_title";
    private Button mPrimaryButton;
    private Button mSecondaryButton;

    protected SiteCreationListenerType mSiteCreationListener;

    protected abstract @LayoutRes
    int getContentLayout();

    protected abstract void setupContent(ViewGroup rootView);

    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
    }

    protected Button getPrimaryButton() {
        return mPrimaryButton;
    }

    protected abstract void onHelp();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    protected ViewGroup createMainView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.site_creation_form_screen, container, false);
        ViewStub formContainer = (rootView.findViewById(R.id.site_creation_form_content_stub));
        formContainer.setLayoutResource(getContentLayout());
        formContainer.inflate();
        return rootView;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = createMainView(inflater, container, savedInstanceState);

        setupContent(rootView);

        mPrimaryButton = rootView.findViewById(R.id.primary_button);
        mSecondaryButton = rootView.findViewById(R.id.secondary_button);
        setupBottomButtons(mSecondaryButton, mPrimaryButton);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getScreenTitle());
            // important for accessibility
            getActivity().setTitle(getScreenTitle());
        }
    }

    private String getScreenTitle() {
        Bundle arguments = getArguments();
        if (arguments == null || !arguments.containsKey(EXTRA_SCREEN_TITLE)) {
            throw new IllegalStateException("Required argument screen title is missing.");
        }
        return arguments.getString(EXTRA_SCREEN_TITLE);
    }

    protected void showHomeButton(boolean visible, boolean isCloseButton) {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(visible);
            if (isCloseButton) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAttach(Context context) {
        super.onAttach(context);

        // this will throw if parent activity doesn't implement the login listener interface
        mSiteCreationListener = (SiteCreationListenerType) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSiteCreationListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_site_creation, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            onHelp();
            return true;
        }

        return false;
    }

    protected void hideActionbar() {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }
}
