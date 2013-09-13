package org.wordpress.android.ui.accounts;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.conn.HttpHostConnectException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.Utils;

public class AccountSetupActivity extends Activity implements OnClickListener {

    private static final String URL_WORDPRESS = "http://wordpress.com";
    private static final String DEFAULT_IMAGE_SIZE = "2000";

    private XMLRPCClient mClient;
    private String mBlogURL, mXmlrpcURL;
    private ProgressDialog mProgressDialog;
    private String mHttpuser = "";
    private String mHttppassword = "";
    private boolean mIsWpcom = false, mAuthOnly = false;
    private int mBlogCtr = 0;
    private ArrayList<CharSequence> mBlogNames = new ArrayList<CharSequence>();
    private boolean mIsCustomURL = false;
    private ConnectivityManager mSystemService;
    private EditText mUrlEdit;
    private EditText mUsernameEdit;
    private EditText mPasswordEdit;
    private Button mSettingsButton;
    private Button mSaveButton;
    private Button mSignUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_account);
        mSettingsButton = (Button) findViewById(R.id.settingsButton);
        mSaveButton = (Button) findViewById(R.id.save);
        mSignUpButton = (Button) findViewById(R.id.wordpressdotcom);
        mUrlEdit = (EditText) findViewById(R.id.url);
        mUsernameEdit = (EditText) findViewById(R.id.username);
        mPasswordEdit = (EditText) findViewById(R.id.password);
        
        ((TextView) findViewById(R.id.l_section1)).setText(getResources().getString(R.string.account_details).toUpperCase());

        mSystemService = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mIsWpcom = extras.getBoolean("wpcom", false);
            mAuthOnly = extras.getBoolean("auth-only", false);
            String username = extras.getString("username");
            if (username != null) {
                mUsernameEdit.setText(username);
            }
        }

        if (mIsWpcom) {
            ((EditText) findViewById(R.id.url)).setVisibility(View.GONE);
        } else {
            ImageView logo = (ImageView) findViewById(R.id.wpcomLogo);
            logo.setImageDrawable(getResources().getDrawable(R.drawable.wplogo));
        }

        if (mIsWpcom) {
            mSettingsButton.setVisibility(View.GONE);
            if (!mAuthOnly && WordPress.hasValidWPComCredentials(this)) {
                setupBlogs();
            }
        }
        else {
            if (mAuthOnly) {
                Blog currentBlog = WordPress.getCurrentBlog();
                if (currentBlog != null) {
                    mUrlEdit.setText(currentBlog.getHomeURL());
                    mUsernameEdit.requestFocus();
                }
            }
            mSettingsButton.setOnClickListener(this);
        }

        mSaveButton.setOnClickListener(this);
        mSignUpButton.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == R.id.settingsButton) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                mHttpuser = extras.getString("httpuser");
                mHttppassword = extras.getString("httppassword");
            }
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void configureAccount() {

        if (mIsWpcom) {
            mBlogURL = URL_WORDPRESS;
        } else {
            mBlogURL = mUrlEdit.getText().toString().trim();
        }
        final String username = mUsernameEdit.getText().toString().trim();
        final String password = mPasswordEdit.getText().toString().trim();

        if (mBlogURL.equals("") || username.equals("") || password.equals("")) {
            mProgressDialog.dismiss();
            AlertUtil.showAlert(AccountSetupActivity.this, R.string.required_fields, R.string.url_username_password_required);
            return;
        }

        // add http to the beginning of the URL if needed
        if (!(mBlogURL.toLowerCase().startsWith("http://")) && !(mBlogURL.toLowerCase().startsWith("https://"))) {
            mBlogURL = "http://" + mBlogURL; // default to http
        }

        if (!URLUtil.isValidUrl(mBlogURL)) {
            mProgressDialog.dismiss();
            AlertUtil.showAlert(AccountSetupActivity.this, R.string.invalid_url, R.string.invalid_url_message);
            return;
        }

        // attempt to get the XMLRPC URL via RSD
        String rsdUrl = getRSDMetaTagHrefRegEx(mBlogURL);
        if (rsdUrl == null) {
            rsdUrl = getRSDMetaTagHref(mBlogURL);
        }

        if (rsdUrl != null) {
            mXmlrpcURL = ApiHelper.getXMLRPCUrl(rsdUrl);
            if (mXmlrpcURL == null)
                mXmlrpcURL = rsdUrl.replace("?rsd", "");
        } else {
            mIsCustomURL = false;
            // try the user entered path
            try {
                mClient = new XMLRPCClient(mBlogURL, mHttpuser, mHttppassword);
                try {
                    mClient.call("system.listMethods");
                    mXmlrpcURL = mBlogURL;
                    mIsCustomURL = true;
                } catch (XMLRPCException e) {
                    // guess the xmlrpc path
                    String guessURL = mBlogURL;
                    if (guessURL.substring(guessURL.length() - 1, guessURL.length()).equals("/")) {
                        guessURL = guessURL.substring(0, guessURL.length() - 1);
                    }
                    guessURL += "/xmlrpc.php";
                    mClient = new XMLRPCClient(guessURL, mHttpuser, mHttppassword);
                    try {
                        mClient.call("system.listMethods");
                        mXmlrpcURL = guessURL;
                    } catch (XMLRPCException ex) {
                    }
                }
            } catch (Exception e) {
            }
        }

        if (mXmlrpcURL == null) {
            mProgressDialog.dismiss();
            AlertUtil.showAlert(AccountSetupActivity.this, R.string.error, R.string.no_site_error);
        } else {
            
            //Valide the URL found before calling the client. Prevent a crash that can occur during the setup of self-hosted sites.
            try {
                URI.create(mXmlrpcURL);
            } catch (Exception e1) {
                mProgressDialog.dismiss();
                AlertUtil.showAlert(AccountSetupActivity.this, R.string.error, R.string.no_site_error);
                return;
            }
            
            // verify settings
            mClient = new XMLRPCClient(mXmlrpcURL, mHttpuser, mHttppassword);

            XMLRPCMethod method = new XMLRPCMethod("wp.getUsersBlogs", new XMLRPCMethodCallback() {

                public void callFinished(Object[] result) {

                    Blog currentBlog = WordPress.getCurrentBlog();
                    if (mIsWpcom) {
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(AccountSetupActivity.this);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(WordPress.WPCOM_USERNAME_PREFERENCE, username);
                        editor.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, WordPressDB.encryptPassword(password));
                        editor.commit();
                        // fire off a request to get an access token
                        WordPress.restClient.get("me", null, null);
                    }

                    if (mAuthOnly) {
                        if (currentBlog != null) {
                            if (mIsWpcom) {
                                WordPress.wpDB.updateWPComCredentials(username, password);
                                if (currentBlog != null && currentBlog.isDotcomFlag()) {
                                    currentBlog.setPassword(password);
                                }
                            } else {
                                currentBlog.setPassword(password);
                            }
                            currentBlog.save("");
                        }
                        setResult(RESULT_OK);
                        finish();
                        return;
                    }
                    
                    Arrays.sort(result, Utils.BlogNameComparator);

                    final String[] blogNames = new String[result.length];
                    final String[] urls = new String[result.length];
                    final String[] homeURLs = new String[result.length];
                    final int[] blogIds = new int[result.length];
                    final boolean[] wpcoms = new boolean[result.length];
                    final String[] wpVersions = new String[result.length];
                    final boolean[] isAdmins = new boolean[result.length];
                    Map<Object, Object> contentHash = new HashMap<Object, Object>();
                    mBlogCtr = 0;
                    // loop this!
                    for (int ctr = 0; ctr < result.length; ctr++) {
                        contentHash = (Map<Object, Object>) result[ctr];

                        String blogName = contentHash.get("blogName").toString();
                        if (blogName.length() == 0) {
                            blogName = contentHash.get("url").toString();
                        }
                        blogNames[mBlogCtr] = blogName;

                        if (mIsCustomURL)
                            urls[mBlogCtr] = mBlogURL;
                        else
                            urls[mBlogCtr] = contentHash.get("xmlrpc").toString();
                        homeURLs[mBlogCtr] = contentHash.get("url").toString();
                        blogIds[mBlogCtr] = Integer.parseInt(contentHash.get("blogid").toString());
                        isAdmins[mBlogCtr] = Boolean.parseBoolean(contentHash.get("isAdmin").toString());
                        String blogURL = urls[mBlogCtr];

                        mBlogNames.add(StringUtils.unescapeHTML(blogNames[mBlogCtr].toString()));

                        boolean wpcomFlag = false;
                        // check for wordpress.com
                        if (blogURL.toLowerCase().contains("wordpress.com")) {
                            wpcomFlag = true;
                        }
                        wpcoms[mBlogCtr] = wpcomFlag;

                        // attempt to get the software version
                        String wpVersion = "";
                        if (!wpcomFlag) {
                            Map<String, String> hPost = new HashMap<String, String>();
                            hPost.put("software_version", "software_version");
                            Object[] vParams = { 1, username, password, hPost };
                            Object versionResult = new Object();
                            try {
                                versionResult = (Object) mClient.call("wp.getOptions", vParams);
                            } catch (XMLRPCException e) {
                            }

                            if (versionResult != null) {
                                try {
                                    contentHash = (Map<Object, Object>) versionResult;
                                    Map<?, ?> sv = (Map<?, ?>) contentHash.get("software_version");
                                    wpVersion = sv.get("value").toString();
                                } catch (Exception e) {
                                }
                            }
                        } else {
                            wpVersion = "3.5";
                        }

                        wpVersions[mBlogCtr] = wpVersion;
                        mBlogCtr++;
                    } // end loop
                    mProgressDialog.dismiss();
                    if (mBlogCtr == 0) {
                        String additionalText = "";
                        if (result.length > 0) {
                            additionalText = getString(R.string.additional);
                        }
                        AlertUtil.showAlert(AccountSetupActivity.this, R.string.no_blogs_found,
                                String.format(getString(R.string.no_blogs_message), additionalText), getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                });
                    } else {
                        // take them to the blog selection screen if
                        // there's more than one blog
                        if (mBlogCtr > 1) {

                            LayoutInflater inflater = (LayoutInflater) AccountSetupActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            final ListView lv = (ListView) inflater.inflate(R.layout.select_blogs_list, null);
                            lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                            lv.setItemsCanFocus(false);

                            ArrayAdapter<CharSequence> blogs = new ArrayAdapter<CharSequence>(AccountSetupActivity.this, R.layout.blogs_row,
                                    mBlogNames);

                            lv.setAdapter(blogs);

                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AccountSetupActivity.this);
                            dialogBuilder.setTitle(R.string.select_blogs);
                            dialogBuilder.setView(lv);
                            dialogBuilder.setNegativeButton(R.string.add_selected, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    SparseBooleanArray selectedItems = lv.getCheckedItemPositions();

                                    for (int i = 0; i < selectedItems.size(); i++) {
                                        if (selectedItems.get(selectedItems.keyAt(i)) == true) {
                                            int rowID = selectedItems.keyAt(i);
                                            long blogID = -1;

                                            blogID = WordPress.wpDB.checkMatch(blogNames[rowID], urls[rowID], username, password);
                                            if (blogID == -1) {
                                                blogID = WordPress.wpDB.addAccount(urls[rowID], homeURLs[rowID], blogNames[rowID], username, password, mHttpuser,
                                                        mHttppassword, "Above Text", false, false, DEFAULT_IMAGE_SIZE, 20, false, blogIds[rowID],
                                                        wpcoms[rowID], wpVersions[rowID], isAdmins[rowID]);
                                            }
                                            //Set the first blog in the list to the currentBlog
                                            if (i == 0) {
                                                if (blogID >= 0) {
                                                    WordPress.setCurrentBlog((int) blogID);
                                                }
                                            }
                                        }
                                    }

                                    setResult(RESULT_OK);
                                    finish();

                                }
                            });
                            dialogBuilder.setPositiveButton(R.string.add_all, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    for (int i = 0; i < mBlogCtr; i++) {
                                        long blogID;
                                        blogID = WordPress.wpDB.checkMatch(blogNames[i], urls[i], username, password);
                                        if (blogID == -1) {
                                            blogID = WordPress.wpDB.addAccount(urls[i], homeURLs[i], blogNames[i], username, password, mHttpuser, mHttppassword,
                                                    "Above Text", false, false, DEFAULT_IMAGE_SIZE, 5, false, blogIds[i], wpcoms[i], wpVersions[i], isAdmins[i]);
                                        }
                                        //Set the first blog in the list to the currentBlog
                                        if (i == 0) {
                                            if (blogID >= 0) {
                                                WordPress.setCurrentBlog((int) blogID);
                                            }
                                        }
                                    }

                                    setResult(RESULT_OK);
                                    finish();
                                }
                            });
                            dialogBuilder.setOnCancelListener(new OnCancelListener() {
                                
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    mBlogNames.clear();
                                }
                            });
                            dialogBuilder.setCancelable(true);
                            AlertDialog ad = dialogBuilder.create();
                            ad.setInverseBackgroundForced(true);
                            ad.show();

                            final Button addSelected = ad.getButton(AlertDialog.BUTTON_NEGATIVE);
                            addSelected.setEnabled(false);

                            lv.setOnItemClickListener(new OnItemClickListener() {
                                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                                    SparseBooleanArray selectedItems = lv.getCheckedItemPositions();
                                    boolean isChecked = false;
                                    for (int i = 0; i < selectedItems.size(); i++) {
                                        if (selectedItems.get(selectedItems.keyAt(i)) == true) {
                                            isChecked = true;
                                        }
                                    }
                                    if (!isChecked) {
                                        addSelected.setEnabled(false);
                                    } else {
                                        addSelected.setEnabled(true);
                                    }
                                }
                            });

                        } else {
                            long blogID;
                            blogID = WordPress.wpDB.checkMatch(blogNames[0], urls[0], username, password);
                            if (blogID == -1) {
                                blogID = WordPress.wpDB.addAccount(urls[0], homeURLs[0], blogNames[0], username, password, mHttpuser, mHttppassword, "Above Text",
                                    false, false, DEFAULT_IMAGE_SIZE, 5, false, blogIds[0], wpcoms[0], wpVersions[0], isAdmins[0]);
                            }
                            if (blogID >= 0) {
                                WordPress.setCurrentBlog((int) blogID);
                            }
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                }
            });
            Object[] params = { username, password };

            method.call(params);
        }
    }

    interface XMLRPCMethodCallback {
        void callFinished(Object[] result);
    }

    class XMLRPCMethod extends Thread {
        private String method;
        private Object[] params;
        private Handler handler;
        private XMLRPCMethodCallback callBack;

        public XMLRPCMethod(String method, XMLRPCMethodCallback callBack) {
            this.method = method;
            this.callBack = callBack;

            handler = new Handler();

        }

        public void call() {
            call(null);
        }

        public void call(Object[] params) {
            this.params = params;
            start();
        }

        @Override
        public void run() {
            try {
                final Object[] result;
                result = (Object[]) mClient.call(method, params);
                handler.post(new Runnable() {
                    public void run() {
                        callBack.callFinished(result);
                    }
                });
            } catch (final XMLRPCFault e) {
                handler.post(new Runnable() {
                    public void run() {
                        // e.printStackTrace();
                        mProgressDialog.dismiss();
                        String message = e.getMessage();
                        if (message.contains("code 403")) {
                            // invalid login
                            Thread shake = new Thread() {
                                public void run() {
                                    Animation shake = AnimationUtils.loadAnimation(AccountSetupActivity.this, R.anim.shake);
                                    findViewById(R.id.sectionContent).startAnimation(shake);
                                    Toast.makeText(AccountSetupActivity.this, getString(R.string.invalid_login), Toast.LENGTH_SHORT).show();
                                }
                            };
                            runOnUiThread(shake);
                        } else {
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AccountSetupActivity.this);
                            dialogBuilder.setTitle(getString(R.string.connection_error));
                            if (message.contains("404")) {
                                message = getString(R.string.xmlrpc_error);
                            } else if (message.contains("425") && mIsWpcom) {//2steps authentication enabled on this .com account
                                dialogBuilder.setTitle(getString(R.string.info));
                                message = getString(R.string.account_two_step_auth_enabled);
                            }
                            
                            dialogBuilder.setMessage(message);
                            dialogBuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            });
                            dialogBuilder.setCancelable(true);
                            dialogBuilder.create().show();
                        }
                    }
                });

            } catch (final XMLRPCException e) {

                handler.post(new Runnable() {
                    public void run() {
                        Throwable couse = e.getCause();
                        e.printStackTrace();
                        mProgressDialog.dismiss();
                        String message = e.getMessage();
                        if (couse instanceof HttpHostConnectException) {

                        } else {
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AccountSetupActivity.this);
                            dialogBuilder.setTitle(getString(R.string.connection_error));
                            if (message.contains("404"))
                                message = getString(R.string.xmlrpc_error);
                            dialogBuilder.setMessage(message);
                            dialogBuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            });
                            dialogBuilder.setCancelable(true);
                            dialogBuilder.create().show();
                        }
                        e.printStackTrace();

                    }
                });
            }
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }

    private static final Pattern rsdLink = Pattern.compile(
            "<link\\s*?rel=\"EditURI\"\\s*?type=\"application/rsd\\+xml\"\\s*?title=\"RSD\"\\s*?href=\"(.*?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private String getRSDMetaTagHrefRegEx(String urlString) {
        String html = ApiHelper.getResponse(urlString);
        if (html != null) {
            Matcher matcher = rsdLink.matcher(html);
            if (matcher.find()) {
                String href = matcher.group(1);
                return href;
            }
        }
        return null;
    }

    private String getRSDMetaTagHref(String urlString) {
        // get the html code
        InputStream in = ApiHelper.getResponseStream(urlString);

        // parse the html and get the attribute for xmlrpc endpoint
        if (in != null) {
            XmlPullParser parser = Xml.newPullParser();
            try {
                // auto-detect the encoding from the stream
                parser.setInput(in, null);
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String name = null;
                    String rel = "";
                    String type = "";
                    String href = "";
                    switch (eventType) {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("link")) {
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String attrName = parser.getAttributeName(i);
                                String attrValue = parser.getAttributeValue(i);
                                if (attrName.equals("rel")) {
                                    rel = attrValue;
                                } else if (attrName.equals("type"))
                                    type = attrValue;
                                else if (attrName.equals("href"))
                                    href = attrValue;
                            }

                            if (rel.equals("EditURI") && type.equals("application/rsd+xml")) {
                                return href;
                            }
                            // currentMessage.setLink(parser.nextText());
                        }
                        break;
                    }
                    eventType = parser.next();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }
        return null; // never found the rsd tag
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.save) {
            setupBlogs();
        } else if (id == R.id.settingsButton) {
            Intent settings = new Intent(AccountSetupActivity.this, AdditionalSettingsActivity.class);
            settings.putExtra("httpuser", mHttpuser);
            settings.putExtra("httppassword", mHttppassword);
            startActivityForResult(settings, R.id.settingsButton);
        } else if (id == R.id.wordpressdotcom) {
            startActivity(new Intent(AccountSetupActivity.this, SignupActivity.class));
        }
    }

    private void setupBlogs() {
        if (mSystemService.getActiveNetworkInfo() == null) {
            AlertUtil.showAlert(AccountSetupActivity.this, R.string.no_network_title, R.string.no_network_message);
        } else {
            mProgressDialog = ProgressDialog.show(AccountSetupActivity.this, getString(R.string.account_setup), getString(R.string.attempting_configure),
                    true, false);

            if (mIsWpcom && WordPress.hasValidWPComCredentials(AccountSetupActivity.this)) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(AccountSetupActivity.this);
                mUsernameEdit.setText(settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, ""));
                mPasswordEdit.setText(WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, "")));
            }

            Thread action = new Thread() {
                public void run() {
                    Looper.prepare();
                    configureAccount();
                    Looper.loop();
                }
            };
            action.start();
        }
    }
}
