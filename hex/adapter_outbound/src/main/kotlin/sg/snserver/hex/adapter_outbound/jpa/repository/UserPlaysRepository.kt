package sg.snserver.hex.adapter_outbound.jpa.repository

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import sg.snserver.hex.adapter_outbound.jpa.entities.CurrentPlayEntity
import sg.snserver.hex.adapter_outbound.jpa.entities.PlayItemEntity
import sg.snserver.hex.adapter_outbound.jpa.entities.QueuesEntity
import sg.snserver.hex.adapter_outbound.jpa.entities.UserPlaysEntity
import sg.snserver.hex.adapter_outbound.jpa.interfaces.*
import sg.snserver.hex.application.NotExistsException
import sg.snserver.hex.application.outbound.GetCurrentPlayPort
import sg.snserver.hex.application.outbound.SaveCurrentPlayPort
import sg.snserver.hex.domain.entities.CurrentPlay
import sg.snserver.hex.domain.entities.PlayItem
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Repository
class UserPlaysRepository(
    private val playItemRepositoryJpa: PlayItemRepositoryJpa,
    private val playlistRepositoryJpa: PlaylistRepositoryJpa,
    private val currentPlayRepositoryJpa: CurrentPlayRepositoryJpa,
    private val queuesRepositoryJpa: QueuesRepositoryJpa,
    private val userPlaysRepositoryJpa: UserPlaysRepositoryJpa,
) : SaveCurrentPlayPort, GetCurrentPlayPort {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun saveCurrentPlay(
        userId: UUID,
        playlistId: String,
        nowPlayingItem: PlayItem?,
        prevItemIdList: List<String>,
        nextItemIdList: List<String>,
        startSeconds: Float,
    ) {
        val playItemEntity: PlayItemEntity? = nowPlayingItem?.let {
            playItemRepositoryJpa.findById(it.playItemId).orElseThrow {
                IllegalArgumentException("PlayItem not found")
            }
        }
        val playlistEntity = playlistRepositoryJpa.findById(playlistId)
            .orElseThrow { IllegalArgumentException("Playlist not found") }

        val userPlaysEntity = userPlaysRepositoryJpa.findByUserId(userId) ?: UserPlaysEntity(
            userId = userId,
            currentPlays = emptyList<CurrentPlayEntity>().toMutableList(),
        ).also { userPlaysRepositoryJpa.save(it) }

        val queuesEntity = queuesRepositoryJpa.findByUserIdAndPlaylistId(userId.toString(), playlistId)
            ?: queuesRepositoryJpa.save(
                QueuesEntity(
                    userId = userId.toString(),
                    playlistId = playlistId,
                    prev = prevItemIdList,
                    next = nextItemIdList,
                )
            )

        // 변경사항이 있는 경우에만 업데이트
        val isDirty = (queuesEntity.prev != prevItemIdList || queuesEntity.next != nextItemIdList)
        if (isDirty) {
            queuesEntity.prev = prevItemIdList
            queuesEntity.next = nextItemIdList
            queuesRepositoryJpa.save(queuesEntity) // 변경된 경우에만 저장
        }

        // 기존 CurrentPlayEntity 조회
        currentPlayRepositoryJpa.findByUserPlaysAndPlaylist(
            userPlaysEntity = userPlaysEntity,
            playlistEntity = playlistEntity,
        )?.apply {
            // 필드 업데이트
            this.nowPlayingItem = playItemEntity
            this.startSeconds = startSeconds
        } ?: CurrentPlayEntity(
            userPlays = userPlaysEntity,
            nowPlayingItem = playItemEntity,
            playlist = playlistEntity,
            queuesId = queuesEntity.id,
        ).also {
            userPlaysEntity.currentPlays.add(it)
        }

        // 저장
        userPlaysRepositoryJpa.save(userPlaysEntity)
    }


    // n+1
    override fun setCurrentPlayStartSeconds(userId: UUID, playlistId: String, startSeconds: Float) {
        val userPlaysEntity =
            userPlaysRepositoryJpa.findByUserId(userId) ?: throw NotExistsException("not user plays entity $userId")
        val playlistEntity = playlistRepositoryJpa.findWithJoinsByPlaylistId(playlistId)
            ?: throw NotExistsException("not playlist entity $playlistId")
        val currentPlayEntity = currentPlayRepositoryJpa.findByUserPlaysAndPlaylist(
            userPlaysEntity = userPlaysEntity,
            playlistEntity = playlistEntity,
        ) ?: throw NotExistsException("not playlist entity $playlistId")

        currentPlayEntity.startSeconds = startSeconds
    }

    override fun getCurrentPlay(userId: UUID, playlistId: String): CurrentPlay {
        val userPlaysEntity =
            userPlaysRepositoryJpa.findByUserId(userId) ?: throw NotExistsException("not user plays entity $userId")
        val playlistEntity = playlistRepositoryJpa.findWithJoinsByPlaylistId(playlistId)
            ?: throw NotExistsException("not playlist entity $playlistId")
        val currentPlayEntity = currentPlayRepositoryJpa.findByUserPlaysAndPlaylist(
            userPlaysEntity = userPlaysEntity,
            playlistEntity = playlistEntity,
        ) ?: throw NotExistsException("not playlist entity $playlistId")
        val queuesEntity = queuesRepositoryJpa.findById(currentPlayEntity.queuesId).getOrNull()
            ?: throw NotExistsException("not queue entity ${currentPlayEntity.queuesId}")

        val prevIdList: List<String> = queuesEntity.prev
        val nextIdList: List<String> = queuesEntity.next

        // 단일 쿼리 최적화를 위한 커스텀 메서드 사용
        val allItems = playItemRepositoryJpa.findAllByPlayItemIds(prevIdList + nextIdList).associateBy { it.playItemId }

        val prevItemList = prevIdList.mapNotNull { allItems[it]?.toDomain() }.toMutableList()
        val nextItemList = nextIdList.mapNotNull { allItems[it]?.toDomain() }.toMutableList()

        logger.debug(prevItemList.toString())
        logger.debug(nextItemList.toString())

        return CurrentPlay(
            id = currentPlayEntity.id,
            nowPlayingItem = currentPlayEntity.nowPlayingItem?.toDomain(),
            playlist = playlistEntity.toDomain(),
            startSeconds = currentPlayEntity.startSeconds,
            prev = prevItemList,
            next = nextItemList,
            prevItemCount = prevItemList.size,
            nextItemCount = nextItemList.size,
        )
    }

    override fun getCurrentPlays(userId: UUID): List<CurrentPlay> {
        val userPlaysEntity =
            userPlaysRepositoryJpa.findByUserId(userId) ?: throw NotExistsException("not user plays entity $userId")
        val currentPlayEntities = currentPlayRepositoryJpa.findAllByUserPlaysBatch(userPlaysEntity)

        return currentPlayEntities.map {
            val queuesEntity = queuesRepositoryJpa.findById(it.queuesId).getOrNull()
                ?: throw NotExistsException("not queue entity ${it.queuesId}")

            println(queuesEntity)
            val prevIdList: List<String> = queuesEntity.prev
            val nextIdList: List<String> = queuesEntity.next

            println(prevIdList)
            println(nextIdList)

            CurrentPlay(
                id = it.id,
                nowPlayingItem = it.nowPlayingItem?.toDomain(),
                playlist = it.playlist.toDomain(itemsNull = true),
                startSeconds = it.startSeconds,
                prev = emptyList<PlayItem>().toMutableList(),
                next = emptyList<PlayItem>().toMutableList(),
                prevItemCount = prevIdList.size,
                nextItemCount = nextIdList.size,
            )
        }
    }
}