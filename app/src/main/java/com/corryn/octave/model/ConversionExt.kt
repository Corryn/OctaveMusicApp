package com.corryn.octave.model

fun Song.toDto(): MusicUiDto.SongUiDto = MusicUiDto.SongUiDto(
    this.id,
    this.artistId,
    this.albumId,
    this.title,
    this.artist,
    this.album
)