package org.wordpress.android.editor.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.wordpress.android.editor.example.EditorExampleActivity;

public class MainExampleActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        Button newEditorPost1 = (Button) findViewById(R.id.new_editor_post_1);
        newEditorPost1.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainExampleActivity.this, EditorExampleActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(EditorExampleActivity.TITLE_PARAM, getString(R.string.example_post_1_title));
                bundle.putString(EditorExampleActivity.CONTENT_PARAM, getString(R.string.example_post_1_content));
                bundle.putInt(EditorExampleActivity.EDITOR_PARAM, EditorExampleActivity.USE_NEW_EDITOR);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        Button legacyEditorPost1Local = (Button) findViewById(R.id.legacy_editor_post_1_local);
        legacyEditorPost1Local.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainExampleActivity.this, EditorExampleActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(EditorExampleActivity.TITLE_PARAM, getString(R.string.example_post_1_title));
                bundle.putString(EditorExampleActivity.CONTENT_PARAM, getString(R.string.example_post_1_content));
                bundle.putInt(EditorExampleActivity.EDITOR_PARAM, EditorExampleActivity.USE_LEGACY_EDITOR);
                bundle.putBoolean(EditorExampleActivity.DRAFT_PARAM, true);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        Button legacyEditorPost1Remote = (Button) findViewById(R.id.legacy_editor_post_1_remote);
        legacyEditorPost1Remote.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainExampleActivity.this, EditorExampleActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(EditorExampleActivity.TITLE_PARAM, getString(R.string.example_post_1_title));
                bundle.putString(EditorExampleActivity.CONTENT_PARAM, getString(R.string.example_post_1_content));
                bundle.putInt(EditorExampleActivity.EDITOR_PARAM, EditorExampleActivity.USE_LEGACY_EDITOR);
                bundle.putBoolean(EditorExampleActivity.DRAFT_PARAM, false);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }
}
