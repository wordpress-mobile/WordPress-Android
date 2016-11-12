package org.wordpress.android.editor.legacy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.wordpress.android.editor.R;

public class EditLinkActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.alert_create_link);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String selectedText = extras.getString("selectedText");
            if (selectedText != null) {
                EditText linkTextET = (EditText) findViewById(R.id.linkText);
                linkTextET.setText(selectedText);
            }
        }

        final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button okButton = (Button) findViewById(R.id.ok);

        final EditText urlEditText = (EditText) findViewById(R.id.linkURL);
        urlEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (urlEditText.getText().toString().equals("")) {
                    urlEditText.setText("http://");
                    urlEditText.setSelection(7);
                }
            }

        });

        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                EditText linkURLET = (EditText) findViewById(R.id.linkURL);
                String linkURL = linkURLET.getText().toString();

                EditText linkTextET = (EditText) findViewById(R.id.linkText);
                String linkText = linkTextET.getText().toString();

                Bundle bundle = new Bundle();
                bundle.putString("linkURL", linkURL);
                if (!linkText.equals("")) {
                    bundle.putString("linkText", linkText);
                }

                Intent mIntent = new Intent();
                mIntent.putExtras(bundle);
                setResult(RESULT_OK, mIntent);
                finish();
            }
        });

        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent mIntent = new Intent();
                setResult(RESULT_CANCELED, mIntent);
                finish();
            }
        });

        // select end of url
        urlEditText.performClick();
    }
}
