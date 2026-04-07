package com.openlattice.chronicle.android

import java.time.OffsetDateTime
import java.util.*

/**
 * * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 *
 * The bina parameters for this query are in the following order:
 * @param studyId (text/uuid)
 * @param participantId (text)
 * @param appPackageName (text)
 * @param interactionType (text)
 * @param timestamp (timestamptz)
 * @param timezone (text)
 * @param user (text)
 * @param applicationLabel (text)
 */
public data class ChronicleUsageEvent(
    val studyId: UUID,
    val participantId: String,
    val appPackageName: String,
    val interactionType: String,
    val eventType: Int = fromInteractionType(interactionType) ,
    val timestamp: OffsetDateTime,
    val timezone: String,
    val user: String,
    val applicationLabel: String,
) : ChronicleSample

public fun fromInteractionType(interactionType: String): Int {
    return when (interactionType) {
        "Activity Paused" -> ChronicleUsageEventType.ACTIVITY_PAUSED.value
        "Activity Resumed" -> ChronicleUsageEventType.ACTIVITY_RESUMED.value
        "Activity Stopped" -> ChronicleUsageEventType.ACTIVITY_STOPPED.value
        "Configuration Change" -> ChronicleUsageEventType.CONFIGURATION_CHANGE.value
        "Device Shutdown" -> ChronicleUsageEventType.DEVICE_SHUTDOWN.value
        "Device Startup" -> ChronicleUsageEventType.DEVICE_STARTUP.value
        "Foreground Service Start" -> ChronicleUsageEventType.FOREGROUND_SERVICE_START.value
        "Foreground Service Stop" -> ChronicleUsageEventType.FOREGROUND_SERVICE_STOP.value
        "Keyguard Shown" -> ChronicleUsageEventType.KEYGUARD_SHOWN.value
        "Keyguard Hidden" -> ChronicleUsageEventType.KEYGUARD_HIDDEN.value
        "Move to Background" -> ChronicleUsageEventType.MOVE_TO_BACKGROUND.value
        "Move to Foreground" -> ChronicleUsageEventType.MOVE_TO_FOREGROUND.value
        "None" -> ChronicleUsageEventType.NONE.value
        "Shortcut Invocation" -> ChronicleUsageEventType.SHORTCUT_INVOCATION.value
        "Screen Interactive" -> ChronicleUsageEventType.SCREEN_INTERACTIVE.value
        "Screen Non-interactive" -> ChronicleUsageEventType.SCREEN_NON_INTERACTIVE.value
        "User Interaction" -> ChronicleUsageEventType.USER_INTERACTION.value
        "Usage Stat" -> -1
        else -> {
            interactionType.substringAfter("Unknown importance: ").toIntOrNull() ?: -1
        }
    }
}


