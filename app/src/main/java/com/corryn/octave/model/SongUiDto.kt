package com.corryn.octave.model

data class SongUiDto(
    val id: Long,
    val artistId: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val album: String
)
