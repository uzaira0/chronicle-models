package com.openlattice.chronicle.study

import com.fasterxml.jackson.annotation.JsonCreator
import com.openlattice.chronicle.collection.AndroidDataCollectionSetting
import com.openlattice.chronicle.collection.CollectionDefaults

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
// The caller-supplied map is copied so that later mutation by the caller cannot desynchronize
// the cached hash code from the contents, which would strand entries in hash-based collections.
public class StudySettings @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor( settings: Map<StudySettingType, StudySetting> = mapOf()) :
    Map<StudySettingType, StudySetting> by LinkedHashMap(settings) {
    private val h = settings.hashCode()
    init {
        require(
            StudySettingType.DataCollection !in settings ||
                settings[StudySettingType.DataCollection] is AndroidDataCollectionSetting,
        ) {
            "DataCollection requires generalized module settings"
        }
        require(StudySettingType.AppUsageSurvey !in settings || settings[StudySettingType.AppUsageSurvey] is AppUsageSurveySetting) {
            "AppUsageSurvey requires survey frequency settings"
        }
        require(StudySettingType.Withdrawal !in settings || settings[StudySettingType.Withdrawal] is StudyWithdrawalSetting) {
            "Invalid withdrawal setting"
        }
    }
    public fun androidDataCollection(): AndroidDataCollectionSetting =
        get(StudySettingType.DataCollection) as? AndroidDataCollectionSetting
            ?: CollectionDefaults.androidDataCollectionSetting()

    public fun appUsageSurvey(): AppUsageSurveySetting =
        get(StudySettingType.AppUsageSurvey) as? AppUsageSurveySetting ?: AppUsageSurveySetting()

    /** The study time zone; survey days and exports use it. */
    public fun timeZone(): java.time.ZoneId = java.time.ZoneId.of(
        (get(StudySettingType.TimeUseDiary) as? com.openlattice.chronicle.timeusediary.TimeUseDiarySettings)?.timeZone ?: "UTC",
    )

    public fun withdrawal(): StudyWithdrawalSetting =
        get(StudySettingType.Withdrawal) as? StudyWithdrawalSetting ?: StudyWithdrawalSetting()

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
