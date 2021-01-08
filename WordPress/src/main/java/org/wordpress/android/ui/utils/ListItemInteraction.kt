package org.wordpress.android.ui.utils

interface ListItemInteraction {
    fun click()

    companion object {
        /**
         * Use this creator only with function pointer. If you use it with a lambda, the created `ListItemInteraction`
         * equals method will always fail because two lambdas are never equal. If you need a parametrized function call,
         * use the other creator method.
         */
        fun create(action: () -> Unit): ListItemInteraction {
            return NoParams(action)
        }

        /**
         * Use this creator only with function pointer. If you use it with a lambda, the created `ListItemInteraction`
         * equals method will always fail because two lambdas are never equal.
         */
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
