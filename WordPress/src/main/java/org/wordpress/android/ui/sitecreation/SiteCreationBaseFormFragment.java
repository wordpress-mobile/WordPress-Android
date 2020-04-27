package org.wordpress.android.ui.sitecreation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.wordpress.android.R;

public abstract class SiteCreationBaseFormFragment extends Fragment {
    public static final String EXTRA_SCREEN_TITLE = "extra_screen_title";

    protected abstract @LayoutRes
    int getContentLayout();

    protected abstract void setupContent(ViewGroup rootView);

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
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupContent((ViewGroup) getView().getRootView());

        Toolbar toolbar = view.findViewById(R.id.toolbar_main);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getScreenTitle());
            // important for accessibility
            getActivity().setTitle(getScreenTitle());
        }
    }

    protected abstract String getScreenTitle();

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
}
