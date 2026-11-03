package com.openlattice.chronicle.serialization

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException
import com.openlattice.chronicle.android.ChronicleData
import com.openlattice.chronicle.android.ChronicleSample
import com.openlattice.chronicle.collection.TestMappers
import com.openlattice.chronicle.fixtures.PayloadFixtureTest
import com.openlattice.chronicle.sources.SourceDevice
import com.openlattice.chronicle.study.StudySetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

/** "@class" names only listed subtypes; the names stay the class names phones and stored settings already carry. */
class PolymorphicTypeAllowlistTest {
    private val mapper = TestMappers.json()

    @Test fun storedAndUploadedJsonReadsAndWritesUnchanged() {
        val device = File(PayloadFixtureTest.resolveRepoRoot(), "fixtures/payloads/ios-device/valid.json").readText()
        assertEquals(mapper.readTree(device), mapper.readTree(mapper.writeValueAsString(mapper.readValue(device, SourceDevice::class.java))))

        val upload = mapper.readValue(
            """[{"@class":"com.openlattice.chronicle.android.ChronicleUsageEvent",
            "studyId":"00000000-0000-4000-8000-000000000001","participantId":"p","appPackageName":"com.example",
            "interactionType":"Move to Foreground","timestamp":"2026-07-01T12:00:00Z","timezone":"UTC",
            "user":"u","applicationLabel":"Example"}]""",
            ChronicleData::class.java,
        )
        val written = mapper.readTree(mapper.writeValueAsString(upload))
        assertEquals("com.openlattice.chronicle.android.ChronicleUsageEvent", written[0]["@class"].asText())
        assertEquals(upload.toList(), mapper.treeToValue(written, ChronicleData::class.java).toList())
    }

    @Test fun everyListedSubtypeKeepsItsClassNameOnTheWire() {
        for (base in listOf(SourceDevice::class.java, ChronicleSample::class.java, StudySetting::class.java)) {
            for (subtype in base.getAnnotation(JsonSubTypes::class.java).value) {
                assertEquals(subtype.value.java.name, subtype.name)
            }
        }
    }

    @Test fun anUnlistedSubtypeIsRefused() {
        assertThrows(InvalidTypeIdException::class.java) {
            mapper.readValue("""{"@class":"${UnlistedDevice::class.java.name}"}""", SourceDevice::class.java)
        }
        assertThrows(InvalidTypeIdException::class.java) {
            mapper.readValue("""{"@class":"${UnlistedSetting::class.java.name}"}""", StudySetting::class.java)
        }
    }

    class UnlistedDevice : SourceDevice
    class UnlistedSetting : StudySetting
}
