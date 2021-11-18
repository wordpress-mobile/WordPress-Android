package org.wordpress.android.ui.about

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.automattic.about.model.AboutConfig
import com.automattic.about.model.AboutConfigProvider
import org.wordpress.android.R

class AboutWordPressActivity : AppCompatActivity(), AboutConfigProvider {
    override val aboutConfig: AboutConfig by lazy {
        AboutConfig(
                context = this,
                onNavigationButtonClick = {
                    onBackPressed()
                }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_word_press_activity)
    }
}
