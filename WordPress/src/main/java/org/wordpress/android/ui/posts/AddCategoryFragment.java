package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.models.CategoryNode;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;

import javax.inject.Inject;

public class AddCategoryFragment extends AppCompatDialogFragment {
    private SiteModel mSite;
    private EditText mCategoryEditText;
    private Spinner mParentSpinner;

    @Inject TaxonomyStore mTaxonomyStore;

    public static AddCategoryFragment newInstance(SiteModel site) {
        AddCategoryFragment fragment = new AddCategoryFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ((WordPress) getActivity().getApplication()).component().inject(this);

        initSite(savedInstanceState);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate view
        //noinspection InflateParams
        View view = inflater.inflate(R.layout.add_category, null);
        mCategoryEditText = (EditText) view.findViewById(R.id.category_name);
        mParentSpinner = (Spinner) view.findViewById(R.id.parent_category);

        loadCategories();

        builder.setView(view)
               .setPositiveButton(android.R.string.ok, null)
               .setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (addCategory()) {
                    dismiss();
                }
            }
        });
    }

    private void initSite(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (getArguments() != null) {
                mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            } else {
                mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getFragmentManager().popBackStack();
        }
    }

    private boolean addCategory() {
        String categoryName = mCategoryEditText.getText().toString();
        CategoryNode selectedCategory = (CategoryNode) mParentSpinner.getSelectedItem();
        long parentId = (selectedCategory != null) ? selectedCategory.getCategoryId() : 0;

        if (categoryName.replaceAll(" ", "").equals("")) {
            mCategoryEditText.setError(getText(R.string.cat_name_required));
            return false;
        }

        TermModel newCategory = new TermModel();
        newCategory.setTaxonomy(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);
        newCategory.setName(categoryName);
        newCategory.setParentRemoteId(parentId);
        ((SelectCategoriesActivity) getActivity()).categoryAdded(newCategory);

        return true;
    }

    private void loadCategories() {
        CategoryNode rootCategory = CategoryNode.createCategoryTreeFromList(mTaxonomyStore.getCategoriesForSite(mSite));
        ArrayList<CategoryNode> categoryLevels = CategoryNode.getSortedListOfCategoriesFromRoot(rootCategory);
        categoryLevels.add(0, new CategoryNode(0, 0, getString(R.string.top_level_category_name)));
        if (categoryLevels.size() > 0) {
            ParentCategorySpinnerAdapter categoryAdapter =
                    new ParentCategorySpinnerAdapter(getActivity(), R.layout.categories_row_parent, categoryLevels);
            mParentSpinner.setAdapter(categoryAdapter);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }
}
