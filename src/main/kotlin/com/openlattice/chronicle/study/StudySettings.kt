package com.openlattice.chronicle.study

import com.fasterxml.jackson.annotation.JsonCreator

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
// The caller-supplied map is copied so that later mutation by the caller cannot desynchronize
// the cached hash code from the contents, which would strand entries in hash-based collections.
public class StudySettings @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor( settings: Map<StudySettingType, StudySetting> = mapOf()) :
    Map<StudySettingType, StudySetting> by LinkedHashMap(settings) {
    private val h = settings.hashCode()
    override fun equals(other: Any?): Boolean {
        return if (other !is StudySettings) return false
        else {
            if (other.keys == this.keys) {
                this.all { other.getValue(it.key) == it.value }
            } else false
        }
    }

    override fun hashCode(): Int = h
}

public enum class StudySettingType {
    DataCollection,
    Notifications,
    Sensor,
    TimeUseDiary,
    Survey,
    AndroidSensor,
    DataQuality,
    Pipeline,
    Encryption,
    ParticipantPolicy,
}
