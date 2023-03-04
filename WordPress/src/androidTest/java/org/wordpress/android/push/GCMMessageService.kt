package org.wordpress.android.push

import com.google.firebase.messaging.FirebaseMessagingService

/**
 * A dummy implementation of the GCMMessageService.
 * This is needed to avoid a race condition between Hilt's test initialization, and the injection
 * done by @AndroidEntryPoint in the service when receiving a new token.
 */
class GCMMessageService : FirebaseMessagingService()
