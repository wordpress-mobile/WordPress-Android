package org.wordpress.android.ui.themes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import org.wordpress.android.R;

public class ThemeSupportActivity extends AppCompatActivity {

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_activate) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadThemeUrlInWebView() {
        WebView webView = (WebView) findViewById(R.id.webView);
        Intent intent = getIntent();
        String themeId = intent.getStringExtra("themeId");
        int type = intent.getIntExtra("type", 0);
        String url = "https://theme.wordpress.com/themes/" + themeId + "/";
        if (type == 0) {
            url = url + "support/";
        }
            webView.loadUrl(url);
    }
}
