package org.wordpress.android.ui.accounts;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.util.WPRestClient;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 *
 */
public abstract class NewAccountAbstractPageFragment extends SherlockFragment {
    /**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";
    
    protected ConnectivityManager mSystemService;
    
    protected ProgressDialog pd;

    protected static RequestQueue requestQueue = null; 

    protected static WPRestClient restClient = null;
    

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    protected int mPageNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt(ARG_PAGE);
        mSystemService = (ConnectivityManager) getActivity().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if ( requestQueue == null )
            requestQueue = Volley.newRequestQueue(getActivity());
        if( restClient == null )
            restClient = new WPRestClient(requestQueue, null);
    }

    /**
     * Returns the page number represented by this fragment object.
     */
    public int getPageNumber() {
        return mPageNumber;
    }
    
    protected class ErrorListener implements RestRequest.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d("ErrorListener", String.format("Error type: %s", error));
            String message = null;

            if (error.networkResponse != null && error.networkResponse.data != null) {
                Log.d("ErrorListener", String.format("Error message: %s", new String(error.networkResponse.data)));
                String jsonString = new String(error.networkResponse.data);
                try {
                    JSONObject errorObj = new JSONObject(jsonString);
                    message = getErrorMessageForErrorCode((String) errorObj.get("error"));
                    if (message == null) { // Not one of our common errors. Show the error message from the server.
                        message = (String) errorObj.get("message");
                    }
                } catch (JSONException e) {
                    Log.d("ErrorListener", String.format("Error parsing the error message :( : %s", e));
                    message = getString(R.string.error_generic);
                }
            } else {
                if (error.getMessage() != null) {
                    if (error.getMessage().contains("Limit reached"))
                        message = getString(R.string.limit_reached);
                    else
                        message = error.getMessage();
                } else {
                    message = getString(R.string.error_generic);
                }
            }
            showError(message);
        }
    }

    protected void showError(String message) {
        if(pd != null ) 
            pd.dismiss();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        NUXDialogFragment nuxAlert = NUXDialogFragment.newInstance(getString(R.string.error),
                message, getString(R.string.nux_tap_continue), R.drawable.nux_icon_alert);
        nuxAlert.show(ft, "alert");
    }
    
    protected String getErrorMessageForErrorCode(String errorCode) {
        if(errorCode.equals("username_only_lowercase_letters_and_numbers"))
            return getString(R.string.username_only_lowercase_letters_and_numbers);
        if(errorCode.equals("username_required"))
            return getString(R.string.username_required);
        if(errorCode.equals("username_not_allowed"))
            return getString(R.string.username_not_allowed);
        if(errorCode.equals("email_cant_be_used_to_signup"))
            return getString(R.string.email_cant_be_used_to_signup);
        if(errorCode.equals("username_must_be_at_least_four_characters"))
            return getString(R.string.username_must_be_at_least_four_characters);
        if(errorCode.equals("username_contains_invalid_characters"))
            return getString(R.string.username_contains_invalid_characters);
        if(errorCode.equals("username_must_include_letters"))
            return getString(R.string.username_must_include_letters);
        if(errorCode.equals("email_invalid"))
            return getString(R.string.email_invalid);
        if(errorCode.equals("email_not_allowed"))
            return getString(R.string.email_not_allowed);
        if(errorCode.equals("username_exists"))
            return getString(R.string.username_exists);
        if(errorCode.equals("email_exists"))
            return getString(R.string.email_exists);
        if(errorCode.equals("username_reserved_but_may_be_available"))
            return getString(R.string.username_reserved_but_may_be_available);
        if(errorCode.equals("email_reserved"))
            return getString(R.string.email_reserved);
        if(errorCode.equals("blog_name_required"))    
            return getString(R.string.blog_name_required);
        if(errorCode.equals("blog_name_not_allowed"))
            return getString(R.string.blog_name_not_allowed);
        if(errorCode.equals("blog_name_must_be_at_least_four_characters"))
            return getString(R.string.blog_name_must_be_at_least_four_characters);
        if(errorCode.equals("blog_name_must_be_less_than_sixty_four_characters"))
            return getString(R.string.blog_name_must_be_less_than_sixty_four_characters);
        if(errorCode.equals("blog_name_contains_invalid_characters"))
            return getString(R.string.blog_name_contains_invalid_characters);
        if(errorCode.equals("blog_name_cant_be_used"))
            return getString(R.string.blog_name_cant_be_used);
        if(errorCode.equals("blog_name_only_lowercase_letters_and_numbers"))
            return getString(R.string.blog_name_only_lowercase_letters_and_numbers);
        if(errorCode.equals("blog_name_must_include_letters"))
            return getString(R.string.blog_name_must_include_letters);
        if(errorCode.equals("blog_name_exists"))
            return getString(R.string.blog_name_exists);
        if(errorCode.equals("blog_name_reserved"))
            return getString(R.string.blog_name_reserved);
        if(errorCode.equals("blog_name_reserved_but_may_be_available"))
            return getString(R.string.blog_name_reserved_but_may_be_available);
        if(errorCode.equals("password_invalid"))
            return getString(R.string.password_invalid);
        if(errorCode.equals("blog_name_invalid"))
            return getString(R.string.blog_name_invalid);
        if(errorCode.equals("blog_title_invalid"))
            return getString(R.string.blog_title_invalid);
        if(errorCode.equals("username_invalid"))
            return getString(R.string.username_invalid);
        
        return null;
    }
}