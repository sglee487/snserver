package sg.snserver.hex.domain.entities

import java.util.*

data class Resource(
    val id: UUID = UUID.randomUUID(),

    val kind: String,
    val videoId: String,
): Base()
