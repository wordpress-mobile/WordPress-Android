package org.wordpress.android.ui.reader.subfilter

import android.os.Bundle
import androidx.fragment.app.Fragment
import org.wordpress.android.models.ReaderTag

interface SubFilterViewModelOwner {
    fun getSubFilterViewModelForKey(key: String): SubFilterViewModel
    fun getSubFilterViewModelForTag(tag: ReaderTag, savedInstanceState: Bundle? = null): SubFilterViewModel

    companion object {
        /**
         * Helper function to get the [SubFilterViewModel] for a given [ReaderTag] from a [Fragment]. Note that the
         * [Fragment] must be a child or descendant of a Fragment that implements [SubFilterViewModelOwner], otherwise
         * this function will throw an [IllegalStateException].
         *
         * @param fragment the [Fragment] to get the [SubFilterViewModel] from
         * @param tag the [ReaderTag] to get the [SubFilterViewModel] for
         * @param savedInstanceState the [Bundle] to pass to the [SubFilterViewModel] when it is created
         * @return the [SubFilterViewModel] for the given [ReaderTag]
         */
        @JvmStatic
        @JvmOverloads
        fun getSubFilterViewModelForTag(
            fragment: Fragment,
            tag: ReaderTag,
            savedInstanceState: Bundle? = null
        ): SubFilterViewModel {
            // traverse the parent fragment hierarchy to find the SubFilterViewModelOwner
            var possibleOwner: Fragment? = fragment
            while (possibleOwner != null) {
                if (possibleOwner is SubFilterViewModelOwner) {
                    return possibleOwner.getSubFilterViewModelForTag(tag, savedInstanceState)
                }
                possibleOwner = possibleOwner.parentFragment
            }
            error("Fragment must be a child or descendant of a Fragment that implements SubFilterViewModelOwner")
        }

        /**
         * Helper function to get the [SubFilterViewModel] for a given key from a [Fragment]. Note that the [Fragment]
         * must be a child or descendant of a Fragment that implements [SubFilterViewModelOwner], otherwise this
         * function will throw an [IllegalStateException].
         *
         * @param fragment the [Fragment] to get the [SubFilterViewModel] from
         * @param key the key to get the [SubFilterViewModel] for
         * @return the [SubFilterViewModel] for the given key, or null if the [Fragment] is not a child or descendant
         * of a Fragment that implements [SubFilterViewModelOwner]
         */
        @JvmStatic
        fun getSubFilterViewModelForKey(
            fragment: Fragment,
            key: String,
        ): SubFilterViewModel {
            // traverse the parent fragment hierarchy to find the SubFilterViewModelOwner
            var possibleOwner: Fragment? = fragment
            while (possibleOwner != null) {
                if (possibleOwner is SubFilterViewModelOwner) {
                    return possibleOwner.getSubFilterViewModelForKey(key)
                }
                possibleOwner = possibleOwner.parentFragment
            }
            error("Fragment must be a child or descendant of a Fragment that implements SubFilterViewModelOwner")
        }
    }
}
