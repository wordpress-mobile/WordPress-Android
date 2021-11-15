package org.wordpress.android.ui;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.wordpress.android.util.helpers.WPWebChromeClient;

import static android.app.Activity.RESULT_OK;
import static org.wordpress.android.ui.RequestCodes.WEB_CHROME_CLIENT_FILE_PICKER;

public class WPWebChromeClientWithFileChooser extends WPWebChromeClient {
    private ValueCallback<Uri[]> mFilePathCallback;
    private final OnShowFileChooserListener mOnShowFileChooserListener;

    public WPWebChromeClientWithFileChooser(Activity activity, View view, int defaultPoster,
                                            ProgressBar progressBar,
                                            OnShowFileChooserListener onShowFileChooserListener) {
        super(activity, view, defaultPoster, progressBar);
        this.mOnShowFileChooserListener = onShowFileChooserListener;
    }

    @Override public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                               FileChooserParams fileChooserParams) {
        this.mFilePathCallback = filePathCallback;

        // Check if MODE_OPEN_MULTIPLE is specified
        Boolean canMultiselect = false;
        if (fileChooserParams.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            canMultiselect = true;
        }

        Intent intent = fileChooserParams.createIntent();
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, canMultiselect);

        String[] acceptableMimeTypes = fileChooserParams.getAcceptTypes();

        // Uses Intent.EXTRA_MIME_TYPES to pass multiple mime types if there are multiple MIME types.
        if (acceptableMimeTypes.length > 1) {
            // Sets an explicit MIME data type that states that all MIME types are supported.
            intent.setType("*/*");
            // Filter MIME types by Intent.EXTRA_MIME_TYPES with the types that are currently acceptable.
            intent.putExtra(Intent.EXTRA_MIME_TYPES, acceptableMimeTypes);
        }

        if (mOnShowFileChooserListener != null) {
            mOnShowFileChooserListener.startActivityForFileChooserResult(intent, WEB_CHROME_CLIENT_FILE_PICKER);
        }

        return true;
    }

    void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Uri[] selectedUris = null;
        if (resultCode == RESULT_OK && intent != null) {
            // if ClipData is not empty that means there are multiple files.
            if (intent.getClipData() != null) {
                // process multiple files
                int clipDataItemCount = intent.getClipData().getItemCount();
                selectedUris = new Uri[clipDataItemCount];
                for (int i = 0; i < clipDataItemCount; i++) {
                    selectedUris[i] = intent.getClipData().getItemAt(i).getUri();
                }
            } else if (intent.getData() != null) {
                // process the single file single-selected file
                selectedUris = WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
            }
        }
        mFilePathCallback.onReceiveValue(selectedUris);
    }

    interface OnShowFileChooserListener {
        void startActivityForFileChooserResult(Intent intent, int requestCode);
    }
}
