package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.signup.SiteCreationCreatingFragment;

public class NewBlogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_blog_activity);

        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, new SiteCreationCreatingFragment(), SiteCreationCreatingFragment.TAG);
            fragmentTransaction.commit();

        }
    }
}
