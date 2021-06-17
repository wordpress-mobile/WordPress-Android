package org.wordpress.android.ui.sitecreation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import org.wordpress.android.R
import org.wordpress.android.R.layout

abstract class SiteCreationBaseFormFragment : Fragment() {
    @get:LayoutRes protected abstract val contentLayout: Int
    protected abstract fun setupContent(rootView: ViewGroup?)
    protected abstract fun onHelp()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    protected fun createMainView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ViewGroup {
        val rootView = inflater.inflate(layout.site_creation_form_screen, container, false) as ViewGroup
        val formContainer = rootView.findViewById<ViewStub>(R.id.site_creation_form_content_stub)
        formContainer.layoutResource = contentLayout
        formContainer.inflate()
        return rootView
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return createMainView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupContent(getView()!!.rootView as ViewGroup)
        val toolbar: Toolbar = view.findViewById(R.id.toolbar_main)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setTitle(screenTitle)
            // important for accessibility
            activity!!.title = screenTitle
        }
    }

    protected abstract val screenTitle: String?
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_site_creation, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            onHelp()
            return true
        }
        return false
    }

    companion object {
        const val EXTRA_SCREEN_TITLE = "extra_screen_title"
    }
}
