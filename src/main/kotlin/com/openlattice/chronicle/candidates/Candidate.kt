package com.openlattice.chronicle.candidates

import com.openlattice.chronicle.ids.IdConstants
import java.util.UUID

public data class Candidate(
    var id: UUID = IdConstants.UNINITIALIZED.id,
)
