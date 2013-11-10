
package org.wordpress.android.ui.accounts;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.Config;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.widgets.WPTextView;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewBlogPageFragment extends NewAccountAbstractPageFragment implements TextWatcher {

    private EditText siteURL;
    private EditText siteTitle;
    private Spinner langSpinner;
    private Spinner privacySpinner;
    private Hashtable<String,String> wordpressComSupportedLanguages = null;
    private String matchedDeviceLanguage = null;
    private WPTextView mNextButton;

    private static int WordPressComApiBlogVisibilityPublic = 0;
    private static int WordPressComApiComBlogVisibilityPrivate = 1;
    private static int WordPressComApiBlogVisibilityHidden = 2;
    
    public NewBlogPageFragment() {

    }

    private boolean checkSiteData() {
        final String siteAddress = siteURL.getText().toString().trim();
        //final String siteTitleString = siteTitle.getText().toString().trim();

        if (siteAddress.equals("")) {
            AlertUtil.showAlert(NewBlogPageFragment.this.getActivity(), R.string.required_fields,
                    R.string.blog_address_required);
            return false;
        }
        return true;
    }
    
    OnClickListener nextClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            
            NewAccountActivity act = (NewAccountActivity)getActivity();
            act.validatedBlogURL = null;
            act.validatedBlogTitle = null;
            act.validatedLanguageID = null;
            act.validatedPrivacyOption = null;
            
            if (mSystemService.getActiveNetworkInfo() == null) {
                AlertUtil.showAlert(getActivity(), R.string.no_network_title, R.string.no_network_message);
                return;
            }

            if (false == checkSiteData())
                return;
            
            final String siteAddress = siteURL.getText().toString().trim();
            final String siteTitleString = siteTitle.getText().toString().trim();
            
            pd = ProgressDialog.show(NewBlogPageFragment.this
                    .getActivity(),
                    getString(R.string.account_setup),
                    getString(R.string.validating_site_data), true, false);

            String path = "sites/new";
            Map<String, String> params = new HashMap<String, String>();
            params.put("blog_name", siteAddress);
            params.put("blog_title", siteTitleString);
            params.put("lang_id", wordpressComSupportedLanguages.get(langSpinner.getSelectedItem()));
            
            final String visibility;
            int selectedPrivacyIndex = privacySpinner.getSelectedItemPosition();
            if( selectedPrivacyIndex == 0 || selectedPrivacyIndex == Spinner.INVALID_POSITION) {
                visibility = String.valueOf(WordPressComApiBlogVisibilityPublic);
            } else if( selectedPrivacyIndex == 1 ) {
                visibility = String.valueOf(WordPressComApiBlogVisibilityHidden);
            } else {
                visibility = String.valueOf(WordPressComApiComBlogVisibilityPrivate);
            }
            
            params.put("public", visibility);
            params.put("validate", "1");
            params.put("client_id", Config.OAUTH_APP_ID);
            params.put("client_secret", Config.OAUTH_APP_SECRET);

            restClient.post(path, params, null,
                    new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (pd != null)
                                pd.dismiss();
                            try {
                                if(response.getBoolean("success")) {
                                    NewAccountActivity act = (NewAccountActivity)getActivity();
                                    act.validatedBlogURL = siteAddress;
                                    act.validatedBlogTitle = siteTitleString;
                                    act.validatedLanguageID = langSpinner.getSelectedItem().toString();
                                    act.validatedPrivacyOption = visibility;
                                    act.showNextItem();
                                } else {
                                    showError(getString(R.string.error_generic));
                                }
                            } catch (JSONException e) {
                                showError(getString(R.string.error_generic));
                            }
                        }
                    },
                    new ErrorListener()
                    );
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.new_account_blog_fragment_screen, container, false);

        mNextButton = (WPTextView) rootView.findViewById(R.id.next_button);
        mNextButton.setOnClickListener(nextClickListener);

        siteURL = (EditText) rootView.findViewById(R.id.site_url);
        siteURL.addTextChangedListener(this);
        siteTitle = (EditText) rootView.findViewById(R.id.site_title);
        siteTitle.addTextChangedListener(this);

        langSpinner = (Spinner) rootView.findViewById(R.id.langs_spinner);
        loadWordPressComLanguages();
        List<String> list = new ArrayList<String>(wordpressComSupportedLanguages.keySet());
        
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.spinner_textview, list);
        dataAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        langSpinner.setAdapter(dataAdapter);
        
        for (int i = 0; i < list.size(); i++) {
            if( list.get(i).equalsIgnoreCase(matchedDeviceLanguage)) {
                langSpinner.setSelection(i);
                break;
            }
        }    
        
        privacySpinner = (Spinner) rootView.findViewById(R.id.privacy_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.privacy_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        privacySpinner.setAdapter(adapter);

        WPTextView termsOfServiceTextView = (WPTextView)rootView.findViewById(R.id.l_agree_terms_of_service);
        termsOfServiceTextView.setText(Html.fromHtml(String.format(getString(R.string.agree_terms_of_service, "<u>", "</u>"))));
        termsOfServiceTextView.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri uri = Uri.parse(Constants.URL_TOS);
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
                }
                );
        
        return rootView;
    }
    
    private void loadWordPressComLanguages() {
        Resources res = getActivity().getResources();
        XmlResourceParser parser = res.getXml(R.xml.wpcom_languages);
        Hashtable<String, String> entries = new Hashtable<String, String>();
        try {
            int eventType = parser.getEventType();
            String deviceLanguageCode = Locale.getDefault().getLanguage();

            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                if(eventType == XmlPullParser.START_TAG)
                {
                    String name = parser.getName();
                    if(name.equals("language")) {
                        String currentID = null;
                        boolean currentLangIsDeviceLanguage = false;
                        int i = 0;
                        while (i<parser.getAttributeCount()){
                            if(parser.getAttributeName(i).equals("id"))
                                currentID = parser.getAttributeValue(i);
                            if(parser.getAttributeName(i).equals("code") && parser.getAttributeValue(i).equalsIgnoreCase(deviceLanguageCode) )
                                currentLangIsDeviceLanguage = true;
                            i++;
                        }

                        while (eventType != XmlPullParser.END_TAG)
                        {  
                            if(eventType == XmlPullParser.TEXT)
                            {
                                entries.put(parser.getText() , currentID );
                                if(currentLangIsDeviceLanguage)
                                    matchedDeviceLanguage = parser.getText();
                            }
                            eventType = parser.next();
                        }  
                    }
                }      
                eventType = parser.next();
            }
        } catch (Exception e) {
            entries = new Hashtable<String, String>();
            entries.put("en - English", "1");
        } 
        wordpressComSupportedLanguages = entries;
        
        if(matchedDeviceLanguage == null)
            matchedDeviceLanguage = "en - English";
    }

    @Override
    public void afterTextChanged(Editable s) {
        
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (siteURL.getText().toString().trim().length() > 0 && siteTitle.getText().toString().length() > 0)
            mNextButton.setEnabled(true);
        else
            mNextButton.setEnabled(false);
    }
}