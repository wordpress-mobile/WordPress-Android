package org.wordpress.android.ui.people;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Person;
import org.wordpress.android.models.Role;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class PersonActivity extends AppCompatActivity {
    public static final String EXTRA_PERSON_ID = "EXTRA_PERSON_ID";

    private int mPersonId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPersonId = getIntent().getExtras().getInt(EXTRA_PERSON_ID);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.person_activity);

        setTitle(R.string.edit_user);

        refreshUserDetails();
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

    private void refreshUserDetails() {
        WPNetworkImageView imgAvatar = (WPNetworkImageView) findViewById(R.id.person_avatar);
        TextView txtDisplayName = (TextView) findViewById(R.id.person_display_name);
        TextView txtUsername = (TextView) findViewById(R.id.person_username);
        TextView txtRole = (TextView) findViewById(R.id.person_role);
        TextView txtRemove = (TextView) findViewById(R.id.person_remove);

        Person person = WordPress.wpDB.getPersonForLocalTablePostId(mPersonId);

        if (person != null) {
            int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
            String avatarUrl = GravatarUtils.fixGravatarUrl(person.getImageUrl(), avatarSz);

            imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
            txtDisplayName.setText(person.getDisplayName());
            txtUsername.setText(person.getUsername());
            txtRole.setText(Role.toString(this, person.getRole()));
            txtRemove.setText(String.format(getString(R.string.remove_user), person.getFirstName().toUpperCase()));

            txtRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // remove user
                }
            });
        }
    }
}
