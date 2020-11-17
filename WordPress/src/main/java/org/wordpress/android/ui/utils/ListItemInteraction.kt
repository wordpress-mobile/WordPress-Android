package org.wordpress.android.ui.utils

interface ListItemInteraction {
    fun click()

    companion object {
        fun create(action: () -> Unit): ListItemInteraction {
            return NoParams(action)
        }

        fun <T> create(data: T, action: (T) -> Unit): ListItemInteraction {
            return OneParam(data, action)
        }
    }

    private data class OneParam<T>(
        val data: T,
        val action: (T) -> Unit
    ) : ListItemInteraction {
        override fun click() {
            action(data)
        }
    }

    private data class NoParams(
        val action: () -> Unit
    ) : ListItemInteraction {
        override fun click() {
            action()
        }
    }
}