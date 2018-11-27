package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsPayload;
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsResponsePayload;
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationSeenResponsePayload;
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsSeenPayload;
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDevicePayload;
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDeviceResponsePayload;
import org.wordpress.android.fluxc.store.NotificationStore.UnregisterDeviceResponsePayload;

@ActionEnum
public enum NotificationAction implements IAction {
    // Remote actions
    @Action(payloadType = RegisterDevicePayload.class)
    REGISTER_DEVICE, // Register device for push notifications with WordPress.com
    @Action
    UNREGISTER_DEVICE, // Unregister device for push notifications with WordPress.com
    @Action(payloadType = FetchNotificationsPayload.class)
    FETCH_NOTIFICATIONS, // Fetch notifications
    @Action(payloadType = MarkNotificationsSeenPayload.class)
    MARK_NOTIFICATIONS_SEEN, // Mark last notification time seen

    // Remote responses
    @Action(payloadType = RegisterDeviceResponsePayload.class)
    REGISTERED_DEVICE, // Response to device registration received
    @Action(payloadType = UnregisterDeviceResponsePayload.class)
    UNREGISTERED_DEVICE, // Response to device unregistration
    @Action(payloadType = FetchNotificationsResponsePayload.class)
    FETCHED_NOTIFICATIONS, // Response to fetching notifications
    @Action(payloadType = MarkNotificationSeenResponsePayload.class)
    MARKED_NOTIFICATIONS_SEEN // Response to marking a notification as seen

    // Local actions
}
