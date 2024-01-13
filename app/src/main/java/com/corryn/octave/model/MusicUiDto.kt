package com.corryn.octave.model

sealed class MusicUiDto {

    data class SongUiDto(
        val id: Long,
        val songName: String,
        val artistName: String
    ): MusicUiDto()

    data class ArtistUiDto(
        val id: Long,
        val name: String
    ): MusicUiDto()

}
