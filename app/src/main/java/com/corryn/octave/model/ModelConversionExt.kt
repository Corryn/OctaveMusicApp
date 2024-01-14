package com.corryn.octave.model

import com.corryn.octave.model.data.Artist
import com.corryn.octave.model.data.Song
import com.corryn.octave.model.dto.MusicUiDto

fun Song.toDto(position: Int = 0): MusicUiDto.SongUiDto = MusicUiDto.SongUiDto(
    this.id,
    this.artistId,
    this.albumId,
    this.title,
    this.artist,
    this.album,
    position % 2 == 1
)

fun Artist.toDto(position: Int = 0): MusicUiDto.ArtistUiDto = MusicUiDto.ArtistUiDto(
    this.id,
    this.name,
    position % 2 == 1
)