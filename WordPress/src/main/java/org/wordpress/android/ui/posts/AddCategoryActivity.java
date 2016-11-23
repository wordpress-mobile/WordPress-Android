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
import org.wordpress.android.models.CategoryNode;

import java.util.ArrayList;

public class AddCategoryActivity extends AppCompatActivity {
    private int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_category);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            id = extras.getInt("id");
        }
        loadCategories();

        final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button okButton = (Button) findViewById(R.id.ok);

        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                EditText categoryNameET = (EditText) findViewById(R.id.category_name);
                String category_name = categoryNameET.getText().toString();
                EditText categorySlugET = (EditText) findViewById(R.id.category_slug);
                String category_slug = categorySlugET.getText().toString();
                EditText categoryDescET = (EditText) findViewById(R.id.category_desc);
                String category_desc = categoryDescET.getText().toString();
                Spinner sCategories = (Spinner) findViewById(R.id.parent_category);
                String parent_category = "";
                if (sCategories.getSelectedItem() != null)
                    parent_category = ((CategoryNode) sCategories.getSelectedItem()).getName().trim();
                int parent_id = 0;
                if (sCategories.getSelectedItemPosition() != 0) {
                    parent_id = WordPress.wpDB.getCategoryId(id, parent_category);
                }

                if (category_name.replaceAll(" ", "").equals("")) {
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

                    bundle.putString("category_name", category_name);
                    bundle.putString("category_slug", category_slug);
                    bundle.putString("category_desc", category_desc);
                    bundle.putInt("parent_id", parent_id);
                    bundle.putString("continue", "TRUE");
                    Intent mIntent = new Intent();
                    mIntent.putExtras(bundle);
                    setResult(RESULT_OK, mIntent);
                    finish();
                }

            }
        });

        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Bundle bundle = new Bundle();

                bundle.putString("continue", "FALSE");
                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
            }
        });
    }

    private void loadCategories() {
        CategoryNode rootCategory = CategoryNode.createCategoryTreeFromDB(id);
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
