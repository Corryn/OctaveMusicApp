package com.corryn.octave.model.dto

sealed class MusicUiDto {
    abstract val activated: Boolean // Determines the color of the list item (white or green)

    data class SongUiDto(
        val id: Long,
        val artistId: Long,
        val albumId: Long,
        val songName: String,
        val artistName: String,
        val albumName: String,
        override val activated: Boolean
    ): MusicUiDto()

    data class ArtistUiDto(
        val id: Long,
        val name: String,
        override val activated: Boolean
    ): MusicUiDto()

}
