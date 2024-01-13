package com.corryn.octave.model

import com.corryn.octave.model.data.Artist
import com.corryn.octave.model.data.Song
import com.corryn.octave.model.dto.MusicUiDto

fun Song.toDto(): MusicUiDto.SongUiDto = MusicUiDto.SongUiDto(
    this.id,
    this.artistId,
    this.albumId,
    this.title,
    this.artist,
    this.album
)

fun Artist.toDto(): MusicUiDto.ArtistUiDto = MusicUiDto.ArtistUiDto(
    this.id,
    this.name
)