package org.wordpress.android.ui.themes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import org.wordpress.android.R;
import org.wordpress.android.models.Theme;

public class ThemeSupportActivity extends AppCompatActivity {
    private String mThemeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_support_activity);
        loadThemeUrlInWebView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_support, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_activate) {
            Intent intent = new Intent();
            intent.putExtra("themeId", mThemeId);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void loadThemeUrlInWebView() {
        WebView webView = (WebView) findViewById(R.id.webView);
        Intent intent = getIntent();
        mThemeId = intent.getStringExtra("themeId");
        String currentBlog = intent.getStringExtra("currentBlog");
        int type = intent.getIntExtra("type", 0);
        String url = "https://theme.wordpress.com/themes/" + mThemeId + "/";
        if (type == 0) {
            url = url + "support/";
        } else if (type == 2) {
            url = String.format("https://wordpress.com/customize/%s?nomuse=1&theme=pub/%s", currentBlog, mThemeId);
        }

        webView.loadUrl(url);
    }

    public String getThemeId() {
        return mThemeId;
    }
}
