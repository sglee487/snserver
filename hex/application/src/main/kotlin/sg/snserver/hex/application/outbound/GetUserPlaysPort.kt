package sg.snserver.hex.application.outbound

import sg.snserver.hex.domain.entities.CurrentPlay
import java.util.*

interface GetUserPlaysPort {
    fun getUserCurrentPlays(userId: UUID): List<Map<String, CurrentPlay>>
}