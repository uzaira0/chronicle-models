package com.openlattice.chronicle.collection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

/** Whether a notification was posted or removed. */
public enum class NotificationEventType { POSTED, REMOVED }

/**
 * One content-free notification-activity sample produced by the `notification_activity` collection
 * module — a "digital interruption load" signal.
 *
 * Shared **wire contract** between the Android app's `NotificationListenerService` and the server.
 * It records that *a* notification from [packageName] was [eventType] at [timestamp], with its
 * Android [category] (a fixed constant such as `msg` / `call` / `alarm` / `email` / `social`, never
 * the message) and a few content-free flags. The server aggregates these into per-app counts,
 * category mixes, and timing.
 *
 * **Content-free by construction**: the notification's title, text, big-text, sub-text, people, and
 * any other free-form payload are **never** read — this DTO has no field for them. That is the line
 * between this module and any (separate, IRB-gated) full-notification-content capture.
 * `BEHAVIORAL_METADATA`-class, opt-in.
 *
 * Hard constraint: no `apiKey`, signing secret, or `participantId` field. Unknown JSON fields are
 * ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class AndroidNotificationActivityEvent(
    /** Stable per-event identifier, used for de-duplication on insert/upload. */
    public val id: String,
    /** Event time in UTC. */
    public val timestamp: OffsetDateTime,
    /** Device-default time-zone id at event time. */
    public val timezone: String,
    /** Whether the notification was posted or removed. */
    public val eventType: NotificationEventType,
    /** Package of the app that posted the notification. */
    public val packageName: String,
    /**
     * Android notification category (`Notification.category`, e.g. `msg`, `call`, `email`, `alarm`,
     * `event`, `social`, `progress`, `transport`) — a fixed constant, never message content. `null`
     * if the app set none.
     */
    public val category: String? = null,
    /** Whether this was an ongoing (non-dismissable) notification. `null` if undetermined. */
    public val ongoing: Boolean? = null,
    /** Android importance/priority bucket of the channel/notification, when known (content-free). */
    public val importance: Int? = null,
) {
    init {
        require(id.isNotBlank()) { "AndroidNotificationActivityEvent.id must not be blank" }
        require(timezone.isNotBlank()) { "AndroidNotificationActivityEvent.timezone must not be blank" }
        require(packageName.isNotBlank()) { "AndroidNotificationActivityEvent.packageName must not be blank" }
    }
}
