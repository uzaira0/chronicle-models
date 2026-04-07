/*
 * Copyright (C) 2019. OpenLattice, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the owner of the copyright at support@openlattice.com
 *
 *
 */

package com.openlattice.chronicle.ids

import java.util.UUID

public enum class IdConstants(public val id: UUID) {
    UNINITIALIZED(UUID(0L, 0L)),
    METHODIC(UUID(0, 1L)),

    /* Organizations */

    SYSTEM_ORGANIZATION(UUID(0L, 2L)),


    /* ElasticSearch */
    LAST_WRITE_KEY_ID(UUID(0L, 3L)), // was UUID(0, 0)


    /* Postgres */

    // misc
    COUNT_ID(UUID(0L, 4L)),
    ID_ID(UUID(0L, 5L)),

    // metadata
    LAST_INDEX_ID(UUID(0L, 6L)),
    LAST_LINK_ID(UUID(0L, 7L)),
    LAST_WRITE_ID(UUID(0L, 8L));

    public companion object {
        @JvmField
        public val RESERVED_IDS_BASE: Long = values().maxOf{ it.id.leastSignificantBits } + 1
    }
}