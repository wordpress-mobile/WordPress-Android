package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.databinding.AddCategoryBinding;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.models.CategoryNode;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;

import javax.inject.Inject;

public class AddCategoryFragment extends AppCompatDialogFragment {
    private SiteModel mSite;
    private AddCategoryBinding mBinding;

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
        ((WordPress) requireActivity().getApplication()).component().inject(this);

        initSite(savedInstanceState);

        AlertDialog.Builder builder =
                new MaterialAlertDialogBuilder(new ContextThemeWrapper(getActivity(), R.style.PostSettingsTheme));
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        //noinspection InflateParams
        mBinding = AddCategoryBinding.inflate(inflater, null, false);

        loadCategories();

        builder.setView(mBinding.getRoot())
               .setPositiveButton(android.R.string.ok, null)
               .setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) requireDialog();
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
                mSite = (SiteModel) requireActivity().getIntent().getSerializableExtra(WordPress.SITE);
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(requireActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getParentFragmentManager().popBackStack();
        }
    }

    private boolean addCategory() {
        String categoryName = mBinding.categoryName.getText().toString();
        CategoryNode selectedCategory = (CategoryNode) mBinding.parentCategory.getSelectedItem();
        long parentId = (selectedCategory != null) ? selectedCategory.getCategoryId() : 0;

        if (categoryName.replaceAll(" ", "").equals("")) {
            mBinding.categoryName.setError(getText(R.string.cat_name_required));
            return false;
        }

        TermModel newCategory = new TermModel(
                TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY,
                categoryName,
                parentId
        );
        ((SelectCategoriesActivity) requireActivity()).categoryAdded(newCategory);

        return true;
    }

    private void loadCategories() {
        CategoryNode rootCategory = CategoryNode.createCategoryTreeFromList(mTaxonomyStore.getCategoriesForSite(mSite));
        ArrayList<CategoryNode> categoryLevels = CategoryNode.getSortedListOfCategoriesFromRoot(rootCategory);
        categoryLevels.add(0, new CategoryNode(0, 0, getString(R.string.top_level_category_name)));
        if (categoryLevels.size() > 0) {
            ParentCategorySpinnerAdapter categoryAdapter =
                    new ParentCategorySpinnerAdapter(getActivity(), R.layout.categories_row_parent, categoryLevels);
            mBinding.parentCategory.setAdapter(categoryAdapter);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBinding = null;
    }
}
