package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;

public class NewAccountActivity extends Activity {
    static final int CREATE_ACCOUNT_REQUEST = 0;
    static final int EXISTING_COM_ACCOUNT_REQUEST = 1;
    static final int EXISTING_ORG_ACCOUNT_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_account);

        Button createAccountButton = (Button) findViewById(R.id.createWPAccount);
        createAccountButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent signupIntent = new Intent(NewAccountActivity.this, SignupActivity.class);
                startActivityForResult(signupIntent, CREATE_ACCOUNT_REQUEST);
            }
        });

        Button dotComButton = (Button) findViewById(R.id.dotcomExisting);
        dotComButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(NewAccountActivity.this, AccountSetupActivity.class);
                i.putExtra("wpcom", true);
                startActivityForResult(i, EXISTING_COM_ACCOUNT_REQUEST);
            }
        });

        Button dotOrgButton = (Button) findViewById(R.id.dotorgExisting);
        dotOrgButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(NewAccountActivity.this, AccountSetupActivity.class);
                startActivityForResult(i, EXISTING_ORG_ACCOUNT_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CREATE_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK && data != null) {
                    String username = data.getStringExtra("username");
                    if (username != null) {
                        Intent i = new Intent(NewAccountActivity.this, AccountSetupActivity.class);
                        i.putExtra("wpcom", true);
                        i.putExtra("username", username);
                        startActivityForResult(i, EXISTING_COM_ACCOUNT_REQUEST);
                    }
                }
                break;
            case EXISTING_COM_ACCOUNT_REQUEST:
            case EXISTING_ORG_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
