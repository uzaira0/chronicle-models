package com.openlattice.chronicle.collection

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated PHI / content-free invariant for every uploadable collection DTO.
 *
 * Chronicle's two hard data-collection guarantees are MIC-FREE and CONTENT-FREE: no
 * uploadable event may carry human-readable content (notification/message text, screen
 * text), free geolocation, or a raw hardware identifier (IMEI/MAC/Android-ID), and no
 * field may carry classic PHI (names, email, phone, address, DOB, MRN, …).
 *
 * This test reflects over the wire DTOs and fails the build if a field name suggests it
 * could hold any of the above. It exists so a future change that adds, say, a
 * `notificationText` or `latitude` field is caught in CI rather than in production.
 *
 * The ONE deliberate, consent-gated exception is `AndroidAudioContentEvent`'s
 * title/artist/album (published media metadata, the opt-in MEDIA_CONTENT layer); it is
 * explicitly allowlisted below so the guard documents — rather than hides — it.
 */
class CollectionDtoPrivacyInvariantTest {

    private companion object {
        // Every uploadable collection DTO. Add new collection event types here so the
        // privacy invariant automatically covers them.
        val DTO_CLASSES: List<Class<*>> = listOf(
            "com.openlattice.chronicle.collection.BatterySample",
            "com.openlattice.chronicle.collection.AndroidInteractionEvent",
            "com.openlattice.chronicle.collection.AndroidAudioActivityEvent",
            "com.openlattice.chronicle.collection.AndroidAudioContentEvent",
            "com.openlattice.chronicle.collection.AndroidNotificationActivityEvent",
            "com.openlattice.chronicle.collection.AndroidSleepEvent",
            "com.openlattice.chronicle.collection.AndroidActivityRecognitionEvent",
            "com.openlattice.chronicle.collection.AndroidHealthMetricEvent",
            "com.openlattice.chronicle.collection.AndroidConnectivityStateEvent",
            "com.openlattice.chronicle.collection.AndroidAppNetworkUsageEvent",
            "com.openlattice.chronicle.collection.AndroidDeviceSettingsEvent",
            "com.openlattice.chronicle.android.AndroidSensorSample",
            "com.openlattice.chronicle.android.ChronicleUsageEvent",
        ).map { Class.forName(it) }

        // Substrings that must never appear in a field name — classic PHI, precise
        // geolocation, and raw hardware identifiers.
        val FORBIDDEN_SUBSTRINGS = listOf(
            "ssn", "socialsecurity", "firstname", "lastname", "fullname", "givenname",
            "surname", "middlename", "maidenname", "contactname",
            "email", "phonenumber", "telephone",
            "streetaddress", "homeaddress", "postalcode", "zipcode",
            "latitude", "longitude", "geolocation", "gpscoordinate", "coordinates",
            "dateofbirth", "birthdate", "creditcard", "passport",
            "medicalrecord", "diagnosis", "icd10",
            "imei", "macaddress", "androidid", "serialnumber", "advertisingid", "hardwareid",
        )

        // Field names (exact, lowercased) that denote human-readable CONTENT.
        val FORBIDDEN_CONTENT_NAMES = setOf(
            "text", "body", "message", "content", "title", "subtitle", "ticker",
            "artist", "album", "caption", "transcript", "snippet", "preview",
        )

        // Suffixes that denote content regardless of prefix (e.g. bigText, tickerText).
        val FORBIDDEN_CONTENT_SUFFIXES = listOf("text", "body", "message")

        // The single documented, consent-gated exception (opt-in MEDIA_CONTENT).
        val ALLOWLIST: Set<Pair<String, String>> = setOf(
            "AndroidAudioContentEvent" to "title",
            "AndroidAudioContentEvent" to "artist",
            "AndroidAudioContentEvent" to "album",
        )

        fun norm(name: String) = name.lowercase().filter { it.isLetterOrDigit() }
    }

    @Test
    fun noCollectionDtoFieldCanCarryPhiContentLocationOrRawIdentifier() {
        val violations = mutableListOf<String>()

        for (clazz in DTO_CLASSES) {
            val simple = clazz.simpleName
            val fields = clazz.declaredFields.filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }
            for (field in fields) {
                if ((simple to field.name) in ALLOWLIST) continue
                val n = norm(field.name)

                FORBIDDEN_SUBSTRINGS.firstOrNull { n.contains(it) }?.let {
                    violations += "$simple.${field.name} — matches forbidden PHI/identifier token '$it'"
                }
                // contentType / *Type enums are metadata, not content: only flag exact names + text/body/message suffixes.
                if (n in FORBIDDEN_CONTENT_NAMES || FORBIDDEN_CONTENT_SUFFIXES.any { n.endsWith(it) }) {
                    violations += "$simple.${field.name} — looks like human-readable content (not allowlisted)"
                }
            }
        }

        assertTrue(
            "Collection DTO privacy invariant violated — these fields could carry PHI/content/location/raw-id. " +
                "If a field is genuinely safe (or a deliberate, consent-gated exception), add it to the ALLOWLIST " +
                "with justification:\n  " + violations.joinToString("\n  "),
            violations.isEmpty(),
        )
    }

    @Test
    fun documentedAudioContentExceptionStillExists() {
        // If the deliberate MEDIA_CONTENT fields are renamed/removed, the allowlist is stale — surface it.
        val audioContent = DTO_CLASSES.first { it.simpleName == "AndroidAudioContentEvent" }
        val names = audioContent.declaredFields.map { it.name }.toSet()
        assertTrue(
            "AndroidAudioContentEvent should still expose title/artist/album (the allowlisted opt-in fields); " +
                "update the ALLOWLIST if this changed. Found: $names",
            names.containsAll(setOf("title", "artist", "album")),
        )
    }
}
