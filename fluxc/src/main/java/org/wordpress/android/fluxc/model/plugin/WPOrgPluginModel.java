package org.wordpress.android.fluxc.model.plugin;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import java.io.Serializable;

@Table
@RawConstraints({"UNIQUE (SLUG)"})
public class WPOrgPluginModel implements Identifiable, Serializable {
    private static final long serialVersionUID = 207979865991034152L;

    @PrimaryKey @Column private int mId;
    @Column private String mAuthorAsHtml;
    @Column private String mAuthorName;
    @Column private String mBanner;
    @Column private String mDescriptionAsHtml;
    @Column private String mDisplayName;
    @Column private String mFaqAsHtml;
    @Column private String mHomepageUrl;
    @Column private String mIcon;
    @Column private String mInstallationInstructionsAsHtml;
    @Column private String mLastUpdated;
    @Column private String mRating;
    @Column private String mRequiredWordPressVersion;
    @Column private String mSlug;
    @Column private String mVersion;
    @Column private String mWhatsNewAsHtml;
    @Column private int mDownloadCount;
    @Column private int mNumberOfRatings;
    @Column private int mNumberOfRatingsOfOne;
    @Column private int mNumberOfRatingsOfTwo;
    @Column private int mNumberOfRatingsOfThree;
    @Column private int mNumberOfRatingsOfFour;
    @Column private int mNumberOfRatingsOfFive;

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public int getId() {
        return mId;
    }

    public String getAuthorAsHtml() {
        return mAuthorAsHtml;
    }

    public void setAuthorAsHtml(String authorAsHtml) {
        mAuthorAsHtml = authorAsHtml;
    }

    public String getAuthorName() {
        return mAuthorName;
    }

    public void setAuthorName(String authorName) {
        mAuthorName = authorName;
    }

    public String getBanner() {
        return mBanner;
    }

    public void setBanner(String banner) {
        mBanner = banner;
    }

    public String getDescriptionAsHtml() {
        return mDescriptionAsHtml;
    }

    public void setDescriptionAsHtml(String descriptionAsHtml) {
        mDescriptionAsHtml = descriptionAsHtml;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getFaqAsHtml() {
        return mFaqAsHtml;
    }

    public void setFaqAsHtml(String faqAsHtml) {
        mFaqAsHtml = faqAsHtml;
    }

    public String getHomepageUrl() {
        return mHomepageUrl;
    }

    public void setHomepageUrl(String homepageUrl) {
        mHomepageUrl = homepageUrl;
    }

    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String icon) {
        mIcon = icon;
    }

    public String getInstallationInstructionsAsHtml() {
        return mInstallationInstructionsAsHtml;
    }

    public void setInstallationInstructionsAsHtml(String installationInstructionsAsHtml) {
        mInstallationInstructionsAsHtml = installationInstructionsAsHtml;
    }

    public String getLastUpdated() {
        return mLastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        mLastUpdated = lastUpdated;
    }

    public String getRating() {
        return mRating;
    }

    public void setRating(String rating) {
        mRating = rating;
    }

    public String getRequiredWordPressVersion() {
        return mRequiredWordPressVersion;
    }

    public void setRequiredWordPressVersion(String requiredWordPressVersion) {
        mRequiredWordPressVersion = requiredWordPressVersion;
    }

    public String getSlug() {
        return mSlug;
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getWhatsNewAsHtml() {
        return mWhatsNewAsHtml;
    }

    public void setWhatsNewAsHtml(String whatsNewAsHtml) {
        mWhatsNewAsHtml = whatsNewAsHtml;
    }

    public int getDownloadCount() {
        return mDownloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        mDownloadCount = downloadCount;
    }

    public int getNumberOfRatings() {
        return mNumberOfRatings;
    }

    public void setNumberOfRatings(int numberOfRatings) {
        mNumberOfRatings = numberOfRatings;
    }

    public int getNumberOfRatingsOfOne() {
        return mNumberOfRatingsOfOne;
    }

    public void setNumberOfRatingsOfOne(int numberOfRatingsOfOne) {
        mNumberOfRatingsOfOne = numberOfRatingsOfOne;
    }

    public int getNumberOfRatingsOfTwo() {
        return mNumberOfRatingsOfTwo;
    }

    public void setNumberOfRatingsOfTwo(int numberOfRatingsOfTwo) {
        mNumberOfRatingsOfTwo = numberOfRatingsOfTwo;
    }

    public int getNumberOfRatingsOfThree() {
        return mNumberOfRatingsOfThree;
    }

    public void setNumberOfRatingsOfThree(int numberOfRatingsOfThree) {
        mNumberOfRatingsOfThree = numberOfRatingsOfThree;
    }

    public int getNumberOfRatingsOfFour() {
        return mNumberOfRatingsOfFour;
    }

    public void setNumberOfRatingsOfFour(int numberOfRatingsOfFour) {
        mNumberOfRatingsOfFour = numberOfRatingsOfFour;
    }

    public int getNumberOfRatingsOfFive() {
        return mNumberOfRatingsOfFive;
    }

    public void setNumberOfRatingsOfFive(int numberOfRatingsOfFive) {
        mNumberOfRatingsOfFive = numberOfRatingsOfFive;
    }
}
