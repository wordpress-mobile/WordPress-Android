package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
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

    // Remote responses
    @Action(payloadType = RegisterDeviceResponsePayload.class)
    REGISTERED_DEVICE, // Response to device registration received
    @Action(payloadType = UnregisterDeviceResponsePayload.class)
    UNREGISTERED_DEVICE // Response to device unregistration

    // Local actions
}
