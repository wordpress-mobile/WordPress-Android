package org.wordpress.android.login;

import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.wordpress.android.util.helpers.Debouncer;

import java.util.concurrent.TimeUnit;

/**
 * Encapsulates the site address validation, cleaning, and error reporting of {@link LoginSiteAddressFragment}.
 */
class LoginSiteAddressValidator {
    private static final int SECONDS_DELAY_BEFORE_SHOWING_ERROR_MESSAGE = 2;

    private MutableLiveData<Boolean> mIsValid = new MutableLiveData<>();
    private MutableLiveData<Integer> mErrorMessageResId = new MutableLiveData<>();

    private String mCleanedSiteAddress = "";
    private Debouncer mDebouncer = new Debouncer();

    @NonNull LiveData<Boolean> getIsValid() {
        return mIsValid;
    }

    @NonNull LiveData<Integer> getErrorMessageResId() {
        return mErrorMessageResId;
    }

    @NonNull String getCleanedSiteAddress() {
        return mCleanedSiteAddress;
    }

    LoginSiteAddressValidator() {
        mIsValid.setValue(false);
    }

    void dispose() {
        mDebouncer.shutdown();
    }

    void setAddress(@NonNull String siteAddress) {
        mCleanedSiteAddress = cleanSiteAddress(siteAddress);
        final boolean isValid = siteAddressIsValid(mCleanedSiteAddress);

        mIsValid.setValue(isValid);
        mErrorMessageResId.setValue(null);

        // Call debounce regardless if there was an error so that the previous Runnable will be cancelled.
        mDebouncer.debounce(Void.class, new Runnable() {
            @Override public void run() {
                if (!isValid && !mCleanedSiteAddress.isEmpty()) {
                    mErrorMessageResId.postValue(R.string.login_invalid_site_url);
                }
            }
        }, SECONDS_DELAY_BEFORE_SHOWING_ERROR_MESSAGE, TimeUnit.SECONDS);
    }

    private static String cleanSiteAddress(@NonNull String siteAddress) {
        return siteAddress.trim().replaceAll("[\r\n]", "");
    }

    private static boolean siteAddressIsValid(@NonNull String cleanedSiteAddress) {
        return Patterns.WEB_URL.matcher(cleanedSiteAddress).matches();
    }
}
