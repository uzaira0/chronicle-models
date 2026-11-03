package com.openlattice.chronicle.study

import com.openlattice.chronicle.generated.CollectionDefaultValues


public data class StudyWithdrawalSetting(
    @field:jakarta.validation.constraints.NotNull
    val mode: WithdrawalMode = CollectionDefaultValues.WITHDRAWAL_MODE,
) : StudySetting
