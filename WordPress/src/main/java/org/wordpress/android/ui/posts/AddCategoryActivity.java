package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
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

public class AddCategoryActivity extends AppCompatActivity {
    public static final String KEY_CATEGORY = "KEY_CATEGORY";

    private SiteModel mSite;

    @Inject TaxonomyStore mTaxonomyStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.add_category);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        loadCategories();

        final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button okButton = (Button) findViewById(R.id.ok);

        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String categoryName = ((EditText) findViewById(R.id.category_name)).getText().toString();
                String categoryDesc = ((EditText) findViewById(R.id.category_desc)).getText().toString();
                Spinner categorySpinner = (Spinner) findViewById(R.id.parent_category);

                CategoryNode selectedCategory = (CategoryNode) categorySpinner.getSelectedItem();
                long parentId = (selectedCategory != null) ? selectedCategory.getCategoryId() : 0;

                if (categoryName.replaceAll(" ", "").equals("")) {
                    //    Name field cannot be empty

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddCategoryActivity.this);
                    dialogBuilder.setTitle(getResources().getText(R.string.required_field));
                    dialogBuilder.setMessage(getResources().getText(R.string.cat_name_required));
                    dialogBuilder.setPositiveButton("OK", new
                            DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Just close the window.

                                }
                            });
                    dialogBuilder.setCancelable(true);
                    dialogBuilder.create().show();
                } else {
                    Bundle bundle = new Bundle();

                    TermModel newCategory = new TermModel();
                    newCategory.setTaxonomy(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);

                    newCategory.setName(categoryName);
                    newCategory.setDescription(categoryDesc);
                    newCategory.setParentRemoteId(parentId);

                    bundle.putSerializable(KEY_CATEGORY, newCategory);

                    Intent mIntent = new Intent();
                    mIntent.putExtras(bundle);
                    setResult(RESULT_OK, mIntent);
                    finish();
                }

            }
        });

        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent mIntent = new Intent();
                setResult(RESULT_CANCELED, mIntent);
                finish();
            }
        });
    }

    private void loadCategories() {
        CategoryNode rootCategory = CategoryNode.createCategoryTreeFromList(mTaxonomyStore.getCategoriesForSite(mSite));
        ArrayList<CategoryNode> categoryLevels = CategoryNode.getSortedListOfCategoriesFromRoot(rootCategory);
        categoryLevels.add(0, new CategoryNode(0, 0, getString(R.string.none)));
        if (categoryLevels.size() > 0) {
            ParentCategorySpinnerAdapter categoryAdapter = new ParentCategorySpinnerAdapter(this,
                    R.layout.categories_row_parent, categoryLevels);
            Spinner sCategories = (Spinner) findViewById(R.id.parent_category);
            sCategories.setAdapter(categoryAdapter);
        }
    }
}
