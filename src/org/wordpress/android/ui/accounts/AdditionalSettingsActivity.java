package org.wordpress.android.ui.accounts;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.wordpress.android.R;


public class AdditionalSettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_account_settings);
        
        ((TextView) findViewById(R.id.l_section1)).setText(getResources().getString(R.string.optional_settings).toUpperCase());

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            EditText httpuserET = (EditText)findViewById(R.id.httpuser);
            httpuserET.setText(extras.getString("httpuser"));
            EditText httppasswordET = (EditText)findViewById(R.id.httppassword);
            httppasswordET.setText(extras.getString("httppassword"));
        }

        final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button saveButton = (Button) findViewById(R.id.save);

        saveButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //capture the entered fields *needs validation*
                EditText httpuserET = (EditText)findViewById(R.id.httpuser);
                String httpuser = httpuserET.getText().toString();
                EditText httppasswordET = (EditText)findViewById(R.id.httppassword);
                String httppassword = httppasswordET.getText().toString();

                //exit settings screen
                Bundle bundle = new Bundle();
                bundle.putString("httpuser", httpuser);
                bundle.putString("httppassword", httppassword);
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    }

}
