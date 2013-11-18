
package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.Utils;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.wordpress.android.widgets.WPTextView;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WelcomeFragmentSignIn extends NewAccountAbstractPageFragment implements TextWatcher {

    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private EditText mUrlEditText;
    private WPTextView mSignInButton;
    private WPTextView mCreateAccountButton;
    private List mUsersBlogsList;
    private static final String DEFAULT_IMAGE_SIZE = "2000";
    private boolean mHttpAuthRequired;
    private String mHttpUsername = "";
    private String mHttpPassword = "";

    public WelcomeFragmentSignIn() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.nux_fragment_welcome_sign_in, container, false);

        mUsernameEditText = (EditText) rootView.findViewById(R.id.nux_username);
        mUsernameEditText.addTextChangedListener(this);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.nux_password);
        mPasswordEditText.addTextChangedListener(this);
        mUrlEditText = (EditText) rootView.findViewById(R.id.nux_url);
        mSignInButton = (WPTextView) rootView.findViewById(R.id.nux_sign_in);
        mSignInButton.setOnClickListener(mSignInClickListener);
        mCreateAccountButton = (WPTextView) rootView.findViewById(R.id.nux_create_account_button);
        mCreateAccountButton.setOnClickListener(mCreateAccountListener);

        return rootView;
    }

    private View.OnClickListener mCreateAccountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent newAccountIntent = new Intent(getActivity(), NewAccountActivity.class);
            startActivityForResult(newAccountIntent, WelcomeActivity.CREATE_ACCOUNT_REQUEST);
        }
    };
    
    private OnClickListener mSignInClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!wpcomFieldsFilled()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment
                        .newInstance(getString(R.string.required_fields));
                alert.show(ft, "alert");
                return;
            }
            new SetupBlogTask().execute();
        }
    };

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (wpcomFieldsFilled()) {
            mSignInButton.setEnabled(true);
        } else {
            mSignInButton.setEnabled(false);
        }
    }

    private boolean wpcomFieldsFilled() {
        return mUsernameEditText.getText().toString().trim().length() > 0
                && mPasswordEditText.getText().toString().trim().length() > 0;
    }
    
    private boolean selfHostedFieldsFilled() {
        return wpcomFieldsFilled()
                && mUrlEditText.getText().toString().trim().length() > 0;
    }

    public void signInDotComUser() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
        String password = WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null));
        if (username != null && password != null) {
            mUsernameEditText.setText(username);
            mPasswordEditText.setText(password);
            new SetupBlogTask().execute();
        }
    }

    private class SetupBlogTask extends AsyncTask<Void, Void, List<Object>> {
        private ProgressDialog mProgressDialog;
        private String mErrorMsg;
        private String mUsername;
        private String mPassword;
        private String mXmlrpcUrl;
        private boolean mIsCustomUrl;
        
        @Override
        protected void onPreExecute() {
            mProgressDialog = ProgressDialog.show(getActivity(), "", (selfHostedFieldsFilled()) ? getString(R.string.attempting_configure) : getString(R.string.connecting_wpcom),
                    true, false);
        }

        @Override
        protected List doInBackground(Void... args) {
            if (selfHostedFieldsFilled()) {
                mXmlrpcUrl = getSelfHostedXmlrpcUrl();
            } else {
                mXmlrpcUrl = Constants.wpcomXMLRPCURL;
            }
            
            if (mXmlrpcUrl == null) {
                if (!mHttpAuthRequired)
                    mErrorMsg = getString(R.string.no_site_error);
                return null;
            }
            
            // Validate the URL found before calling the client. Prevent a crash that can occur during the setup of self-hosted sites.
            try {
                URI.create(mXmlrpcUrl);
            } catch (Exception e1) {
                mErrorMsg = getString(R.string.no_site_error);
                return null;
            }
            
            mUsername = mUsernameEditText.getText().toString().trim();
            mPassword = mPasswordEditText.getText().toString().trim();
            
            XMLRPCClient client = new XMLRPCClient(mXmlrpcUrl, mHttpUsername, mHttpPassword);
            Object[] params = { mUsername, mPassword };
            try {
                Object[] userBlogs = (Object[]) client.call("wp.getUsersBlogs", params);
                Arrays.sort(userBlogs, Utils.BlogNameComparator);
                List<Object> userBlogsList = Arrays.asList(userBlogs);
                return userBlogsList;
            } catch (XMLRPCException e) {
                String message = e.getMessage();
                if (message.contains("code 403"))
                    mErrorMsg = getString(R.string.update_credentials);
                else if (message.contains("404"))
                    mErrorMsg = getString(R.string.xmlrpc_error);
                else if (message.contains("425"))
                    mErrorMsg = getString(R.string.account_two_step_auth_enabled);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Object> usersBlogsList) {
            mProgressDialog.dismiss();

            if (mHttpAuthRequired) {
                // Prompt for http credentials
                mHttpAuthRequired = false;
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.http_authorization_required);

                View httpAuth = getActivity().getLayoutInflater().inflate(R.layout.alert_http_auth, null);
                final EditText usernameEditText = (EditText)httpAuth.findViewById(R.id.http_username);
                final EditText passwordEditText = (EditText)httpAuth.findViewById(R.id.http_password);
                alert.setView(httpAuth);

                alert.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mHttpUsername = usernameEditText.getText().toString();
                        mHttpPassword = passwordEditText.getText().toString();
                        new SetupBlogTask().execute();
                    }
                });

                alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
                return;
            }

            if (usersBlogsList == null && mErrorMsg != null) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                NUXDialogFragment nuxAlert = NUXDialogFragment
                        .newInstance(getString(R.string.nux_cannot_log_in), mErrorMsg,                                getString(R.string.nux_tap_continue), R.drawable.nux_icon_alert);
                nuxAlert.show(ft, "alert");
                mErrorMsg = null;
                return;
            }
            
            // Update wp.com credentials
            if (mXmlrpcUrl.contains("wordpress.com")) {
              SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
              SharedPreferences.Editor editor = settings.edit();
              editor.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mUsername);
              editor.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, WordPressDB.encryptPassword(mPassword));
              editor.commit();
              // Fire off a request to get an access token
              WordPress.restClient.get("me", null, null);
            }
            
            if (usersBlogsList != null) {
                mUsersBlogsList = usersBlogsList;

                if (mUsersBlogsList.size() == 1) {
                    // Just add the one blog and finish up
                    SparseBooleanArray oneBlogArray = new SparseBooleanArray();
                    oneBlogArray.put(0, true);
                    addBlogs(oneBlogArray);
                    return;
                }

                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final ListView lv = (ListView) inflater.inflate(R.layout.select_blogs_list, null);
                lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                lv.setItemsCanFocus(false);

                final UsersBlogsArrayAdapter adapter = new UsersBlogsArrayAdapter(getActivity(), R.layout.blogs_row,
                        mUsersBlogsList);

                lv.setAdapter(adapter);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                dialogBuilder.setTitle(R.string.select_blogs);
                dialogBuilder.setView(lv);
                dialogBuilder.setNegativeButton(R.string.add_selected, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        SparseBooleanArray selectedBlogs = lv.getCheckedItemPositions();
                        addBlogs(selectedBlogs);
                    }
                });
                dialogBuilder.setPositiveButton(R.string.add_all, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        SparseBooleanArray allBlogs = new SparseBooleanArray();
                        for (int i = 0; i < adapter.getCount(); i++) {
                            allBlogs.put(i, true);
                        }
                        if (allBlogs.size() > 0) 
                            addBlogs(allBlogs);

                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
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
            }
        }
        
        // Attempts to retrieve the xmlrpc url for a self-hosted site, in this order:
        // 1: Try to retrieve it by finding the ?rsd url in the site's header
        // 2: Take whatever URL the user entered to see if that returns a correct response
        // 3: Finally, just guess as to what the xmlrpc url should be
        private String getSelfHostedXmlrpcUrl() {
            String xmlrpcUrl = null;
            String url = mUrlEditText.getText().toString().trim();
            // Add http to the beginning of the URL if needed
            if (!(url.toLowerCase().startsWith("http://")) && !(url.toLowerCase().startsWith("https://"))) {
                url = "http://" + url; // default to http
            }

            if (!URLUtil.isValidUrl(url)) {
                mErrorMsg = getString(R.string.invalid_url_message);
                return null;
            }
            
            // Attempt to get the XMLRPC URL via RSD
            String rsdUrl = ApiHelper.getRSDMetaTagHrefRegEx(url);
            if (rsdUrl == null) {
                rsdUrl = ApiHelper.getRSDMetaTagHref(url);
            }
            
            if (rsdUrl != null) {
                xmlrpcUrl = ApiHelper.getXMLRPCUrl(rsdUrl);
                if (xmlrpcUrl == null)
                    xmlrpcUrl = rsdUrl.replace("?rsd", "");
            } else {
                // Try the user entered path
                try {
                    XMLRPCClient client = new XMLRPCClient(url, mHttpUsername, mHttpPassword);
                    try {
                        client.call("system.listMethods");
                        xmlrpcUrl = url;
                        mIsCustomUrl = true;
                    } catch (XMLRPCException e) {

                        if (e.getMessage().contains("401")) {
                            mHttpAuthRequired = true;
                            return null;
                        }

                        // Guess the xmlrpc path
                        String guessURL = url;
                        if (guessURL.substring(guessURL.length() - 1, guessURL.length()).equals("/")) {
                            guessURL = guessURL.substring(0, guessURL.length() - 1);
                        }
                        guessURL += "/xmlrpc.php";
                        client = new XMLRPCClient(guessURL, mHttpUsername, mHttpPassword);
                        try {
                            client.call("system.listMethods");
                            xmlrpcUrl = guessURL;
                        } catch (XMLRPCException ex) {
                        }
                    }
                } catch (Exception e) {
                }
            }
            return xmlrpcUrl;
        }
        
        // Add selected blog(s) to the database
        private void addBlogs(SparseBooleanArray selectedBlogs) {
            for (int i = 0; i < selectedBlogs.size(); i++) {
                if (selectedBlogs.get(selectedBlogs.keyAt(i)) == true) {
                    int rowID = selectedBlogs.keyAt(i);
                    
                    Map blogMap = (HashMap)mUsersBlogsList.get(rowID);
                    
                    String blogName = StringUtils.unescapeHTML(blogMap.get("blogName").toString());
                    String xmlrpcUrl = (mIsCustomUrl) ? mXmlrpcUrl : blogMap.get("xmlrpc").toString();

                    if (!WordPress.wpDB.checkForExistingBlog(blogName, xmlrpcUrl, mUsername, mPassword)) {
                        // The blog isn't in the app, so let's create it
                        Blog blog = new Blog(xmlrpcUrl, mUsername, mPassword);
                        blog.setHomeURL(blogMap.get("url").toString());
                        blog.setHttpuser(mHttpUsername);
                        blog.setHttppassword(mHttpPassword);
                        blog.setBlogName(blogName);
                        blog.setImagePlacement(""); //deprecated
                        blog.setFullSizeImage(false);
                        blog.setMaxImageWidth(DEFAULT_IMAGE_SIZE);
                        blog.setMaxImageWidthId(5);
                        blog.setRunService(false); //deprecated
                        blog.setBlogId(Integer.parseInt(blogMap.get("blogid").toString()));
                        blog.setDotcomFlag(xmlrpcUrl.contains("wordpress.com"));
                        blog.setWpVersion(""); // assigned later in getOptions call
                        if (blog.save(null) && i == 0)
                            WordPress.setCurrentBlog(blog.getId());
                    }
                }
            }

            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }
    }
    
    private class UsersBlogsArrayAdapter extends ArrayAdapter {

        public UsersBlogsArrayAdapter(Context context, int resource,
                List<Object> list) {
            super(context, resource, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                convertView = inflater.inflate(R.layout.blogs_row, parent, false);
            }
            
            Map<String, Object> blogMap = (HashMap<String, Object>) mUsersBlogsList.get(position);
            if (blogMap != null) {
                
                CheckedTextView blogTitleView = (CheckedTextView)convertView.findViewById(R.id.blog_title);
                String blogTitle = blogMap.get("blogName").toString();
                if (blogTitle != null && blogTitle.trim().length() > 0) {
                    blogTitleView.setText(StringUtils.unescapeHTML(blogTitle));
                } else {
                    blogTitleView.setText(blogMap.get("url").toString());
                }
            }
            
            return convertView;
        }
    }

}
