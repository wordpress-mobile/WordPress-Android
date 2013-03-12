package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.wordpress.android.R;

public class EditLinkActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.link);

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

        final EditText linkURLET = (EditText) findViewById(R.id.linkURL);
        linkURLET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (linkURLET.getText().toString().equals("")) {
                    linkURLET.setText("http://");
                    linkURLET.setSelection(7);
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

    }

}
