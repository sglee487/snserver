package sg.snserver.hex.domain.entities

import java.util.*

data class CurrentPlay(
    val id: UUID = UUID.randomUUID(),

    val nowPlayingItem: PlayItem?,
    val playlist: Playlist,

    var startSeconds: Float,
    val prev: MutableList<PlayItem>,
    val next: MutableList<PlayItem>,
    val prevItemCount: Int,
    val nextItemCount: Int,
)