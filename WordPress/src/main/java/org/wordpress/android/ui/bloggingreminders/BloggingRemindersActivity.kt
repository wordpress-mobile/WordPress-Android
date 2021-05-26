package org.wordpress.android.ui.bloggingreminders

import android.os.Bundle
import org.wordpress.android.databinding.BloggingRemindersActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class BloggingRemindersActivity : LocaleAwareActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(BloggingRemindersActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            setSupportActionBar(toolbar)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onSupportNavigateUp()
        finish()
        return true
    }
}
