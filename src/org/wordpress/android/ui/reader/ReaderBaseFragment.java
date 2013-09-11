package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockFragment;

public abstract class ReaderBaseFragment extends SherlockFragment {

    protected final String interfaceNameForJS = "Android";

    protected String httpuser = "";
    protected String httppassword = "";

    public String topicTitle;

    private UpdateTopicTitleListener updateTopicTitleListener;
    private UpdateTopicIDListener updateTopicIDListener;
    private ChangeTopicListener onChangeTopicListener;
    private GetLoadedItemsListener getLoadedItemsListener;
    private UpdateButtonStatusListener updateButtonStatusListener;
    private GetPermalinkListener getPermalinkListener;
    private GetLastSelectedItemListener getLastSelectedItemListener;

    protected void setDefaultWebViewSettings(WebView wv) {
        WebSettings webSettings = wv.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUserAgentString("wp-android-native");
        webSettings.setSavePassword(false);
        webSettings.setRenderPriority(RenderPriority.HIGH);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            updateTopicTitleListener = (UpdateTopicTitleListener) activity;
            updateTopicIDListener = (UpdateTopicIDListener) activity;
            onChangeTopicListener = (ChangeTopicListener) activity;
            getLoadedItemsListener = (GetLoadedItemsListener) activity;
            updateButtonStatusListener = (UpdateButtonStatusListener) activity;
            getPermalinkListener = (GetPermalinkListener) activity;
            getLastSelectedItemListener = (GetLastSelectedItemListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    protected class WordPressWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                HttpAuthHandler handler, String host, String realm) {
            handler.proceed(httpuser, httppassword);
        }
    }


    protected class JavaScriptInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void setTopicTitle(String topicTitle) {
            updateTopicTitleListener.updateTopicTitle(topicTitle);
        }

        @JavascriptInterface
        public void setSelectedTopic(String topicID) {
            updateTopicIDListener.onUpdateTopicID(topicID);
        }

        @JavascriptInterface
        public void selectTopic(String topicID, String topicName) {
             onChangeTopicListener.onChangeTopic(topicID, topicName);
        }

        @JavascriptInterface
        public void getLoadedItems(String items) {
            getLoadedItemsListener.getLoadedItems(items);
        }

        @JavascriptInterface
        public void getArticlePermalink(String permalink) {
            getPermalinkListener.getPermalink(permalink);
        }

        @JavascriptInterface
        public void getLastSelectedItem(String item) {
            getLastSelectedItemListener.getLastSelectedItem(item);
        }

        @JavascriptInterface
        public void hasPrev(boolean isPrev) {
            updateButtonStatusListener.updateButtonStatus(0, isPrev);
        }

        @JavascriptInterface
        public void hasNext(String isNext) {
            updateButtonStatusListener.updateButtonStatus(1, Boolean.parseBoolean(isNext));
        }
    }

    public interface UpdateTopicTitleListener {
        public void updateTopicTitle(String topicTitle);
    }

    public interface UpdateTopicIDListener {
        public void onUpdateTopicID(String topicID);
    }

    public interface ChangeTopicListener {
        public void onChangeTopic(String topicID, String topicName);
    }

    public interface GetLoadedItemsListener {
        public void getLoadedItems(String items);
    }

    public interface UpdateButtonStatusListener {
        public void updateButtonStatus(int button, boolean enabled);
    }

    public interface GetPermalinkListener {
        public void getPermalink(String permalink);
    }

    public interface GetLastSelectedItemListener {
        public void getLastSelectedItem(String lastSelectedItem);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

}
