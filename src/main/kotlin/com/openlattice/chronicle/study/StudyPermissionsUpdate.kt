package com.openlattice.chronicle.study

import java.util.UUID

public data class StudyPermissionsUpdate(
    val grantViewStudy: Set<String> = setOf(),
    val grantManageStudy: Set<String> = setOf(),
    val grantOwnerStudy: Set<String> = setOf(),
    val revokeViewStudy: Set<String> = setOf(),
    val revokeManageStudy: Set<String> = setOf(),
    val revokeOwnerStudy: Set<String> = setOf(),
)