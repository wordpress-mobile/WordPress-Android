
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
import android.util.Patterns;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker.EmailChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WelcomeFragmentSignIn extends NewAccountAbstractPageFragment implements TextWatcher {
    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private EditText mUrlEditText;
    private boolean mSelfHosted;
    private WPTextView mSignInButton;
    private WPTextView mCreateAccountButton;
    private WPTextView mAddSelfHostedButton;
    private WPTextView mProgressTextSignIn;
    private ProgressBar mProgressBarSignIn;
    private List mUsersBlogsList;
    private boolean mHttpAuthRequired;
    private String mHttpUsername = "";
    private String mHttpPassword = "";
    private EmailChecker mEmailChecker;
    private boolean mEmailAutoCorrected;

    public WelcomeFragmentSignIn() {
        mEmailChecker = new EmailChecker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.nux_fragment_welcome, container, false);

        ImageView statsIcon = (ImageView) rootView.findViewById(R.id.nux_fragment_icon);
        statsIcon.setImageResource(R.drawable.nux_icon_wp);

        final RelativeLayout urlButtonLayout = (RelativeLayout) rootView.
                findViewById(R.id.url_button_layout);

        mUsernameEditText = (EditText) rootView.findViewById(R.id.nux_username);
        mUsernameEditText.addTextChangedListener(this);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.nux_password);
        mPasswordEditText.addTextChangedListener(this);
        mUrlEditText = (EditText) rootView.findViewById(R.id.nux_url);
        mSignInButton = (WPTextView) rootView.findViewById(R.id.nux_sign_in_button);
        mSignInButton.setOnClickListener(mSignInClickListener);
        mProgressBarSignIn = (ProgressBar) rootView.findViewById(R.id.nux_sign_in_progress_bar);
        mProgressTextSignIn = (WPTextView) rootView.findViewById(R.id.nux_sign_in_progress_text);

        mCreateAccountButton = (WPTextView) rootView.findViewById(R.id.nux_create_account_button);
        mCreateAccountButton.setOnClickListener(mCreateAccountListener);
        mAddSelfHostedButton = (WPTextView) rootView.findViewById(R.id.nux_add_selfhosted_button);
        mAddSelfHostedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (urlButtonLayout.getVisibility() == View.VISIBLE) {
                    urlButtonLayout.setVisibility(View.GONE);
                    mAddSelfHostedButton.setText(getString(R.string.nux_add_selfhosted_blog));
                    mSelfHosted = false;
                } else {
                    urlButtonLayout.setVisibility(View.VISIBLE);
                    mAddSelfHostedButton.setText(getString(R.string.nux_oops_not_selfhosted_blog));
                    mSelfHosted = true;
                }
            }
        });

        mUsernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    autocorrectUsername();
                }
            }
        });
        return rootView;
    }

    private void autocorrectUsername() {
        if (mEmailAutoCorrected)
            return;
        final String email = mUsernameEditText.getText().toString().trim();
        // Check if the username looks like an email address
        final Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);
        if (!matcher.find()) {
            return ;
        }
        // It looks like an email address, then try to correct it
        String suggest = mEmailChecker.suggestDomainCorrection(email);
        if (suggest.compareTo(email) != 0) {
            mEmailAutoCorrected = true;
            mUsernameEditText.setText(suggest);
            mUsernameEditText.setSelection(suggest.length());
        }
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
        mPasswordEditText.setError(null);
        mUsernameEditText.setError(null);
    }

    private boolean wpcomFieldsFilled() {
        return mUsernameEditText.getText().toString().trim().length() > 0
                && mPasswordEditText.getText().toString().trim().length() > 0;
    }

    private boolean selfHostedFieldsFilled() {
        return wpcomFieldsFilled()
                && mUrlEditText.getText().toString().trim().length() > 0;
    }

    private void showPasswordError(int messageId) {
        mPasswordEditText.setError(getString(messageId));
        mPasswordEditText.requestFocus();
    }

    private void showUsernameError(int messageId) {
        mUsernameEditText.setError(getString(messageId));
        mUsernameEditText.requestFocus();
    }


    protected boolean specificShowError(int messageId) {
        switch (getErrorType(messageId)) {
            case USERNAME:
            case PASSWORD:
                showUsernameError(messageId);
                showPasswordError(messageId);
                return true;
        }
        return false;
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

    protected void startProgress(String message) {
        mProgressBarSignIn.setVisibility(View.VISIBLE);
        mProgressTextSignIn.setVisibility(View.VISIBLE);
        mSignInButton.setVisibility(View.GONE);
        mProgressBarSignIn.setEnabled(false);
        mProgressTextSignIn.setText(message);
    }

    protected void endProgress() {
        mProgressBarSignIn.setVisibility(View.GONE);
        mProgressTextSignIn.setVisibility(View.GONE);
        mSignInButton.setVisibility(View.VISIBLE);
    }

    private class SetupBlogTask extends AsyncTask<Void, Void, List<Object>> {
        private SetupBlog mSetupBlog;
        private int mErrorMsgId;

        @Override
        protected void onPreExecute() {
            mSetupBlog = new SetupBlog();
            mSetupBlog.setUsername(mUsernameEditText.getText().toString().trim());
            mSetupBlog.setPassword(mPasswordEditText.getText().toString().trim());
            if (mSelfHosted) {
                mSetupBlog.setSelfHostedURL(mUrlEditText.getText().toString().trim());
            } else {
                mSetupBlog.setSelfHostedURL(null);
            }
            startProgress(selfHostedFieldsFilled() ? getString(R.string.attempting_configure) :
                    getString(R.string.connecting_wpcom));
        }

        @Override
        protected List doInBackground(Void... args) {
            List userBlogList = mSetupBlog.getBlogList();
            mErrorMsgId = mSetupBlog.getErrorMsgId();
            return userBlogList;
        }

        @Override
        protected void onPostExecute(List<Object> usersBlogsList) {
            if (mHttpAuthRequired) {
                // Prompt for http credentials
                mHttpAuthRequired = false;
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.http_authorization_required);

                View httpAuth = getActivity().getLayoutInflater().inflate(R.layout.alert_http_auth, null);
                final EditText usernameEditText = (EditText) httpAuth.findViewById(R.id.http_username);
                final EditText passwordEditText = (EditText) httpAuth.findViewById(R.id.http_password);
                alert.setView(httpAuth);

                alert.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mSetupBlog.setHttpUsername(usernameEditText.getText().toString());
                        mSetupBlog.setHttpPassword(passwordEditText.getText().toString());
                        new SetupBlogTask().execute();
                    }
                });

                alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
                endProgress();
                return;
            }

            if (usersBlogsList == null && mErrorMsgId != 0) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                NUXDialogFragment nuxAlert;
                if (mErrorMsgId == R.string.account_two_step_auth_enabled) {
                    nuxAlert = NUXDialogFragment.newInstance(getString(R.string.nux_cannot_log_in),
                            getString(mErrorMsgId), getString(R.string.nux_tap_continue),
                            R.drawable.nux_icon_alert, true,
                            getString(R.string.visit_security_settings),
                            NUXDialogFragment.ACTION_OPEN_URL,
                            "https://wordpress.com/settings/security/?ssl=forced");
                } else {
                    if (mErrorMsgId == R.string.update_credentials) {
                        showUsernameError(mErrorMsgId);
                        showPasswordError(mErrorMsgId);
                        endProgress();
                        return ;
                    } else {
                        nuxAlert = NUXDialogFragment.newInstance(getString(R.string.nux_cannot_log_in),
                                getString(mErrorMsgId), getString(R.string.nux_tap_continue),
                                R.drawable.nux_icon_alert);
                    }
                }
                nuxAlert.show(ft, "alert");
                mErrorMsgId = 0;
                endProgress();
                return;
            }

            // Update wp.com credentials
            if (mSetupBlog.getXmlrpcUrl().contains("wordpress.com")) {
                SharedPreferences settings = PreferenceManager.
                        getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mSetupBlog.getUsername());
                editor.putString(WordPress.WPCOM_PASSWORD_PREFERENCE,
                        WordPressDB.encryptPassword(mSetupBlog.getPassword()));
                editor.commit();
                // Fire off a request to get an access token
                WordPress.restClient.get("me", null, null);
            }

            if (usersBlogsList != null) {
                mUsersBlogsList = usersBlogsList;
                showBlogSelectionDialog();
            } else {
                endProgress();
            }
        }

        private void showBlogSelectionDialog() {
            if (mUsersBlogsList == null) {
                mUsersBlogsList = new ArrayList();
            }
            if (mUsersBlogsList.size() == 0) {
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
                return;
            }
            if (mUsersBlogsList.size() == 1) {
                // Just add the one blog and finish up
                SparseBooleanArray oneBlogArray = new SparseBooleanArray();
                oneBlogArray.put(0, true);
                mSetupBlog.addBlogs(mUsersBlogsList, oneBlogArray);
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
                return;
            }
            if (mUsersBlogsList != null && mUsersBlogsList.size() != 0) {
                SparseBooleanArray allBlogs = new SparseBooleanArray();
                for (int i = 0; i < mUsersBlogsList.size(); i++) {
                    allBlogs.put(i, true);
                }
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final ListView listView = (ListView) inflater.inflate(R.layout.select_blogs_list,
                        null);
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                listView.setItemsCanFocus(false);
                final UsersBlogsArrayAdapter adapter = new UsersBlogsArrayAdapter(getActivity(),
                        R.layout.blogs_row,
                        mUsersBlogsList);
                listView.setAdapter(adapter);
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                dialogBuilder.setTitle(R.string.select_blogs);
                dialogBuilder.setView(listView);
                dialogBuilder.setNegativeButton(R.string.add_selected,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                SparseBooleanArray selectedBlogs = listView.
                                        getCheckedItemPositions();
                                mSetupBlog.addBlogs(mUsersBlogsList, selectedBlogs);
                                getActivity().setResult(Activity.RESULT_OK);
                                getActivity().finish();
                            }
                        });
                dialogBuilder.setPositiveButton(R.string.add_all,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                SparseBooleanArray allBlogs = new SparseBooleanArray();
                                for (int i = 0; i < adapter.getCount(); i++) {
                                    allBlogs.put(i, true);
                                }
                                if (allBlogs.size() > 0) {
                                    mSetupBlog.addBlogs(mUsersBlogsList, allBlogs);
                                }
                                getActivity().setResult(Activity.RESULT_OK);
                                getActivity().finish();
                            }
                        });
                dialogBuilder.setOnKeyListener(new ProgressDialog.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        endProgress();
                        return false;
                    }
                });
                dialogBuilder.setCancelable(true);
                AlertDialog ad = dialogBuilder.create();
                ad.setInverseBackgroundForced(true);
                ad.show();


                final Button addSelected = ad.getButton(AlertDialog.BUTTON_NEGATIVE);
                addSelected.setEnabled(false);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        SparseBooleanArray selectedItems = listView.getCheckedItemPositions();
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
    }

    private class UsersBlogsArrayAdapter extends ArrayAdapter {
        public UsersBlogsArrayAdapter(Context context, int resource, List<Object> list) {
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

                CheckedTextView blogTitleView = (CheckedTextView)
                        convertView.findViewById(R.id.blog_title);
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
