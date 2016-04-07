package org.wordpress.android.ui.people;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;

public class PeopleManagementActivity extends AppCompatActivity {

    private PeopleListFragment mPeopleListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.people_management_activity);

        FragmentManager fragmentManager = getFragmentManager();
        if (mPeopleListFragment == null) {
            mPeopleListFragment = PeopleListFragment.newInstance();

            fragmentManager.beginTransaction()
                    .add(android.R.id.content, mPeopleListFragment)
                    .commit();
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
