package sg.snserver.hex.adapter_outbound.jpa.entities

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "sn_current_play")
data class CurrentPlayEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne
    val userPlays: UserPlaysEntity,

    @JoinColumn
    @ManyToOne(cascade = [CascadeType.MERGE, CascadeType.PERSIST])
    val nowPlayingItem: PlayItemEntity,

    @JoinColumn
    @ManyToOne(cascade = [CascadeType.MERGE, CascadeType.PERSIST])
    val playlist: PlaylistEntity,

    @Column(name = "sn_current_play_queues")
    val queuesId: String,

    ) : BaseEntity()