package org.wordpress.android.ui.about

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.automattic.about.model.AboutConfig
import com.automattic.about.model.AboutConfigProvider
import com.automattic.about.model.HeaderConfig
import com.automattic.about.model.LegalConfig
import com.automattic.about.model.SocialsConfig
import org.wordpress.android.R

class AboutWordPressActivity : AppCompatActivity(), AboutConfigProvider {
    override fun getAboutConfig(): AboutConfig {
        return AboutConfig(
                headerConfig = HeaderConfig.fromContext(this),
                socialsConfig = SocialsConfig(
                        instagramUsername = "wordpressdotcom",
                        twitterUsername = "wordpressdotcom"
                ),
                legalConfig = LegalConfig(
                        tosUrl = "https://wordpress.com/tos/",
                        privacyPolicyUrl = "https://automattic.com/privacy/",
                        acknowledgementsUrl = "file:///android_asset/licenses.html"
                ),
                onDismiss = { onBackPressed() }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_word_press_activity)
    }
}
