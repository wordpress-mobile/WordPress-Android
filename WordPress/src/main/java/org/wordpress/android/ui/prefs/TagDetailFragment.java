package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.ui.reader.utils.ReaderUtils.sanitizeWithDashes;

/**
 * A fragment for editing a tag
 */
public class TagDetailFragment extends Fragment {
    private static final String ARGS_TERM = "term";
    private static final String ARGS_IS_NEW_TERM = "is_new";

    static final String TAG = "TagDetailFragment";

    @Inject Dispatcher mDispatcher;
    @Inject TaxonomyStore mTaxonomyStore;

    private EditText mNameView;
    private EditText mDescriptionView;

    private TermModel mTerm;
    private SiteModel mSite;
    private boolean mIsNewTerm;

    private ProgressDialog mProgressDialog;

    /*
     * use this to edit an existing tag
     */
    public static TagDetailFragment newInstance(@NonNull SiteModel site, @NonNull TermModel term) {
        TagDetailFragment fragment = new TagDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARGS_TERM, term);
        args.putSerializable(WordPress.SITE, site);
        fragment.setArguments(args);
        return fragment;
    }

    /*
     * use this to add a new tag
     */
    public static TagDetailFragment newInstance(@NonNull SiteModel site) {
        TagDetailFragment fragment = new TagDetailFragment();
        TermModel term = new TermModel();
        term.setTaxonomy(TaxonomyStore.DEFAULT_TAXONOMY_TAG);

        Bundle args = new Bundle();
        args.putSerializable(ARGS_TERM, term);
        args.putBoolean(ARGS_IS_NEW_TERM, true);
        args.putSerializable(WordPress.SITE, site);
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
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.tag_detail_fragment, container, false);

        mNameView = view.findViewById(R.id.edit_name);
        mDescriptionView = view.findViewById(R.id.edit_description);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        mTerm = (TermModel) getArguments().getSerializable(ARGS_TERM);
        mIsNewTerm = getArguments().getBoolean(ARGS_IS_NEW_TERM);

        loadTagDetail();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tag_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_trash).setVisible(!mIsNewTerm);
        menu.findItem(R.id.menu_search).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_trash) {
            confirmTrashTag();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadTagDetail() {
        if (!isAdded()) return;

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

    public void saveChanges() {
        if (!isAdded()) return;

        String thisName = EditTextUtils.getText(mNameView);
        String thisDescription = EditTextUtils.getText(mDescriptionView);

        if (TextUtils.isEmpty(thisName)) {
            return;
        }

        if (mIsNewTerm && termExists(thisName)) {
            ToastUtils.showToast(getActivity(), R.string.error_tag_exists);
            return;
        }

        boolean hasChanged = !StringUtils.equals(mTerm.getName(), thisName)
                || !StringUtils.equals(mTerm.getDescription(), thisDescription);
        if (hasChanged) {
            mTerm.setName(thisName);
            mTerm.setDescription(thisDescription);
            if (mIsNewTerm) {
                mTerm.setSlug(sanitizeWithDashes(thisName));
            }
            getArguments().putSerializable(ARGS_TERM, mTerm);
            Action action = TaxonomyActionBuilder.newPushTermAction(new TaxonomyStore.RemoteTermPayload(mTerm, mSite));
            mDispatcher.dispatch(action);
        }
    }

    private boolean termExists(@NonNull String termName) {
        List<TermModel> terms = mTaxonomyStore.getTagsForSite(mSite);
        for (TermModel term: terms) {
            if (termName.equalsIgnoreCase(term.getName())) {
                return true;
            }
        }
        return false;
    }

    private void confirmTrashTag() {
        if (!NetworkUtils.checkConnection(getActivity())) return;

        String message = String.format(getString(R.string.dlg_confirm_trash_tag), mTerm.getName());
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(getResources().getText(R.string.trash));
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton(getResources().getText(R.string.trash_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        trashTag();
                    }
                });
        dialogBuilder.setNegativeButton(getResources().getText(R.string.trash_no), null);
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    private void trashTag() {
        Action action = TaxonomyActionBuilder.newDeleteTermAction(new TaxonomyStore.RemoteTermPayload(mTerm, mSite));
        mDispatcher.dispatch(action);

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.deleting_media_dlg));
        mProgressDialog.show();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(TaxonomyStore.OnTaxonomyChanged event) {
        if (isAdded()) {
            switch (event.causeOfChange) {
                case PUSHED_TERM:
                case UPDATE_TERM:
                    mIsNewTerm = false;
                    getArguments().putBoolean(ARGS_IS_NEW_TERM, false);
                    break;
                case DELETED_TERM:
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    mIsNewTerm = false;
                    getActivity().getFragmentManager().popBackStack();
                    break;
            }
        }
    }
}
