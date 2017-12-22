package org.wordpress.android.ui.prefs;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import static org.wordpress.android.fluxc.action.TaxonomyAction.FETCH_TAGS;

/**
 * A fragment for editing a tag
 */
public class TagDetailFragment extends Fragment {
    private static final String ARGS_TAG_ID = "tag_id";
    static final String TAG = "TagDetailFragment";

    @Inject Dispatcher mDispatcher;
    @Inject TaxonomyStore mTaxonomyStore;

    private EditText mNameView;
    private EditText mDescriptionView;

    private long mTagId;
    private SiteModel mSite;

    public static TagDetailFragment newInstance(@NonNull SiteModel site, long tagRemoteId) {
        TagDetailFragment fragment = new TagDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARGS_TAG_ID, tagRemoteId);
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

        if (getArguments() != null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            mTagId = getArguments().getLong(ARGS_TAG_ID);
        }

        loadTagDetail();
    }

    void loadTagDetail() {
        if (!isAdded()) return;

        TermModel tag = mTaxonomyStore.getTagByRemoteId(mSite, mTagId);
        if (tag == null) {
            ToastUtils.showToast(getActivity(), R.string.error_generic);
            return;
        }

        mNameView.setText(tag.getName());
        mDescriptionView.setText(tag.getDescription());

        mNameView.requestFocus();
        mNameView.setSelection(mNameView.getText().length());
    }

    public void saveChanges() {
        if (!isAdded()) return;

        TermModel tag = mTaxonomyStore.getTagByRemoteId(mSite, mTagId);
        if (tag == null) {
            ToastUtils.showToast(getActivity(), R.string.error_generic);
            return;
        }

        String thisName = EditTextUtils.getText(mNameView);
        String thisDescription = EditTextUtils.getText(mDescriptionView);

        boolean hasChanged = !StringUtils.equals(tag.getName(), thisName)
                || !StringUtils.equals(tag.getDescription(), thisDescription);
        if (hasChanged) {
            tag.setName(thisName);
            tag.setDescription(thisDescription);
            mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(new TaxonomyStore.RemoteTermPayload(tag, mSite)));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(TaxonomyStore.OnTaxonomyChanged event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.SETTINGS, event.error.message);
        } else if (isAdded() && event.causeOfChange == FETCH_TAGS) {
            loadTagDetail();
        }
    }
}
