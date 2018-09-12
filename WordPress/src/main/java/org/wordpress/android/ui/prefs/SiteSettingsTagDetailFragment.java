package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.StringUtils;

import static org.wordpress.android.ui.reader.utils.ReaderUtils.sanitizeWithDashes;

/**
 * A fragment for editing a tag
 */
// TODO: android.app.Fragment  is deprecated since Android P.
// Needs to be replaced with android.support.v4.app.Fragment
// See https://developer.android.com/reference/android/app/Fragment
public class SiteSettingsTagDetailFragment extends android.app.Fragment {
    private static final String ARGS_TERM = "term";
    private static final String ARGS_IS_NEW_TERM = "is_new";

    static final String TAG = "TagDetailFragment";

    public interface OnTagDetailListener {
        void onRequestDeleteTag(@NonNull TermModel tag);
    }

    private EditText mNameView;
    private EditText mDescriptionView;

    private TermModel mTerm;
    private boolean mIsNewTerm;
    private OnTagDetailListener mListener;

    /*
     * pass an existing term to edit it, or pass null to create a new one
     */
    public static SiteSettingsTagDetailFragment newInstance(@Nullable TermModel term) {
        SiteSettingsTagDetailFragment fragment = new SiteSettingsTagDetailFragment();
        Bundle args = new Bundle();
        if (term == null) {
            args.putBoolean(ARGS_IS_NEW_TERM, true);
            term = new TermModel();
            term.setTaxonomy(TaxonomyStore.DEFAULT_TAXONOMY_TAG);
        }
        args.putSerializable(ARGS_TERM, term);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.site_settings_tag_detail_fragment, container, false);

        mNameView = view.findViewById(R.id.edit_name);
        mDescriptionView = view.findViewById(R.id.edit_description);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mTerm = (TermModel) getArguments().getSerializable(ARGS_TERM);
        mIsNewTerm = getArguments().getBoolean(ARGS_IS_NEW_TERM);

        if (savedInstanceState == null && !DisplayUtils.isLandscape(getActivity())) {
            EditTextUtils.showSoftInput(mNameView);
        }

        loadTagDetail();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.tag_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_trash).setVisible(!mIsNewTerm);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_trash && mListener != null) {
            mListener.onRequestDeleteTag(mTerm);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void setOnTagDetailListener(@NonNull OnTagDetailListener listener) {
        mListener = listener;
    }

    private void loadTagDetail() {
        if (!isAdded()) {
            return;
        }

        if (mIsNewTerm) {
            getActivity().setTitle(R.string.add_new_tag);
        } else {
            getActivity().setTitle(mTerm.getName());
        }

        mNameView.setText(mTerm.getName());
        mDescriptionView.setText(mTerm.getDescription());

        mNameView.requestFocus();
        mNameView.setSelection(mNameView.getText().length());
    }

    boolean hasChanges() {
        String thisName = EditTextUtils.getText(mNameView);
        String thisDescription = EditTextUtils.getText(mDescriptionView);
        if (mIsNewTerm) {
            return !TextUtils.isEmpty(thisName);
        }
        return !TextUtils.isEmpty(thisName)
               && (!StringUtils.equals(mTerm.getName(), thisName)
                   || !StringUtils.equals(mTerm.getDescription(), thisDescription));
    }

    @NonNull
    TermModel getTerm() {
        String thisName = EditTextUtils.getText(mNameView);
        String thisDescription = EditTextUtils.getText(mDescriptionView);
        mTerm.setName(thisName);
        mTerm.setDescription(thisDescription);
        if (mIsNewTerm) {
            mTerm.setSlug(sanitizeWithDashes(thisName));
        }
        return mTerm;
    }

    boolean isNewTerm() {
        return mIsNewTerm;
    }
}
