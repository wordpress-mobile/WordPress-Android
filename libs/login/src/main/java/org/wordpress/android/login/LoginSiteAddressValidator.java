package org.wordpress.android.login;

import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

/**
 * Encapsulates the site address validation, cleaning, and error handling of {@link LoginSiteAddressFragment}.
 */
class LoginSiteAddressValidator {
    private MutableLiveData<Boolean> mIsValid = new MutableLiveData<>();

    @NonNull private String mCleanedSiteAddress = "";

    LiveData<Boolean> getIsValid() {
        return mIsValid;
    }

    @NotNull String getCleanedSiteAddress() {
        return mCleanedSiteAddress;
    }

    LoginSiteAddressValidator() {
        mIsValid.postValue(false);
    }

    public void setAddress(@NonNull String siteAddress) {
        mCleanedSiteAddress = cleanSiteAddress(siteAddress);
        mIsValid.postValue(siteAddressIsValid(mCleanedSiteAddress));
    }

    private static String cleanSiteAddress(@NonNull String siteAddress) {
        return siteAddress.trim().replaceAll("[\r\n]", "");
    }

    private static boolean siteAddressIsValid(@NonNull String cleanedSiteAddress) {
        return Patterns.WEB_URL.matcher(cleanedSiteAddress).matches();
    }
}
