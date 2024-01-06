package com.corryn.octave.model

sealed class MusicUiDto {

    data class SongUiDto(
        val id: String,
        val songName: String,
        val artistName: String
    ): MusicUiDto()

    data class ArtistUiDto(
        val id: String,
        val name: String
    ): MusicUiDto()

}
