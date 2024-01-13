package com.corryn.octave.model.data

data class Song(
    val id: Long,
    val artistId: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val data: String
)
