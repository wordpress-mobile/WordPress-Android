package org.wordpress.android.editor.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.wordpress.android.editor.EditorActivity;

public class ExampleActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        Button newEditorPost1 = (Button) findViewById(R.id.new_editor_post_1);
        newEditorPost1.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(ExampleActivity.this, EditorActivity.class));
            }
        });

        Button oldEditorPost1 = (Button) findViewById(R.id.old_editor_post_1);
        oldEditorPost1.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(ExampleActivity.this, EditorActivity.class));
            }
        });
    }
}
