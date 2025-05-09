package sg.snserver.hex.adapter_inbound

import jakarta.annotation.security.PermitAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import sg.snserver.hex.adapter_inbound.dto.ApiResponseDTO
import sg.snserver.hex.adapter_inbound.dto.CreatePlaylistRequestDTO
import sg.snserver.hex.adapter_inbound.dto.GetPlaylistResponseDTO
import sg.snserver.hex.adapter_inbound.dto.toResponseDTO
import sg.snserver.hex.application.inbound.GetPlaylistUseCase
import sg.snserver.hex.application.inbound.SaveYoutubeDataUseCase
import sg.snserver.hex.application.inbound.UpdateYoutubeDataUseCase

@RestController
@RequestMapping(
    "/api/v1/playlists",
    consumes = [],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class PlaylistController(
    val saveYoutubeDataUseCase: SaveYoutubeDataUseCase,
    val getPlaylistUseCase: GetPlaylistUseCase,
    val updateYoutubeDataUseCase: UpdateYoutubeDataUseCase,
) {

    @PreAuthorize("hasRole('USER')")
    @Transactional
    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun createPlaylist(
        @RequestBody request: CreatePlaylistRequestDTO,
    ): ApiResponseDTO.Success<Unit> {

        saveYoutubeDataUseCase.savePlaylist(request.playlistId)

        return ApiResponseDTO.emptySuccess(
            message = "save successful",
        )
    }

    @PermitAll
    @GetMapping
    fun getPlaylists(
        pageable: Pageable,
    ): ApiResponseDTO.Success<Page<GetPlaylistResponseDTO>> {

        val playlistBatch = getPlaylistUseCase.getPlaylistBatch(pageable)

        return ApiResponseDTO.Success(
            message = "get playlistBatch success",
            data = playlistBatch.map {
                it.toResponseDTO()
            }
        )
    }

    @PermitAll
    @GetMapping(
        "/{playlistId}",
    )
    fun getPlaylist(
        @PathVariable playlistId: String,
    ): ApiResponseDTO.Success<GetPlaylistResponseDTO> {

        val playlist = getPlaylistUseCase.getPlaylist(playlistId)

        return ApiResponseDTO.Success(
            message = "get playlist success",
            data = playlist.toResponseDTO()
        )

    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    @PatchMapping(
        "/{playlistId}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun updatePlaylist(
        @PathVariable playlistId: String,
    ): ApiResponseDTO.Success<Unit> {
        updateYoutubeDataUseCase.updatePlaylist(playlistId = playlistId)

        return ApiResponseDTO.emptySuccess(
            message = "update successful",
        )
    }
}