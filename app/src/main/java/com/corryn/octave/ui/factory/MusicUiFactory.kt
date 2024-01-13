package com.corryn.octave.ui.factory

import com.corryn.octave.model.data.Artist
import com.corryn.octave.model.data.Song
import com.corryn.octave.model.dto.MusicUiDto
import com.corryn.octave.model.toDto

class MusicUiFactory {

    fun createArtistUiDtos(artists: List<Artist>): List<MusicUiDto.ArtistUiDto> {
        return artists.map { it.toDto() }
    }

    fun createSongUiDtos(songs: List<Song>): List<MusicUiDto.SongUiDto> {
        return songs.map { it.toDto() }
    }

}