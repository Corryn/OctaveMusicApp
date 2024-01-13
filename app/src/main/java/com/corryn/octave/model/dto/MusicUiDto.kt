package com.corryn.octave.model.dto

sealed class MusicUiDto {

    data class SongUiDto(
        val id: Long,
        val artistId: Long,
        val albumId: Long,
        val songName: String,
        val artistName: String,
        val albumName: String
    ): MusicUiDto()

    data class ArtistUiDto(
        val id: Long,
        val name: String
    ): MusicUiDto()

}
