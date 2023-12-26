package com.corryn.octave.model

data class Song(
    val id: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val data: String
)
