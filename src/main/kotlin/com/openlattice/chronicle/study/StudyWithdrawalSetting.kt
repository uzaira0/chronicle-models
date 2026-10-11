package com.openlattice.chronicle.study

public enum class WithdrawalMode { KEEP_PRIOR_DATA, ERASE }

public data class StudyWithdrawalSetting(
    @field:jakarta.validation.constraints.NotNull
    val mode: WithdrawalMode = WithdrawalMode.ERASE,
) : StudySetting
