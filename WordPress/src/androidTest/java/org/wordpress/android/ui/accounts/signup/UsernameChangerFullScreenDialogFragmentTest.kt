package org.wordpress.android.ui.accounts.signup

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import dagger.android.AndroidInjector
import dagger.android.AndroidInjector.Factory
import dagger.android.DispatchingAndroidInjector
import dagger.android.DispatchingAndroidInjector_Factory
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameSuggestionsFetched
import org.wordpress.android.support.WPSupportUtils.checkViewHasText
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.FullScreenDialogFragment.Builder
import javax.inject.Provider

/**
 * Test to verify that the UsernameChanger functionality is still intact.
 */
@RunWith(AndroidJUnit4::class)
class UsernameChangerFullScreenDialogFragmentTest {
    companion object {
        const val USERNAME = "john876"
        const val DISPLAY_NAME = "John"
        const val USERNAME_TO_BE_SELECTED = "john178"
        const val USERNAME_ALTERNATIVE = "john1"
    }

    lateinit var fragment: BaseUsernameChangerFullScreenDialogFragment
    private val suggestions = OnUsernameSuggestionsFetched().apply {
        suggestions = listOf(USERNAME_TO_BE_SELECTED, USERNAME_ALTERNATIVE)
    }

    /**
     * Emulates the behaviour of the dispatcher. Once a request is made for suggestions
     * and they are returned the fragment is passed the result for processing.
     */
    val mockDispatcher: Dispatcher = mock {
        on { dispatch(any()) } doAnswer {
            fragment.onUsernameSuggestionsFetched(suggestions)
        }
    }

    /**
     * Utilizes the FragmentActivity as a container to host our fragment in an
     * isolated environment.
     */
    @get:Rule
    val activityTestRule = object : ActivityTestRule<FragmentActivity>(
            FragmentActivity::class.java,
            true,
            true
    ) {
        /**
         * Replaces the application-wide fragment injector with the test version.
         */
        override fun beforeActivityLaunched() {
            super.beforeActivityLaunched()
            val wordPressApp = InstrumentationRegistry.getTargetContext().applicationContext as WordPress
            wordPressApp.setSupportFragmentInjector(
                    createFakeFragmentInjector<UsernameChangerFullScreenDialogFragment> {
                        mDispatcher = mockDispatcher
                    })
        }

        /**
         * Opens the dialog in a similar fashion to the production code.
         */
        override fun afterActivityLaunched() {
            super.afterActivityLaunched()

            val bundle = BaseUsernameChangerFullScreenDialogFragment.newBundle(
                    DISPLAY_NAME, USERNAME
            )

            val dialog = Builder(activity)
                    .setTitle(string.username_changer_title)
                    .setAction(string.username_changer_action)
                    .setOnConfirmListener(null)
                    .setOnDismissListener(null)
                    .setContent(UsernameChangerFullScreenDialogFragment::class.java, bundle)
                    .build()

            dialog.show(activity.supportFragmentManager, FullScreenDialogFragment.TAG)
            fragment = dialog.content as BaseUsernameChangerFullScreenDialogFragment
        }
    }

    @Test
    fun verifyHeaderTextLiveUpdates() {
        // Checks to see if see if header's text has the initial username
        checkViewHasText(
                onView(withId(R.id.header)), fragment.getHeaderText(
                USERNAME,
                DISPLAY_NAME
        ).toString()
        )

        // Clicks on the second username suggestion.
        onView(withId(R.id.suggestions)).perform(
                RecyclerViewActions.actionOnItemAtPosition<UsernameChangerRecyclerViewAdapter.ViewHolder>(
                        1,
                        click()
                )
        )

        // Verifies that the username was selected.
        assertEquals(USERNAME_TO_BE_SELECTED, fragment.mUsernameSelected)

        // Verifies that the header text has updated.
        checkViewHasText(
                onView(withId(R.id.header)), fragment.getHeaderText(
                USERNAME_TO_BE_SELECTED,
                DISPLAY_NAME
        ).toString()
        )
    }

    /**
     * Creates a fragment injector that replaces the Application injector and calls
     * the lambda with the Fragment instance so injection can be done with the mocked instances.
     *
     * Inspired by Ronen Sabag
     * https://proandroiddev.com/fragment-espresso-testing-with-daggers-android-injector-2bd70b6a842d
     */
    inline fun <reified F : Fragment> createFakeFragmentInjector(crossinline block: F.() -> Unit):
            DispatchingAndroidInjector<Fragment> {
        // obtain the fragment level injector
        val myApp = InstrumentationRegistry.getTargetContext().applicationContext as WordPress
        val originalFragmentInjector = myApp.supportFragmentInjector()

        // set the fragment injector which apply the original injector and than apply our block
        val fragmentInjector = AndroidInjector<Fragment> { fragment ->
            originalFragmentInjector?.inject(fragment)
            if (fragment is F) {
                fragment.block()
            }
        }
        val fragmentFactory = Factory<Fragment> { fragmentInjector }
        val fragmentMap = mapOf(Pair<Class<*>, Provider<Factory<*>>>(F::class.java, Provider { fragmentFactory }))

        return DispatchingAndroidInjector_Factory.newInstance(fragmentMap, emptyMap())
    }
}
