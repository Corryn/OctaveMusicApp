package com.corryn.octave.ui.factory

import com.corryn.octave.model.Artist
import com.corryn.octave.model.MusicUiDto
import com.corryn.octave.model.Song

class MusicUiFactory {

    fun createArtistUiDtos(artists: List<Artist>): List<MusicUiDto.ArtistUiDto> {
        return artists.map {
            MusicUiDto.ArtistUiDto(it.id, it.name)
        }
    }

    fun createSongUiDtos(songs: List<Song>): List<MusicUiDto.SongUiDto> {
        return songs.map {
            MusicUiDto.SongUiDto(
                it.id,
                it.artistId,
                it.albumId,
                it.title,
                it.artist,
                it.album
            )
        }
    }

}