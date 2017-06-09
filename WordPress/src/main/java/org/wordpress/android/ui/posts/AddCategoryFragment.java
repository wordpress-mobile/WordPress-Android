package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate view
        View view = inflater.inflate(R.layout.add_category, null);
        final EditText categoryEditText = (EditText) view.findViewById(R.id.category_name);
        final Spinner parentSpinner = (Spinner) view.findViewById(R.id.parent_category);

        loadCategories(parentSpinner);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        addCategory(categoryEditText, parentSpinner);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddCategoryFragment.this.getDialog().cancel();
                    }
                });
        return builder.create();
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
            getDialog().cancel();
        }
    }

    private void addCategory(EditText categoryEditText, Spinner parentSpinner) {
        String categoryName = categoryEditText.getText().toString();
        CategoryNode selectedCategory = (CategoryNode) parentSpinner.getSelectedItem();
        long parentId = (selectedCategory != null) ? selectedCategory.getCategoryId() : 0;

        if (categoryName.replaceAll(" ", "").equals("")) {
            // Name field cannot be empty
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setTitle(getResources().getText(R.string.required_field));
            dialogBuilder.setMessage(getResources().getText(R.string.cat_name_required));
            dialogBuilder.setPositiveButton(android.R.string.ok, new
                    DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Just close the window.
                        }
                    });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            TermModel newCategory = new TermModel();
            newCategory.setTaxonomy(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);
            newCategory.setName(categoryName);
            newCategory.setParentRemoteId(parentId);
            ((SelectCategoriesActivity) getActivity()).categoryAdded(newCategory);
        }
    }

    private void loadCategories(Spinner parentSpinner) {
        CategoryNode rootCategory = CategoryNode.createCategoryTreeFromList(mTaxonomyStore.getCategoriesForSite(mSite));
        ArrayList<CategoryNode> categoryLevels = CategoryNode.getSortedListOfCategoriesFromRoot(rootCategory);
        categoryLevels.add(0, new CategoryNode(0, 0, getString(R.string.top_level_category_name)));
        if (categoryLevels.size() > 0) {
            ParentCategorySpinnerAdapter categoryAdapter = new ParentCategorySpinnerAdapter(getActivity(),
                    R.layout.categories_row_parent, categoryLevels);
            parentSpinner.setAdapter(categoryAdapter);
        }
    }
}
